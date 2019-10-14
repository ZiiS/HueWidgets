// Copyright © 2019 Barnaby Shearer <b@Zi.iS>

package is.zi.huewidgets;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.service.quicksettings.Tile;

import org.json.JSONException;

import java.util.Objects;

import is.zi.NonNull;
import is.zi.Nullable;
import is.zi.hue.HueBridgeService;
import is.zi.hue.HueLight;

@TargetApi(Build.VERSION_CODES.N)
public class QSTileService extends android.service.quicksettings.TileService {
    @Nullable
    private AlertDialog alertDialog;
    @Nullable
    private ServiceConnection connection;
    @Nullable
    private HueBridgeService hueBridgeService;

    @Override
    public void onTileAdded() {
        Intent intent = new Intent(this, ConfigureActivity.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityAndCollapse(intent);
    }

    @Override
    public void onTileRemoved() {
        //noinspection deprecation
        PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext()
        ).edit().remove("light_-1").apply();
    }

    @Override
    public void onStartListening() {
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
                        AlertDialog.Builder builder = new AlertDialog.Builder(QSTileService.this);
                        builder.setTitle(R.string.error_title);
                        builder.setIcon(android.R.drawable.ic_dialog_alert);
                        builder.setMessage(alert);
                        alertDialog = builder.create();
                        showDialog(alertDialog);
                    }
                });
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

    @Override
    public void onStopListening() {
        unbindService(connection);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onClick() {
        HueLight light;
        try {
            light = new HueLight(
                    PreferenceManager.getDefaultSharedPreferences(
                            getApplicationContext()
                    ).getString("light_-1", null)
            );
        } catch (JSONException e) {
            onTileAdded();
            return;
        }

        Tile tile = getQsTile();
        switch (tile.getState()) {
            case Tile.STATE_ACTIVE:
                Objects.requireNonNull(hueBridgeService).setOn(light, false);
                tile.setState(Tile.STATE_INACTIVE);
                break;
            case Tile.STATE_INACTIVE:
                Objects.requireNonNull(hueBridgeService).setOn(light, true);
                tile.setState(Tile.STATE_ACTIVE);
                break;
        }
        tile.updateTile();
    }
}