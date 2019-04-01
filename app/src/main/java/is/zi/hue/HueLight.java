// Copyright © 2019 Barnaby Shearer <b@Zi.iS>

package is.zi.hue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import is.zi.NonNull;
import is.zi.Nullable;

public class HueLight {
    @NonNull
    private final JSONObject data;

    public HueLight(@Nullable String raw) throws JSONException {
        if (raw == null) {
            throw new JSONException("Null");
        }
        data = new JSONObject(raw);
    }

    public HueLight(@NonNull String path, @NonNull JSONObject data) throws JSONException {
        //TODO should store which account this light uses.
        this.data = data;
        data.put("path", path);
    }

    @NonNull
    public String getPath() throws JSONException {
        return data.getString("path");
    }

    @NonNull
    public String getStatePath() throws JSONException {
        return getPath() + (isGroup() ? "/action" : "/state");
    }

    @Override
    @NonNull
    public String toString() {
        try {
            return data.getString("name");
        } catch (JSONException e) {
            return "Unknown";
        }
    }

    public String toString(int indent) throws JSONException {
        return data.toString(indent);
    }

    private boolean isGroup() {
        try {
            return data.getString("path").startsWith("/groups/");
        } catch (JSONException e) {
            return false;
        }
    }

    public int getInterval() {
        return isGroup() ? 1000 : 100;
    }

    @NonNull
    public float[] getGamut() throws JSONException {
        if (
                data.has("capabilities")
                        && data.getJSONObject("capabilities").has("control")
                        && data.getJSONObject("capabilities").getJSONObject("control").has("colorgamut")
        ) {
            JSONArray gamut = data.getJSONObject("capabilities")
                    .getJSONObject("control")
                    .getJSONArray("colorgamut");
            return new float[]{
                    (float) gamut.getJSONArray(0).getDouble(0),
                    (float) gamut.getJSONArray(0).getDouble(1),
                    (float) gamut.getJSONArray(1).getDouble(0),
                    (float) gamut.getJSONArray(1).getDouble(1),
                    (float) gamut.getJSONArray(2).getDouble(0),
                    (float) gamut.getJSONArray(2).getDouble(1),
            };
        } else {
            //TODO Groups could combine all thier lights
            return new float[]{
                    1f, 0f,
                    0f, 1f,
                    0f, 0f
            };
        }
    }
}