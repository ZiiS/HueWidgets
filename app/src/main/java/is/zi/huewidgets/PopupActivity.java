// Copyright © 2019 Barnaby Shearer <b@Zi.iS>

package is.zi.huewidgets;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONException;

import is.zi.Nullable;
import is.zi.hue.HueColorPicker;
import is.zi.hue.HueLight;


public class PopupActivity extends AlertActivity {

    @Nullable
    private HueLight light;

    @Override
    protected void onDestroy() {
        if (hueBridgeService != null && light != null) {
            hueBridgeService.setOnColorListener(light, null);
        }
        super.onDestroy();
    }

    @Override
    protected void onServiceConnected() {
        assert hueBridgeService != null;
        assert light != null;
        HueColorPicker lightColor = findViewById(R.id.lightColor);
        hueBridgeService.setOnColorListener(light, lightColor::setColor);
        lightColor.setOnColorListener((c) -> hueBridgeService.setColor(light, c), light.getInterval());
        hueBridgeService.getColor(light);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        overridePendingTransition(0, 0);

        int mAppWidgetId = getIntent().getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);

        try {
            //noinspection deprecation
            light = new HueLight(PreferenceManager.getDefaultSharedPreferences(
                    getApplicationContext()
            ).getString("light_" + mAppWidgetId, null));
        } catch (JSONException e) {
            Log.e("HueAppWidgetPopup", "Invalid light for widget " + mAppWidgetId, e);
            Intent intent = new Intent(this, ConfigureActivity.class);
            intent.putExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    mAppWidgetId
            );
            startActivity(intent);
            setResult(Activity.RESULT_CANCELED);
            finish();
            super.onCreate(savedInstanceState);
            return;
        }

        setContentView(R.layout.popup);

        HueColorPicker lightColor = findViewById(R.id.lightColor);

        try {
            lightColor.setGamut(light.getGamut());
        } catch (JSONException e) {
            Log.e("HueAppWidgetPopup", "Invalid light for widget " + mAppWidgetId, e);
        }

        super.onCreate(savedInstanceState);
    }


}


