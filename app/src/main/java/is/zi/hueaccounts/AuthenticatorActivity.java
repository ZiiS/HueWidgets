// Copyright © 2019 Barnaby Shearer <b@Zi.iS>

package is.zi.hueaccounts;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;

import java.security.cert.CertificateEncodingException;
import java.util.Objects;

import is.zi.NonNull;
import is.zi.hue.HueBridge;
import is.zi.huewidgets.AlertActivity;
import is.zi.huewidgets.R;

public class AuthenticatorActivity extends AlertActivity {

    private AccountAuthenticatorResponse mAccountAuthenticatorResponse;

    @Override
    protected void onDestroy() {
        if (hueBridgeService != null) {
            hueBridgeService.setOnBridgesListener(null);
            hueBridgeService.setOnPairListener(null);
        }
        super.onDestroy();
    }


    @SuppressWarnings("deprecation")
    @Override
    protected void onServiceConnected() {
        assert hueBridgeService != null;
        hueBridgeService.setOnBridgesListener(bridges -> {
            if (bridges != null) {
                ListView view = findViewById(android.R.id.list);
                view.setAdapter(
                        new ArrayAdapter<HueBridge>(
                                AuthenticatorActivity.this,
                                R.layout.bridge_list_item,
                                android.R.id.text1,
                                bridges
                        ) {
                            @Override
                            @NonNull
                            public View getView(int position, View
                                    convertView, @NonNull ViewGroup parent) {
                                View view = super.getView(position, convertView, parent);
                                HueBridge item = getItem(position);
                                if (item != null) {
                                    if (item.getIcon() != null) {
                                        ((ImageView) view.findViewById(android.R.id.icon1))
                                                .setImageBitmap(item.getIcon());
                                    }
                                }
                                return view;
                            }
                        }
                );
                if (bridges.length != 0) {
                    hueBridgeService.getPair();
                    view.setVisibility(View.VISIBLE);
                }
            }
            new Handler().postDelayed(() -> hueBridgeService.getPair(), 3000);
        });
        hueBridgeService.setOnPairListener(paired -> {
            if (paired == null) {
                new Handler().postDelayed(() -> hueBridgeService.getPair(), 100);
                return;
            }
            Log.d("Paired", "Paired");
            AccountManager am = AccountManager.get(this);
            Account account = new Account(paired.getName(), AccountAuthenticatorService.ACCOUNT_TYPE);
            Bundle bundle = new Bundle();
            bundle.putString(AccountAuthenticatorService.KEY_URL, Objects.requireNonNull(paired.getUrl()).toString());
            try {
                bundle.putString(
                        AccountAuthenticatorService.KEY_CERT,
                        Base64.encodeToString(Objects.requireNonNull(paired.getCert()).getEncoded(), Base64.DEFAULT)
                );
            } catch (CertificateEncodingException e) {
                Log.e("AuthenticatorActivity", "bad cert", e);
            }
            am.addAccountExplicitly(account, paired.getUsername(), bundle);
            Bundle mResultBundle = new Bundle();
            mResultBundle.putString(AccountManager.KEY_ACCOUNT_NAME, paired.getName());
            mResultBundle.putString(AccountManager.KEY_ACCOUNT_TYPE, AccountAuthenticatorService.ACCOUNT_TYPE);
            mResultBundle.putString(AccountManager.KEY_AUTHTOKEN, paired.getUsername());
            mAccountAuthenticatorResponse.onResult(mResultBundle);
            finish();
        });
        hueBridgeService.getBridges();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        //noinspection deprecation
        mAccountAuthenticatorResponse =
                getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
        if (mAccountAuthenticatorResponse != null) {
            mAccountAuthenticatorResponse.onRequestContinued();
        }

        setContentView(R.layout.bridge_list);

        findViewById(R.id.ip_add).setOnClickListener(event -> {
            if(hueBridgeService!=null)
                hueBridgeService.addBridge("https://" + ((EditText)findViewById(R.id.ip)).getText().toString());
        });
        super.onCreate(savedInstanceState);
    }


}
