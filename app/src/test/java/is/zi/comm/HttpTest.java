package is.zi.comm;

import android.graphics.Bitmap;

import org.json.JSONException;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.xpath.XPathExpressionException;

import is.zi.NonNull;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class HttpTest {

    @NonNull
    @Spy
    private final Http http = new Http(new URL("http://127.0.0.1"), "/test", null);
    @NonNull
    @Spy
    private final Http httpCollection = new Http(new URL("http://127.0.0.1"), "/test/", null);
    @NonNull
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    @Mock
    private HttpsURLConnection conn;

    @Mock
    private Bitmap bitmap;

    public HttpTest() throws IOException {
        super();
    }

    @Test
    public void constructor() throws MalformedURLException {
        new Http(new URL("http://127.0.0.1"), "/test", null);
    }

    @Test
    public void xml() throws IOException, XPathExpressionException, Http.HttpException {
        when(conn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(conn.getInputStream()).thenReturn(new ByteArrayInputStream("<xml />".getBytes(StandardCharsets.UTF_8)));
        doReturn(conn).when(http).getConnection(any());
        http.xml("uri:test", "/test:xml");
    }

    @Test
    public void json() throws IOException, Http.HttpException, JSONException, Http.ApplicationLayerException {
        when(conn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(conn.getInputStream()).thenReturn(new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)));
        doReturn(conn).when(http).getConnection(any());
        http.json();
    }

    @Test(expected = Http.ApplicationLayerException.class)
    public void json_err() throws IOException, Http.HttpException, JSONException, Http.ApplicationLayerException {
        when(conn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(conn.getInputStream()).thenReturn(new ByteArrayInputStream("{\"error\": {\"type\": 1, \"description\": \"Foobar\"}}".getBytes(StandardCharsets.UTF_8)));
        doReturn(conn).when(http).getConnection(any());
        http.json();
    }

    @Test
    public void json_put() throws IOException, Http.HttpException, JSONException, Http.ApplicationLayerException {
        when(conn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(conn.getInputStream()).thenReturn(new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)));
        when(conn.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        doReturn(conn).when(http).getConnection(any());
        http.json("{}");
    }

    @Test
    public void json_post() throws IOException, Http.HttpException, JSONException, Http.ApplicationLayerException {
        when(conn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(conn.getInputStream()).thenReturn(new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)));
        when(conn.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        doReturn(conn).when(httpCollection).getConnection(any());
        httpCollection.json("{}");
    }

    @Test(expected = Http.HttpException.class)
    public void json_error() throws IOException, Http.HttpException, JSONException, Http.ApplicationLayerException {
        when(conn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_NOT_FOUND);
        doReturn(conn).when(http).getConnection(any());
        http.json();
    }

    @Test
    public void bitmap() throws IOException, Http.HttpException {
        when(conn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(conn.getInputStream()).thenReturn(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)));
        doReturn(conn).when(http).getConnection(any());
        doReturn(bitmap).when(http).decodeStream(any());
        http.bitmap();
    }

}
