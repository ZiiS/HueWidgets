// Copyright © 2019 Barnaby Shearer <b@Zi.iS>

package is.zi.comm;

import android.text.TextUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URL;

import is.zi.NonNull;
import is.zi.Nullable;

public class HttpMU implements AutoCloseable {
    @NonNull
    private final String hostAndPort;
    @Nullable
    private DatagramSocket conn;

    public HttpMU(@NonNull String hostAndPort) {
        this.hostAndPort = hostAndPort;
    }

    private byte[] getRequest(@SuppressWarnings("SameParameterValue") String method, @NonNull String[] headers, @Nullable @SuppressWarnings("SameParameterValue") String body) {
        return String.format(
                "M-%s * HTTP/1.1\r\nHOST: %s\r\n%s\r\n\r\n%s",
                method,
                hostAndPort,
                TextUtils.join("\r\n", headers),
                body
        ).getBytes();
    }

    @NonNull
    public DatagramSocket getSocket() throws SocketException {
        return new DatagramSocket();
    }

    public void search(@NonNull String[] headers) throws IOException {
        URL url = new URL("http://" + hostAndPort);
        InetAddress IPAddress = InetAddress.getByName(url.getHost());
        byte[] sendData = getRequest("SEARCH", headers, "");
        conn = getSocket();
        conn.setSoTimeout(5000);
        conn.send(new DatagramPacket(sendData, sendData.length, IPAddress, url.getPort()));
    }

    @NonNull
    public Response read() throws IOException {
        if (conn == null) {
            throw new IOException("Connection Closed");
        }
        byte[] receiveData = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        conn.receive(receivePacket);
        return new Response(
                receivePacket.getAddress().getHostAddress(),
                new String(receivePacket.getData())
        );
    }

    @Override
    public void close() {
        if (conn != null) {
            conn.close();
        }
    }

    public class Response {
        @NonNull
        public final String source;
        @NonNull
        public final String message;

        Response(@NonNull String source, @NonNull String message) {
            this.source = source;
            this.message = message;
        }
    }
}
