// Copyright © 2019 Barnaby Shearer <b@Zi.iS>

package is.zi.comm;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import is.zi.NonNull;
import is.zi.Nullable;

public class Http implements AutoCloseable {
    @NonNull
    private final URL base;
    @NonNull
    private final String path;
    @Nullable
    public X509Certificate cert;
    @Nullable
    private HttpsURLConnection conn;

    public Http(@NonNull URL base, @NonNull String path, @Nullable X509Certificate cert) {
        this.base = base;
        this.path = path;
        this.cert = cert;
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[]{
                    new X509TrustManager() {

                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                            throw new CertificateException();
                        }

                        @Override
                        public void checkServerTrusted(@NonNull X509Certificate[] chain, String authType) throws CertificateException {
                            if (Http.this.cert == null) {
                                Http.this.cert = chain[chain.length - 1];
                                Log.i("Http", "Trusting new cert " + Http.this.cert.getSubjectDN().getName());
                            } else {
                                try {
                                    chain[chain.length - 1].verify(Objects.requireNonNull(cert).getPublicKey());
                                } catch (@NonNull NullPointerException | InvalidKeyException | NoSuchAlgorithmException | NoSuchProviderException | SignatureException e) {
                                    throw new CertificateException(e);
                                }
                            }
                        }

                        @NonNull
                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[]{};
                        }
                    }
            }, null);
            HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
            //Tofu dosn't care about hostname
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (@NonNull NoSuchAlgorithmException | KeyManagementException e) {
            Log.e("Http", "TLS", e);
        }
    }

    @NonNull
    private static byte[] read(@NonNull InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    @NonNull
    public HttpsURLConnection getConnection(@NonNull URL url) throws IOException {
        return (HttpsURLConnection) url.openConnection();
    }

    @NonNull
    public Bitmap decodeStream(InputStream stream) {
        return BitmapFactory.decodeStream(stream);
    }

    @NonNull
    public XPathNode xml(@NonNull String ns, @NonNull String root)
            throws IOException, XPathExpressionException, HttpException {
        conn = getConnection(new URL(base, path));
        conn.setReadTimeout(1000);
        conn.setConnectTimeout(1500);
        conn.connect();
        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xPath = factory.newXPath();
            xPath.setNamespaceContext(new NamespaceContext() {
                @NonNull
                @Override
                public String getNamespaceURI(String prefix) {
                    return ns;
                }

                @Nullable
                @Override
                public Iterator<String> getPrefixes(String val) {
                    return null;
                }

                @Nullable
                @Override
                public String getPrefix(String uri) {
                    return null;
                }
            });

            return new XPathNode(
                    xPath,
                    (Node) xPath.evaluate(
                            root,
                            new InputSource(conn.getInputStream()), XPathConstants.NODE)
            );
        } else {
            throw new HttpException(conn.getResponseCode(), conn.getResponseMessage());
        }
    }

    @NonNull
    public Bitmap bitmap() throws IOException, HttpException {
        conn = getConnection(new URL(base, path));
        conn.setReadTimeout(1000);
        conn.setConnectTimeout(1500);
        conn.connect();
        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
            return decodeStream(conn.getInputStream());
        } else {
            throw new HttpException(conn.getResponseCode(), conn.getResponseMessage());
        }
    }

    @NonNull
    public JSONObject json() throws HttpException, JSONException, IOException, ApplicationLayerException {
        return json(null);
    }

    @NonNull
    public JSONObject json(@Nullable String data) throws HttpException, JSONException, IOException, ApplicationLayerException {
        conn = getConnection(new URL(base, path));
        conn.setReadTimeout(1000);
        conn.setConnectTimeout(1500);
        if (data != null) {
            if (path.charAt(path.length() - 1) == '/') {
                conn.setRequestMethod("POST");
            } else {
                conn.setRequestMethod("PUT");
            }
            conn.setDoOutput(true);
        }
        conn.connect();
        if (data != null) {
            try (OutputStream out = conn.getOutputStream()) {
                out.write(data.getBytes());
            }
        }
        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
            try (InputStream in = conn.getInputStream()) {
                JSONObject json;
                String raw = new String(Http.read(in));
                if (raw.charAt(0) == '[') {
                    json = new JSONArray(raw).getJSONObject(0);
                } else {
                    json = new JSONObject(raw);
                }
                if (json.has("error")) {
                    throw new ApplicationLayerException(
                            json.getJSONObject("error").getInt("type"),
                            json.getJSONObject("error").getString("description")
                    );
                }
                return json;
            }
        } else {
            throw new HttpException(conn.getResponseCode(), conn.getResponseMessage());
        }
    }

    @Override
    public void close() {
        if (conn != null) {
            conn.disconnect();
        }
    }

    public static class ApplicationLayerException extends Exception {
        public static final int UNAUTHORIZED = 1;
        public static final int NOT_PRESSED = 101;
        private static final long serialVersionUID = -4787910976320874857L;
        public final int type;

        public ApplicationLayerException(int type, String message) {
            super(message);
            this.type = type;
        }
    }

    public class XPathNode {
        @NonNull
        public final XPath xpath;
        @NonNull
        public final Node node;

        XPathNode(@NonNull XPath xpath, @NonNull Node node) {
            this.xpath = xpath;
            this.node = node;
        }
    }

    public class HttpException extends Exception {
        private static final long serialVersionUID = 8707153568373554106L;
        @SuppressWarnings("unused")
        final int status;

        HttpException(int status, String message) {
            super(message);
            this.status = status;
        }
    }
}
