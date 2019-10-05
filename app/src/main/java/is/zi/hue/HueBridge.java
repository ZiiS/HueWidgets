// Copyright © 2019 Barnaby Shearer <b@Zi.iS>

package is.zi.hue;

import android.graphics.Bitmap;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.xml.xpath.XPathExpressionException;

import is.zi.NonNull;
import is.zi.Nullable;
import is.zi.comm.Http;
import is.zi.comm.HttpMU;

public class HueBridge {
    @NonNull
    private final URL url;
    @Nullable
    protected String name = null;
    @Nullable
    private Bitmap icon = null;
    @Nullable
    private String username = null;
    @Nullable
    private X509Certificate cert = null;

    public HueBridge(@NonNull String url) throws MalformedURLException {
        this.url = new URL(url);
    }

    public HueBridge(@NonNull String url, @NonNull String username, @Nullable X509Certificate cert) throws MalformedURLException {
        //TODO Should handle bridge moving to new url
        this.url = new URL(url);
        this.username = username;
        this.cert = cert;
    }

    @NonNull
    public Http getHttp(@NonNull String path) {
        return new Http(url, path, cert);
    }

    public void load() throws IOException, Http.HttpException, XPathExpressionException {
        try (Http http = getHttp("/description.xml")) {
            Http.XPathNode xml = http.xml(
                    "urn:schemas-upnp-org:device-1-0",
                    "/upnp:root/upnp:device"
            );
            name = xml.xpath.evaluate("upnp:friendlyName", xml.node);
            //serial = xml.xpath.evaluate("upnp:serialNumber", xml.node);
            String iconPath = "/" + xml.xpath.evaluate(
                    "upnp:iconList/upnp:icon[1]/upnp:url",
                    xml.node
            );
            try (Http png = getHttp(iconPath)) {
                icon = png.bitmap();
            }
        }
    }

    @Nullable
    public HueBridge pair() throws NotPressedException, IOException, JSONException, Http.HttpException, Http.ApplicationLayerException {
        try (Http http = getHttp("/api/")) {
            JSONObject json = http.json("{\"devicetype\": \"android#is.zi.huewidgets\"}");
            username = json.getJSONObject("success").getString("username");
            cert = http.cert;
            return this;
        } catch (Http.ApplicationLayerException e) {
            if (e.type == Http.ApplicationLayerException.NOT_PRESSED) {
                throw new NotPressedException(e);
            } else {
                throw e;
            }
        }
    }

    @NonNull
    public HueLight[] getLights()
            throws Http.ApplicationLayerException, Http.HttpException, JSONException, IOException {
        ArrayList<HueLight> lights = new ArrayList<>();
        try (Http http = getHttp("/api/" + username)) {
            JSONObject json = http.json();
            if (json.has("groups")) {
                JSONObject lightArray = json.getJSONObject("groups");
                for (int i = 1; ; i++) {
                    if (!lightArray.has(Integer.toString(i))) {
                        break;
                    }
                    HueLight light = new HueLight("/groups/" + i, lightArray.getJSONObject(Integer.toString(i)));
                    lights.add(light);
                }
            }
            if (json.has("lights")) {
                JSONObject lightArray = json.getJSONObject("lights");
                for (int i = 1; ; i++) {
                    if (!lightArray.has(Integer.toString(i))) {
                        break;
                    }
                    HueLight light = new HueLight("/lights/" + i, lightArray.getJSONObject(Integer.toString(i)));
                    lights.add(light);
                }
            }
            return lights.toArray(new HueLight[]{});
        }
    }

    @NonNull
    public HueColor getColor(@NonNull HueLight light)
            throws Http.ApplicationLayerException, Http.HttpException, JSONException, IOException {
        try (Http http = getHttp("/api/" + username + light.getPath())) {
            JSONObject json = http.json();
            if (json.has("action")) {
                return new HueColor(http.json().getJSONObject("action"));
            } else {
                return new HueColor(http.json().getJSONObject("state"));
            }
        }
    }

    public void setColor(@NonNull HueLight light, @NonNull HueColor oldColor, @NonNull HueColor color)
            throws Http.ApplicationLayerException, Http.HttpException, JSONException, IOException {
        String path = "/api/" + username + light.getStatePath();
        try (Http http = getHttp(path)) {
            http.json(color.toString(oldColor));
        }
    }

    @Nullable
    public Bitmap getIcon() {
        return icon;
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public String getUsername() {
        return username;
    }

    @NonNull
    public URL getUrl() {
        return url;
    }

    @Nullable
    public X509Certificate getCert() {
        return cert;
    }

    public void setOn(@NonNull HueLight light, boolean on)
            throws Http.ApplicationLayerException, Http.HttpException, JSONException, IOException {
        String path = "/api/" + username + light.getStatePath();
        try (Http http = getHttp(path)) {
            http.json(on ? "{\"transitiontime\": 0, \"on\": true}" : "{\"transitiontime\": 0, \"on\": false}");
        }
    }

    @Override
    @SuppressWarnings("unused") // lint is wrong
    public boolean equals(@Nullable Object other) {
        return other instanceof HueBridge && url.sameFile(((HueBridge) other).url);
    }

    @Override
    @SuppressWarnings("unused") // lint is wrong
    @NonNull
    public String toString() {
        if (name != null) {
            return name;
        } else {
            return url.toString();
        }
    }

    public static class Factory {
        @NonNull
        public HueBridge findNew(@NonNull List<HueBridge> ignore) throws NoWifiException, NotFoundException, IOException, XPathExpressionException, Http.HttpException {
            try (HttpMU upnp = new HttpMU("239.255.255.250:1900")) {
                Log.d("Bridge", "uPnP M-SEARCH to 239.255.255.250:1900");
                upnp.search(new String[]{"MAN: ssdp:discover", "MX: 10", "ST: ssdp:all"});
                while (true) {
                    HttpMU.Response reply = upnp.read();
                    Log.d("Bridge", "uPnP " + reply.source + " replied: " + reply.message);
                    if(reply.message.contains(" IpBridge/")) {
                        HueBridge newBridge = new HueBridge("https://" + reply.source);
                        if (!ignore.contains(newBridge)) {
                            Log.d("Bridge", "uPnP found new bridge " + reply.source);
                            newBridge.load();
                            return newBridge;
                        }
                    } else {
                            Log.d("Bridge", "uPnP non-bridge " + reply.source);
                    }
                }
            } catch (java.net.SocketTimeoutException e) {
                throw new NotFoundException(e);
            } catch (IOException e) {
                if (
                        Objects.requireNonNull(e.getMessage()).equals("Network is unreachable")
                                || e.getCause() instanceof android.system.ErrnoException
                ) {
                    throw new NoWifiException(e);
                }
                throw e;
            }
        }

        @Nullable
        public HueBridge create(@NonNull String url, @NonNull String username, @Nullable X509Certificate cert) throws MalformedURLException {
            return new HueBridge(url, username, cert) {
                @Override
                public HueBridge pair() {
                    return this;
                }
            };
        }
    }

    static class NotPressedException extends Exception {
        private static final long serialVersionUID = -5143082573472113188L;

        public NotPressedException(Throwable e) {
            super(e);
        }
    }

    static class NoWifiException extends Exception {
        private static final long serialVersionUID = -2579608764059181380L;

        NoWifiException(Throwable e) {
            super(e);
        }
    }

    static class NotFoundException extends Exception {
        private static final long serialVersionUID = 130094177142841709L;

        NotFoundException(Throwable e) {
            super(e);
        }
    }
}
