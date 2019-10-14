// Copyright © 2019 Barnaby Shearer <b@Zi.iS>

package is.zi.coffee;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.res.TypedArray;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.android.vending.billing.IInAppBillingService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Objects;

import is.zi.NonNull;
import is.zi.Nullable;
import is.zi.huewidgets.R;

public class Coffee extends android.app.Fragment {
    private final static int REQUEST_CODE = 1042;

    @Nullable
    private IInAppBillingService inAppBillingService;
    @Nullable
    private ServiceConnection serviceConnection;
    @Nullable
    private String itemCode;

    @Override
    public void onInflate(@NonNull Context context, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(context, attrs, savedInstanceState);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Coffee);
        itemCode = a.getString(R.styleable.Coffee_sku);
        a.recycle();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.coffee, container, false);

        Button coffee = view.findViewById(R.id.coffee);
        TextView thankYou = view.findViewById(R.id.thankYou);

        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName name) {
                inAppBillingService = null;
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                inAppBillingService = IInAppBillingService.Stub.asInterface(service);
                CallBilling callBilling = new CallBilling(
                        inAppBillingService,
                        Objects.requireNonNull(getActivity()).getPackageName()
                );
                callBilling.setOnResult(result -> {
                    if (result != null) {
                        if (result.thankYou) {
                            thankYou.setVisibility(View.VISIBLE);
                        } else {
                            coffee.setText(result.price);
                            coffee.setVisibility(View.VISIBLE);
                        }
                    }
                });
                callBilling.execute(itemCode);
            }
        };
        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        Objects.requireNonNull(getActivity()).bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        coffee.setOnClickListener(v -> {
            if (inAppBillingService != null) {
                try {
                    Bundle buyIntentBundle = inAppBillingService.getBuyIntent(
                            3,
                            getActivity().getPackageName(),
                            itemCode,
                            "inapp",
                            null
                    );
                    PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
                    getActivity().startIntentSenderForResult(
                            Objects.requireNonNull(pendingIntent).getIntentSender(),
                            Coffee.REQUEST_CODE,
                            new Intent(),
                            0,
                            0,
                            0,
                            null
                    );
                } catch (@NonNull RemoteException | IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
            }
        });
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (serviceConnection != null) {
            Objects.requireNonNull(getActivity()).unbindService(serviceConnection);
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @NonNull Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Coffee.REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
            try {
                JSONObject jo = new JSONObject(Objects.requireNonNull(purchaseData));
                String sku = jo.getString("productId");
                if (sku.equals(itemCode)) {
                    Objects.requireNonNull(getView()).findViewById(R.id.coffee).setVisibility(View.GONE);
                    getView().findViewById(R.id.thankYou).setVisibility(View.VISIBLE);
                }
            } catch (JSONException e) {
                Log.e("Main", "onActivityResult", e);
            }
        }
    }

    static class CallBilling extends AsyncTask<String, Void, CallBilling.Result> {

        private final IInAppBillingService inAppBillingService;
        private final String packageName;
        private OnResultCallback callback;

        CallBilling(IInAppBillingService inAppBillingService, String packageName) {
            this.inAppBillingService = inAppBillingService;
            this.packageName = packageName;
        }

        void setOnResult(OnResultCallback callback) {
            this.callback = callback;
        }

        @Override
        protected void onPostExecute(Result result) {
            callback.onResult(result);
        }

        @Nullable
        @Override
        protected Result doInBackground(String... items) {
            try {
                Bundle purchasesDetails = inAppBillingService.getPurchases(
                        3,
                        packageName,
                        "inapp",
                        null
                );
                int response = purchasesDetails.getInt("RESPONSE_CODE");
                if (response == 0) {
                    ArrayList<String> responseList = purchasesDetails.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
                    if (Objects.requireNonNull(responseList).contains(items[0])) {
                        return new Result();
                    } else {
                        Bundle querySkus = new Bundle();
                        ArrayList<String> itemIdList = new ArrayList<>();
                        itemIdList.add(items[0]);
                        querySkus.putStringArrayList("ITEM_ID_LIST", itemIdList);
                        try {
                            Bundle skuDetails = inAppBillingService.getSkuDetails(
                                    3,
                                    packageName,
                                    "inapp",
                                    querySkus
                            );
                            response = skuDetails.getInt("RESPONSE_CODE");
                            if (response == 0) {
                                responseList = skuDetails.getStringArrayList("DETAILS_LIST");

                                for (String thisResponse : Objects.requireNonNull(responseList)) {
                                    JSONObject object = new JSONObject(thisResponse);
                                    if (object.getString("productId").equals(items[0])) {
                                        return new Result(
                                                object.getString("title") +
                                                        " " +
                                                        object.getString("price")
                                        );
                                    }
                                }
                            } else {
                                Log.e("Main", skuDetails.toString());
                            }
                        } catch (@NonNull RemoteException | JSONException e) {
                            Log.e("Main", "getSkuDetails", e);
                        }
                    }
                } else {
                    Log.e("Main", purchasesDetails.toString());
                }
            } catch (RemoteException e) {
                Log.e("Main", "getPurchases", e);
            }
            return null;
        }

        interface OnResultCallback {
            void onResult(Result result);
        }

        static class Result {
            final boolean thankYou;
            @Nullable
            final String price;

            Result() {
                thankYou = true;
                price = null;
            }

            Result(@NonNull String price) {
                this.price = price;
                thankYou = false;
            }
        }
    }
}
