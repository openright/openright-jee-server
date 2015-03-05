package net.openright.jee.container.jetty;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import net.openright.jee.LogSniffer;
import net.openright.jee.container.jetty.ShutdownHandler;

public class ShutdownHandlerTestIT extends AbstractJettyTestIT {

    @Rule
    public final LogSniffer sniffer = new LogSniffer();

    private String shutdownToken = "shutdownNow!bwltasdf";
    private int serverPort;
    private ShutdownHandler shutdownHandler;

    @Before
    public void startServer() throws Exception {
        shutdownHandler = Mockito.spy(new ShutdownHandler(shutdownToken));
        doNothing().when(shutdownHandler).shutdownServer();

        Server server = new Server(0);
        server.setHandler(shutdownHandler);
        server.start();
        serverPort = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
    }

    @Test
    public void should_shutdown_server_when_token_is_correct() throws Exception {
        ShutdownHandler.attemptShutdownFromClient(serverPort, shutdownToken);
        verify(shutdownHandler).shutdownServer();
    }

    @Test
    public void should_not_shutdown_server_when_token_is_wrong() throws Exception {
        ShutdownHandler.attemptShutdownFromClient(serverPort, "abc");
        verify(shutdownHandler, never()).shutdownServer();
        sniffer.assertHarWarnMelding("Unauthorized shutdown attempt");
    }

    @Test
    public void should_not_shutdown_server_when_not_loopback() throws Exception {
        String linkLocalAddress = "169.254.1.1";
        doReturn(linkLocalAddress).when(shutdownHandler).getRemoteAddr(Matchers.any(HttpServletRequest.class));

        ShutdownHandler.attemptShutdownFromClient(serverPort, shutdownToken);
        verify(shutdownHandler, never()).shutdownServer();

        sniffer.assertHarWarnMelding("Unauthorized shutdown attempt");
    }

}
