package is.zi.hue;

import android.graphics.Bitmap;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.xpath.XPathExpressionException;

import is.zi.NonNull;
import is.zi.comm.Http;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class HueBridgeTest {
    @NonNull
    final private static String HUE_EXAMPLE_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><root xmlns=\"urn:schemas-upnp-org:device-1-0\"><specVersion><major>1</major><minor>0</minor></specVersion><URLBase>http://192.0.2.1:80/</URLBase><device><deviceType>urn:schemas-upnp-org:device:Basic:1</deviceType><friendlyName>Philips hue (192.0.2.1)</friendlyName><manufacturer>Royal Philips Electronics</manufacturer><manufacturerURL>http://www.philips.com</manufacturerURL><modelDescription>Philips hue Personal Wireless Lighting</modelDescription><modelName>Philips hue bridge 2015</modelName><modelNumber>BSB002</modelNumber><modelURL>http://www.meethue.com</modelURL><serialNumber>000000000000</serialNumber><UDN>uuid:dbe2fdab-f64b-4fa3-8785-7c2adde8d432</UDN><presentationURL>index.html</presentationURL><iconList><icon><mimetype>image/png</mimetype><height>48</height><width>48</width><depth>24</depth><url>hue_logo_0.png</url></icon></iconList></device></root>";
    @NonNull
    final private static String HUE_EXAMPLE_LIGHTS = "{\"lights\":{\"1\":{\"state\":{\"on\":false,\"bri\":1,\"hue\":33761,\"sat\":254,\"effect\":\"none\",\"xy\":[0.3171,0.3366],\"ct\":159,\"alert\":\"none\",\"colormode\":\"xy\",\"mode\":\"homeautomation\",\"reachable\":true},\"swupdate\":{\"state\":\"noupdates\",\"lastinstall\":\"2018-01-02T19:24:20\"},\"type\":\"Extended color light\",\"name\":\"Hue color lamp 7\",\"modelid\":\"LCT007\",\"manufacturername\":\"Philips\",\"productname\":\"Hue color lamp\",\"capabilities\":{\"certified\":true,\"control\":{\"mindimlevel\":5000,\"maxlumen\":600,\"colorgamuttype\":\"B\",\"colorgamut\":[[0.675,0.322],[0.409,0.518],[0.167,0.04]],\"ct\":{\"min\":153,\"max\":500}},\"streaming\":{\"renderer\":true,\"proxy\":false}},\"config\":{\"archetype\":\"sultanbulb\",\"function\":\"mixed\",\"direction\":\"omnidirectional\"},\"uniqueid\":\"00:17:88:01:00:bd:c7:b9-0b\",\"swversion\":\"5.105.0.21169\"}}}";

    @NonNull
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @NonNull
    @Spy
    private HueBridge bridge = new HueBridge("http://127.0.0.1");

    public HueBridgeTest() throws MalformedURLException {
        super();
    }

    @Test
    public void toString_fallback() {
        assertThat(
                bridge.toString(),
                equalTo("http://127.0.0.1")
        );
    }

    @Test
    public void load() throws IOException, XPathExpressionException, Http.HttpException {
        Http http = spy(new Http(new URL("http://127.0.0.1"), "/test", null));
        HttpURLConnection conn = mock(HttpsURLConnection.class);
        when(conn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(conn.getInputStream()).thenReturn(new ByteArrayInputStream(HueBridgeTest.HUE_EXAMPLE_XML.getBytes(StandardCharsets.UTF_8)));
        doReturn(conn).when(http).getConnection(any());

        Http httpLogo = spy(new Http(new URL("http://127.0.0.1"), "/test", null));
        Bitmap bitmap = mock(Bitmap.class);
        HttpURLConnection connLogo = mock(HttpsURLConnection.class);
        when(connLogo.getResponseCode()).thenReturn(HttpsURLConnection.HTTP_OK);
        when(connLogo.getInputStream()).thenReturn(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)));
        doReturn(bitmap).when(httpLogo).decodeStream(any());
        doReturn(connLogo).when(httpLogo).getConnection(any());

        doReturn(http).when(bridge).getHttp(eq("/description.xml"));
        doReturn(httpLogo).when(bridge).getHttp(eq("/hue_logo_0.png"));

        bridge.load();
    }

    @Test
    public void pair() throws JSONException, Http.HttpException, HueBridge.NotPressedException, IOException, Http.ApplicationLayerException {
        Http http = mock(Http.class);
        when(http.json(any())).thenReturn(new JSONObject("{\"success\": {\"username\": \"Foobar\"}}"));
        doReturn(http).when(bridge).getHttp(any());
        bridge.pair();
    }

    @Test(expected = HueBridge.NotPressedException.class)
    public void pair_notpressed() throws JSONException, Http.HttpException, HueBridge.NotPressedException, IOException, Http.ApplicationLayerException {
        Http http = mock(Http.class);
        when(http.json(any())).thenThrow(new Http.ApplicationLayerException(Http.ApplicationLayerException.NOT_PRESSED, "Foobar"));
        doReturn(http).when(bridge).getHttp(any());
        bridge.pair();
    }


    @Test
    public void getLights() throws JSONException, Http.HttpException, IOException, Http.ApplicationLayerException {
        Http http = mock(Http.class);
        when(http.json()).thenReturn(new JSONObject(HueBridgeTest.HUE_EXAMPLE_LIGHTS));
        doReturn(http).when(bridge).getHttp(any());
        bridge.getLights();
    }


}
