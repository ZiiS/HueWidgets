// Copyright © 2019 Barnaby Shearer <b@Zi.iS>

package is.zi.hue;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OnAccountsUpdateListener;
import android.accounts.OperationCanceledException;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import org.json.JSONException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import javax.xml.xpath.XPathExpressionException;

import is.zi.NonNull;
import is.zi.Nullable;
import is.zi.comm.Http;
import is.zi.hueaccounts.AccountAuthenticatorService;
import is.zi.huewidgets.R;


@SuppressWarnings("deprecation")
public class HueBridgeService extends Service {
    @NonNull
    private final IBinder mBinder = new LocalBinder();
    @NonNull
    private final HashMap<HueLight, HueColor> colors = new HashMap<>();
    @NonNull
    private final ArrayList<HueBridge> bridges = new ArrayList<>();
    @NonNull
    private final HashMap<HueLight, OnListener<HueColor>> onColorListeners = new HashMap<>();
    @NonNull
    private HueBridge.Factory bridgeFactory = new HueBridge.Factory();
    private boolean throwOnAlert = false;
    @Nullable
    private AccountManager am;
    @Nullable
    private OnAccountsUpdateListener onAccounts;
    @Nullable
    private HueLight[] lights;
    @Nullable
    private OnListener<Integer> onAlertListener;
    @Nullable
    private OnListener<HueLight[]> onLightsListener;
    @Nullable
    private OnListener<HueBridge[]> onBridgesListener;
    @Nullable
    private OnListener<HueBridge> onPairListener;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        am = AccountManager.get(this);
    }

    @Override
    public void onDestroy() {
        if (am != null && onAccounts != null) {
            am.removeOnAccountsUpdatedListener(onAccounts);
            onAccounts = null;
        }
        am = null;
    }

    private void onAlert(@Nullable Integer alert) {
        if (am != null && alert != null && alert == R.string.error_account) {
            withAccount(am::clearPassword);
        }
        if (onAlertListener != null) {
            onAlertListener.on(alert);
        }
        if (throwOnAlert && alert != null) {
            throw new RuntimeException(getResources().getString(alert));
        }
    }

    public void setOnAlertListener(@Nullable OnListener<Integer> onAlertListener) {
        this.onAlertListener = onAlertListener;
    }

    private void withAccount(@NonNull OnListener<Account> withAccount) {
        Account[] accounts = Objects.requireNonNull(am).getAccountsByType(AccountAuthenticatorService.ACCOUNT_TYPE);
        if (accounts.length == 0) {
            if (onAccounts != null) {
                am.removeOnAccountsUpdatedListener(onAccounts);
                onAccounts = null;
            }
            onAccounts = newAccounts -> {
                if (newAccounts.length > 0) {
                    am.removeOnAccountsUpdatedListener(onAccounts);
                    onAccounts = null;
                    withAccount.on(newAccounts[0]);
                }
            };
            am.addOnAccountsUpdatedListener(onAccounts, null, true);

            Intent intent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                intent = AccountManager.newChooseAccountIntent(
                        null,
                        null,
                        new String[]{AccountAuthenticatorService.ACCOUNT_TYPE},
                        null,
                        AccountAuthenticatorService.ACCOUNT_TYPE,
                        null,
                        null
                );
            } else {
                intent = AccountManager.newChooseAccountIntent(
                        null,
                        null,
                        new String[]{AccountAuthenticatorService.ACCOUNT_TYPE},
                        false,
                        null,
                        AccountAuthenticatorService.ACCOUNT_TYPE,
                        null,
                        null
                );
            }
            intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            withAccount.on(accounts[0]);
        }
    }

    public void setBridgeFactory(@NonNull HueBridge.Factory bridgeFactory) {
        this.bridgeFactory = bridgeFactory;
    }

    public void setThrowOnAlert() {
        throwOnAlert = true;
    }

    private void withBridge(OnListener<HueBridge> onBridgeListener) {
        if (am != null) {
            withAccount(account ->
                    new GetToken(onBridgeListener, this::onAlert, am, Objects.requireNonNull(account), bridgeFactory).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            );
        }
    }

    private void onLights(@Nullable HueLight[] lights) {
        this.lights = lights;
        if (onLightsListener != null) {
            onLightsListener.on(lights);
        }
    }

    public void setOnLightsListener(@Nullable OnListener<HueLight[]> onLightsListener) {
        this.onLightsListener = onLightsListener;
    }

    public void getLights() {
        if (lights == null) {
            withBridge(hueBridge -> new GetWithBridgeLights(this::onLights, this::onAlert).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, hueBridge));
        } else {
            onLights(lights);
        }
    }

    private void onColor(@NonNull Pair<HueLight, HueColor> color) {
        colors.put(color.first, color.second);
        if (onColorListeners.containsKey(color.first)) {
            Objects.requireNonNull(onColorListeners.get(color.first)).on(color.second);
        }
    }

    public void setOnColorListener(@NonNull HueLight light, @Nullable OnListener<HueColor> onLightsListener) {
        if (onLightsListener == null) {
            onColorListeners.remove(light);
        } else {
            onColorListeners.put(light, onLightsListener);
        }
    }

    public void getColor(HueLight light) {
        if (colors.containsKey(light)) {
            onColor(new Pair<>(light, colors.get(light)));
        } else {
            withBridge(hueBridge -> new GetWithBridgeColor(this::onColor, this::onAlert, light).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, hueBridge));
        }
    }

    public void setColor(HueLight light, HueColor color) {
        withBridge(hueBridge -> new SetWithBridgeColor(null, this::onAlert, light, colors.get(light), color).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, hueBridge));
    }

    public void setOn(HueLight light, boolean on) {
        withBridge(hueBridge -> new SetWithBridgeOn(null, this::onAlert, light, on).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, hueBridge));
    }

    private void onBridge(@Nullable HueBridge hueBridge) {
        if (hueBridge != null) {
            bridges.add(hueBridge);
        }
        onBridges(bridges);
    }

    private void onBridges(@Nullable ArrayList<HueBridge> hueBridges) {
        if (onBridgesListener != null) {
            onBridgesListener.on(Objects.requireNonNull(hueBridges).toArray(new HueBridge[]{}));
        }
    }

    public void setOnBridgesListener(@Nullable OnListener<HueBridge[]> onBridgesListener) {
        this.onBridgesListener = onBridgesListener;
    }

    public void getBridges() {
        onBridges(bridges);
        new GetBridge(this::onBridge, this::onAlert, bridges, bridgeFactory).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void addBridge(String url) {
        new AddBridge(this::onBridge, this::onAlert, url).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void onPair(@Nullable HueBridge hueBridge) {
        if (onPairListener != null) {
            onPairListener.on(hueBridge);
        }
    }

    public void setOnPairListener(@Nullable OnListener<HueBridge> onPairListener) {
        this.onPairListener = onPairListener;
    }

    public void getPair() {
        new GetPair(this::onPair, this::onAlert, bridges).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public interface OnListener<T> {
        void on(@Nullable T e);
    }

    abstract private static class AsyncTaskGet<Param, Return> extends AsyncTask<Param, Void, Return> {
        @Nullable
        private final OnListener<Return> onComplete;
        @Nullable
        private final OnListener<Integer> onFail;
        @Nullable
        Integer error = null;

        private AsyncTaskGet(@Nullable OnListener<Return> onComplete, @Nullable OnListener<Integer> onFail) {
            this.onComplete = onComplete;
            this.onFail = onFail;
        }

        @Override
        protected void onPostExecute(@Nullable Return ret) {
            if (ret == null && onFail != null) {
                onFail.on(error);
            }
            if (onComplete != null) {
                onComplete.on(ret);
            }
        }
    }

    abstract private static class AsyncTaskGetWithBridge<Return> extends AsyncTaskGet<HueBridge, Return> {

        private AsyncTaskGetWithBridge(OnListener<Return> onComplete, OnListener<Integer> onFail) {
            super(onComplete, onFail);
        }

        @Nullable
        abstract Return doInBackgroundWithBridge(HueBridge hueBridge) throws IOException, Http.HttpException, JSONException, Http.ApplicationLayerException;

        @Nullable
        @Override
        protected Return doInBackground(HueBridge... hueBridges) {
            try {
                return doInBackgroundWithBridge(hueBridges[0]);
            } catch (Http.ApplicationLayerException e) {
                Log.e("AsyncTaskBridgeGet", "", e);
                if (e.type == Http.ApplicationLayerException.UNAUTHORIZED) {
                    error = R.string.error_account;
                } else {
                    error = R.string.error_bridge;
                }
            } catch (@NonNull IOException | Http.HttpException | JSONException e) {
                error = R.string.error_bridge;
                Log.e("AsyncTaskBridgeGet", "", e);
            }
            return null;
        }
    }

    private static class GetBridge extends AsyncTaskGet<Void, HueBridge> {
        private final HueBridge.Factory factory;
        private final ArrayList<HueBridge> known;

        private GetBridge(OnListener<HueBridge> onComplete, OnListener<Integer> onFail, ArrayList<HueBridge> known, HueBridge.Factory factory) {
            super(onComplete, onFail);
            this.known = known;
            this.factory = factory;
        }

        @Nullable
        @Override
        protected HueBridge doInBackground(Void... voids) {
            try {
                return factory.findNew(known);
            } catch (HueBridge.NotFoundException e) {
                //pass
            } catch (HueBridge.NoWifiException e) {
                error = R.string.error_no_wifi;
                Log.e("GetBridge", "", e);
            } catch (@NonNull IOException | Http.HttpException | XPathExpressionException e) {
                error = R.string.error_bridge;
                Log.e("GetBridge", "", e);
            }
            return null;
        }

    }

    private static class AddBridge extends AsyncTaskGet<Void, HueBridge> {
        private final String url;

        private AddBridge(OnListener<HueBridge> onComplete, OnListener<Integer> onFail, String url) {
            super(onComplete, onFail);
            this.url = url;
        }

        @Nullable
        @Override
        protected HueBridge doInBackground(Void... voids) {
            try {
                HueBridge bridge = new HueBridge(url);
                bridge.load();
                return bridge;
            } catch (@NonNull IOException | Http.HttpException | XPathExpressionException e) {
                error = R.string.error_bridge;
                Log.e("AddBridge", "", e);
            }
            return null;
        }

    }


    private static class GetPair extends AsyncTaskGet<Void, HueBridge> {
        private final ArrayList<HueBridge> known;

        private GetPair(OnListener<HueBridge> onComplete, OnListener<Integer> onFail, ArrayList<HueBridge> known) {
            super(onComplete, onFail);
            this.known = known;
        }

        @Nullable
        @Override
        protected HueBridge doInBackground(Void... voids) {
            try {
                for (HueBridge bridge : known) {
                    try {
                        return bridge.pair();
                    } catch (HueBridge.NotPressedException e) {
                        //pass
                    }
                }
            } catch (@NonNull IOException | Http.HttpException | JSONException | Http.ApplicationLayerException e) {
                error = R.string.error_bridge;
                Log.e("GetPair", "", e);
            }
            return null;
        }

    }


    private static class GetWithBridgeColor extends AsyncTaskGetWithBridge<Pair<HueLight, HueColor>> {
        private final HueLight light;

        private GetWithBridgeColor(OnListener<Pair<HueLight, HueColor>> onComplete, OnListener<Integer> onFail, HueLight light) {
            super(onComplete, onFail);
            this.light = light;
        }

        @NonNull
        @Override
        Pair<HueLight, HueColor> doInBackgroundWithBridge(@NonNull HueBridge hueBridge) throws Http.HttpException, Http.ApplicationLayerException, JSONException, IOException {
            return new Pair<>(light, hueBridge.getColor(light));
        }

        @Nullable
        @Override
        protected Pair<HueLight, HueColor> doInBackground(HueBridge... hueBridges) {
            try {
                return doInBackgroundWithBridge(hueBridges[0]);
            } catch (Http.ApplicationLayerException e) {
                Log.e("AsyncTaskBridgeGet", "", e);
                if (e.type == Http.ApplicationLayerException.UNAUTHORIZED) {
                    error = R.string.error_account;
                } else {
                    error = R.string.error_bridge;
                }
            } catch (@NonNull IOException | Http.HttpException | JSONException e) {
                error = R.string.error_bridge;
                Log.e("AsyncTaskBridgeGet", "", e);
            }
            return new Pair<>(light, null);
        }
    }

    private static class SetWithBridgeColor extends AsyncTaskGetWithBridge<Boolean> {
        private final HueLight light;
        private final HueColor oldColor;
        private final HueColor color;

        private SetWithBridgeColor(@SuppressWarnings("SameParameterValue") OnListener<Boolean> onComplete, OnListener<Integer> onFail, HueLight light, HueColor oldColor, HueColor color) {
            super(onComplete, onFail);
            this.light = light;
            this.oldColor = oldColor;
            this.color = color;
        }

        @NonNull
        @Override
        protected Boolean doInBackgroundWithBridge(@NonNull HueBridge hueBridge) throws Http.HttpException, Http.ApplicationLayerException, JSONException, IOException {
            hueBridge.setColor(light, oldColor, color);
            return true;
        }
    }

    private static class SetWithBridgeOn extends AsyncTaskGetWithBridge<Boolean> {
        private final HueLight light;
        private final boolean on;

        private SetWithBridgeOn(@SuppressWarnings("SameParameterValue") OnListener<Boolean> onComplete, OnListener<Integer> onFail, HueLight light, boolean on) {
            super(onComplete, onFail);
            this.light = light;
            this.on = on;
        }

        @NonNull
        @Override
        protected Boolean doInBackgroundWithBridge(@NonNull HueBridge hueBridge) throws Http.HttpException, Http.ApplicationLayerException, JSONException, IOException {
            hueBridge.setOn(light, on);
            return true;
        }
    }

    private static class GetWithBridgeLights extends AsyncTaskGetWithBridge<HueLight[]> {

        private GetWithBridgeLights(OnListener<HueLight[]> onComplete, OnListener<Integer> onFail) {
            super(onComplete, onFail);
        }

        @Nullable
        @Override
        protected HueLight[] doInBackgroundWithBridge(@Nullable HueBridge hueBridge) throws Http.HttpException, Http.ApplicationLayerException, JSONException, IOException {
            if(hueBridge != null) {
                return hueBridge.getLights();
            }
            return null;
        }
    }

    private static class GetToken extends AsyncTaskGet<Void, HueBridge> {
        @Nullable
        private final String url;
        @Nullable
        private final AccountManagerFuture<Bundle> token;
        @NonNull
        private final HueBridge.Factory bridgeFactory;
        @Nullable
        private final X509Certificate cert;

        private GetToken(@Nullable OnListener<HueBridge> onComplete, @Nullable OnListener<Integer> onFail, @NonNull AccountManager am, @NonNull Account account, @NonNull HueBridge.Factory bridgeFactory) {
            super(onComplete, onFail);
            this.bridgeFactory = bridgeFactory;
            if(!account.type.equals(AccountAuthenticatorService.ACCOUNT_TYPE)){
                Log.w("GetToken", "Unexpected account type " + account.type + " != " + AccountAuthenticatorService.ACCOUNT_TYPE);
                // Not sure why this happens
                url = null;
                token = null;
                cert = null;
                return;
            }
            //Don't hold am
            url = am.getUserData(account, AccountAuthenticatorService.KEY_URL);

            token = am.getAuthToken(
                    account,
                    AccountAuthenticatorService.TOKEN_TYPE,
                    null,
                    null,
                    null,
                    null
            );
            X509Certificate cert1;
            try {
                cert1 = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(
                        new ByteArrayInputStream(
                                Base64.decode(
                                        am.getUserData(account, AccountAuthenticatorService.KEY_CERT),
                                        Base64.DEFAULT
                                )
                        )
                );
            } catch (CertificateException e) {
                cert1 = null;
                Objects.requireNonNull(onFail).on(R.string.error_account);
            }
            cert = cert1;
        }

        @Nullable
        @Override
        protected HueBridge doInBackground(Void... voids) {
            if(token != null) {
                try {
                    return bridgeFactory.create(
                            Objects.requireNonNull(url),
                            Objects.requireNonNull(token.getResult().getString(AccountManager.KEY_AUTHTOKEN)),
                            cert
                    );

                } catch (@NonNull IOException | AuthenticatorException | OperationCanceledException e) {
                    error = R.string.error_account;
                    Log.e("GetToken", "", e);
                }
            }
            return null;
        }
    }

    public class LocalBinder extends Binder {
        @NonNull
        public HueBridgeService getService() {
            return HueBridgeService.this;
        }
    }

}

