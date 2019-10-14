// Copyright © 2019 Barnaby Shearer <b@Zi.iS>

package is.zi.huewidgets;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.service.quicksettings.TileService;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONException;

import java.util.Objects;

import is.zi.NonNull;
import is.zi.hue.HueLight;


public class ConfigureActivity extends AlertActivity {

    @Override
    protected void onDestroy() {
        if (hueBridgeService != null) {
            hueBridgeService.setOnLightsListener(null);
        }
        super.onDestroy();
    }

    @Override
    protected void onServiceConnected() {
        assert hueBridgeService != null;
        hueBridgeService.setOnLightsListener(lights -> {
            if (lights != null) {
                ListView view = findViewById(android.R.id.list);
                view.setAdapter(
                        new ArrayAdapter<>(
                                ConfigureActivity.this,
                                android.R.layout.simple_list_item_1,
                                android.R.id.text1,
                                lights
                        )
                );
                if (lights.length != 0) {
                    view.setVisibility(View.VISIBLE);
                }
            } else {
                new Handler().postDelayed(() -> hueBridgeService.getLights(), 3000);
            }
        });
        hueBridgeService.getLights();
    }

        private void installShortcut(@NonNull Intent intent, String id, @NonNull String name) {
        if (Build.VERSION.SDK_INT >= 26) {
            ShortcutInfo shortcut = new ShortcutInfo.Builder(this, id)
                    .setShortLabel(name)
                    .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
                    .setIntent(intent)
                    .build();
            Objects.requireNonNull(getSystemService(ShortcutManager.class)).requestPinShortcut(shortcut, null);
        } else {
            Intent installIntent = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
            installIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
            installIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(this, R.mipmap.ic_launcher));
            installIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
            sendBroadcast(installIntent);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        int mAppWidgetId;
        boolean widget;
        setResult(Activity.RESULT_CANCELED);

        {
            Intent intent = getIntent();
            Bundle extras = intent.getExtras();

            if (intent.getAction() != null && intent.getAction().equals(TileService.ACTION_QS_TILE_PREFERENCES)) {
                mAppWidgetId = -1;
                widget = false;
            } else if (extras != null && extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
            ) != AppWidgetManager.INVALID_APPWIDGET_ID) {
                mAppWidgetId = extras.getInt(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID
                );
                widget = true;
            } else {
                mAppWidgetId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
                widget = false;
            }
        }

        setContentView(R.layout.configure);

        ListView view = findViewById(android.R.id.list);

        view.setOnItemClickListener((parent, view1, position, id) -> {
            @SuppressWarnings("deprecation") SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(
                    getApplicationContext()
            ).edit();
            try {
                prefs.putString("light_" + mAppWidgetId, ((HueLight) parent.getItemAtPosition(position)).toString(0));
            } catch (JSONException e) {
                Log.e("Configure", "Can't reserialize light data", e);
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
            prefs.apply();

            if (mAppWidgetId != -1) {
                if (widget) {

                    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
                    WidgetProvider.doUpdate(this, appWidgetManager, mAppWidgetId);

                    Intent resultValue = new Intent();
                    resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                    setResult(Activity.RESULT_OK, resultValue);
                } else {
                    Intent intent = new Intent(ConfigureActivity.this, PopupActivity.class);
                    intent.setAction(Intent.ACTION_MAIN);
                    intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                    installShortcut(intent, "light_" + mAppWidgetId, (parent.getItemAtPosition(position)).toString());
                }
            }
            finish();
        });

        super.onCreate(savedInstanceState);
    }

}