package is.zi.comm;


import org.junit.Test;

import java.io.IOException;
import java.net.DatagramSocket;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class HttpMUTest {

    @Test
    public void search() throws IOException {
        try (HttpMU cut = spy(new HttpMU("239.255.255.250:1900"))) {
            DatagramSocket socket = mock(DatagramSocket.class);
            doReturn(socket).when(cut).getSocket();
            cut.search(new String[]{"MAN: ssdp:discover", "MX: 10", "ST: ssdp:all"});
        }
    }

}
