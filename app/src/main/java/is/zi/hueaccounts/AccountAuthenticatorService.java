// Copyright © 2019 Barnaby Shearer <b@Zi.iS>

package is.zi.hueaccounts;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import is.zi.NonNull;
import is.zi.Nullable;

import static android.accounts.AccountManager.KEY_BOOLEAN_RESULT;

public class AccountAuthenticatorService extends Service {
    @NonNull
    public static final String KEY_CERT = "KEY_CERT";
    @NonNull
    public static final String KEY_URL = "KEY_URL";
    // This must match the authenticator.xml value or crytic errors abound.
    @NonNull
    public static final String ACCOUNT_TYPE = "huewidgets.zi.is";
    @NonNull
    public static final String TOKEN_TYPE = "username";

    @Override
    public IBinder onBind(Intent intent) {
        return new AccountAuthenticator(this).getIBinder();
    }

    static class AccountAuthenticator extends AbstractAccountAuthenticator {

        private final Context mContext;

        AccountAuthenticator(Context context) {
            super(context);
            mContext = context;
        }

        @SuppressWarnings("deprecation")
        @NonNull
        @Override
        public Bundle addAccount(
                AccountAuthenticatorResponse response,
                String accountType,
                String authTokenType,
                String[] requiredFeatures,
                Bundle options
        ) {
            Intent intent = new Intent(mContext, AuthenticatorActivity.class);
            intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
            Bundle bundle = new Bundle();
            bundle.putParcelable(AccountManager.KEY_INTENT, intent);
            return bundle;
        }

        @SuppressWarnings("deprecation")
        @NonNull
        @Override
        public Bundle getAuthToken(
                AccountAuthenticatorResponse response,
                @NonNull Account account, String authTokenType,
                Bundle options
        ) {
            AccountManager am = AccountManager.get(mContext);
            if (am.getPassword(account) != null) {
                Bundle result = new Bundle();
                result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
                result.putString(AccountManager.KEY_AUTHTOKEN, am.getPassword(account));
                return result;
            }

            Intent intent = new Intent(mContext, AuthenticatorActivity.class);
            intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
            Bundle bundle = new Bundle();
            bundle.putParcelable(AccountManager.KEY_INTENT, intent);
            return bundle;
        }


        @Override
        public String getAuthTokenLabel(
                String authTokenType
        ) {
            return authTokenType;
        }

        @NonNull
        @Override
        public Bundle hasFeatures(
                AccountAuthenticatorResponse response,
                Account account,
                String[] features
        ) {
            Bundle result = new Bundle();
            result.putBoolean(KEY_BOOLEAN_RESULT, false);
            return result;
        }

        @Nullable
        @Override
        public Bundle editProperties(
                AccountAuthenticatorResponse response,
                String accountType
        ) {
            return null;
        }

        @Nullable
        @Override
        public Bundle confirmCredentials(
                AccountAuthenticatorResponse response,
                Account account,
                Bundle options
        ) {
            return null;
        }

        @Nullable
        @Override
        public Bundle updateCredentials(
                AccountAuthenticatorResponse response,
                Account account,
                String authTokenType,
                Bundle options
        ) {
            return null;
        }
    }
}