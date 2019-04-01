package is.zi.hue;


import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class HueColorTest {
    @Test
    public void toString_dark() {
        assertThat(
                new HueColor(0.33f, 0.33f, 0).toString(null),
                equalTo("{\"transitiontime\": 0, \"on\": false}")
        );
    }

    @Test
    public void toString_nearlyDark() {
        assertThat(
                new HueColor(0.33f, 0.33f, 1).toString(null),
                equalTo("{\"transitiontime\": 0, \"on\": true, \"bri\": 1, \"xy\": [0.330000, 0.330000]}")
        );
    }

    @Test
    public void constructor_nearlyDark() throws JSONException {
        assertThat(
                new HueColor(new JSONObject("{\"transitiontime\": 0, \"on\": true, \"bri\": 1, \"xy\": [0.330000, 0.330000]}")).toString(null),
                equalTo(new HueColor(0.33f, 0.33f, 1).toString(null))
        );
    }

    @Test
    public void constructor_dark() throws JSONException {
        assertThat(
                new HueColor(new JSONObject("{\"transitiontime\": 0, \"on\": true, \"bri\": 0, \"xy\": [0.330000, 0.330000]}")).toString(null),
                equalTo(new HueColor(0.33f, 0.33f, 0).toString(null))
        );
    }
}
