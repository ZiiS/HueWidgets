package is.zi.hue;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import is.zi.NonNull;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

public class HueLightTest {
    @NonNull
    final private static String HUE_EXAMPLE = "{\"state\":{\"on\":false,\"bri\":1,\"hue\":33761,\"sat\":254,\"effect\":\"none\",\"xy\":[0.3171,0.3366],\"ct\":159,\"alert\":\"none\",\"colormode\":\"xy\",\"mode\":\"homeautomation\",\"reachable\":true},\"swupdate\":{\"state\":\"noupdates\",\"lastinstall\":\"2018-01-02T19:24:20\"},\"type\":\"Extended color light\",\"name\":\"Hue color lamp 7\",\"modelid\":\"LCT007\",\"manufacturername\":\"Philips\",\"productname\":\"Hue color lamp\",\"capabilities\":{\"certified\":true,\"control\":{\"mindimlevel\":5000,\"maxlumen\":600,\"colorgamuttype\":\"B\",\"colorgamut\":[[0.675,0.322],[0.409,0.518],[0.167,0.04]],\"ct\":{\"min\":153,\"max\":500}},\"streaming\":{\"renderer\":true,\"proxy\":false}},\"config\":{\"archetype\":\"sultanbulb\",\"function\":\"mixed\",\"direction\":\"omnidirectional\"},\"uniqueid\":\"00:17:88:01:00:bd:c7:b9-0b\",\"swversion\":\"5.105.0.21169\"}";

    @Test
    public void constructor_notnull() throws JSONException {
        new HueLight("{}");
    }

    @Test(expected = JSONException.class)
    public void constructor_null() throws JSONException {
        new HueLight(null);
    }

    @Test
    public void constructor_json() throws JSONException {
        new HueLight("lights/1", new JSONObject(HueLightTest.HUE_EXAMPLE));
    }

    @Test
    public void toString_unknown() throws JSONException {
        assertThat(
                new HueLight("{}").toString(),
                equalTo("Unknown")
        );
    }

    @Test
    public void toString_known() throws JSONException {
        assertThat(
                new HueLight(HueLightTest.HUE_EXAMPLE).toString(),
                equalTo("Hue color lamp 7")
        );
    }

    @Test
    public void toString_json() throws JSONException {
        assertNotNull(
                new HueLight(HueLightTest.HUE_EXAMPLE).toString(1)
        );
    }

    @Test
    public void getGamut() throws JSONException {
        assertThat(
                new HueLight(HueLightTest.HUE_EXAMPLE).getGamut(),
                equalTo(new float[]{
                        0.675f, 0.322f, 0.409f, 0.518f, 0.167f, 0.04f
                })
        );
    }

    @Test
    public void getPath() throws JSONException {
        assertThat(
                new HueLight("lights/1", new JSONObject(HueLightTest.HUE_EXAMPLE)).getPath(),
                equalTo("lights/1")
        );
    }

}
