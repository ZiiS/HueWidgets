// Copyright © 2019 Barnaby Shearer <b@Zi.iS>

package is.zi.huewidgets;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import is.zi.NonNull;
import is.zi.Nullable;
import is.zi.hue.HueBridgeService;

abstract public class AlertActivity extends Activity {

    @Nullable
    protected HueBridgeService hueBridgeService;
    @Nullable
    private AlertDialog alertDialog;
    @Nullable
    private ServiceConnection connection;


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (hueBridgeService != null) {
            hueBridgeService.setOnAlertListener(null);
        }
        if (connection != null) {
            unbindService(connection);
        }
    }

    protected abstract void onServiceConnected();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isFinishing()) {
            connection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName className, @NonNull IBinder service) {
                    hueBridgeService = ((HueBridgeService.LocalBinder) service).getService();

                    hueBridgeService.setOnAlertListener(alert -> {
                        if (alertDialog != null) {
                            alertDialog.dismiss();
                            alertDialog = null;
                        }
                        if (alert != null) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(AlertActivity.this);
                            builder.setTitle(R.string.error_title);
                            builder.setIcon(android.R.drawable.ic_dialog_alert);
                            builder.setMessage(alert);
                            alertDialog = builder.create();
                            alertDialog.show();
                        }
                    });
                    AlertActivity.this.onServiceConnected();
                }

                @Override
                public void onServiceDisconnected(ComponentName className) {
                    hueBridgeService = null;
                }
            };

            bindService(
                    new Intent(
                            this,
                            HueBridgeService.class
                    ),
                    connection,
                    Context.BIND_AUTO_CREATE
            );
        }
    }
}
