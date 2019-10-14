// Copyright © 2019 Barnaby Shearer <b@Zi.iS>

package is.zi.hue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import is.zi.NonNull;
import is.zi.Nullable;

public class HueColor {
    final public int bri;
    final public float x;
    final public float y;

    HueColor(@NonNull JSONObject json) throws JSONException {
        if (json.has("xy")) {
            JSONArray xy = json.getJSONArray("xy");
            x = (float) xy.getDouble(0);
            y = (float) xy.getDouble(1);
        } else {
            x = 0.32f;
            y = 0.32f;
        }
        if (json.getBoolean("on")) {
            if (json.has("bri")) {
                bri = json.getInt("bri");
            } else {
                bri = 255;
            }
        } else {
            bri = 0;
        }
    }

    HueColor(float x, float y, int bri) {
        this.x = Math.max(0f, Math.min(1f, x));
        this.y = Math.max(0f, Math.min(1f, y));
        this.bri = Math.max(0, Math.min(255, bri));
    }

    @NonNull
    public String toString(@Nullable HueColor other) {
        if (bri == 0) {
            return "{\"transitiontime\": 0, \"on\": false}";
        }
        if (other != null) {
            if (other.bri == bri) {
                return String.format(Locale.ENGLISH, "{\"transitiontime\": 0, \"xy\": [%f, %f]}", x, y);
            }
            if (x == other.x && y == other.y) {
                if (other.bri != 0) {
                    return String.format(Locale.ENGLISH, "{\"transitiontime\": 0, \"bri\": %d}", bri);
                }
                return String.format(Locale.ENGLISH, "{\"transitiontime\": 0, \"on\": true, \"bri\": %d}", bri);
            }
        }
        return String.format(Locale.ENGLISH, "{\"transitiontime\": 0, \"on\": true, \"bri\": %d, \"xy\": [%f, %f]}", bri, x, y);
    }

}
