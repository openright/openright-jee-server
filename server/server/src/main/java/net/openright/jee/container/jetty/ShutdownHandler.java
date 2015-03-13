package net.openright.jee.container.jetty;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShutdownHandler extends AbstractHandler {

    private static Logger logger = LoggerFactory.getLogger(ShutdownHandler.class);

    private final String shutdownToken;

    public ShutdownHandler(String shutdownToken) {
        this.shutdownToken = shutdownToken;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        if (!"/shutdown".equals(target)) {
            return;
        }

        if (!"POST".equals(request.getMethod())) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        if (!hasCorrectSecurityToken(request)) {
            logger.warn("Unauthorized shutdown attempt from " + getRemoteAddr(request));
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        if (!requestFromLocalhost(request)) {
            logger.warn("Unauthorized shutdown attempt from " + getRemoteAddr(request));
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        logger.info("Shutting down by request from " + getRemoteAddr(request));
        try {
            shutdownServer();
        } catch (Exception e) {
            throw new IllegalStateException("Shutting down server", e);
        }
    }

    private boolean requestFromLocalhost(HttpServletRequest request) {
        return "127.0.0.1".equals(getRemoteAddr(request));
    }

    public String getRemoteAddr(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private boolean hasCorrectSecurityToken(HttpServletRequest request) {
        return shutdownToken.equals(request.getParameter("token"));
    }

    public void shutdownServer() throws Exception {// NOSONAR
        System.exit(0);// NOSONAR
    }

    public static void attemptShutdownFromClient(int port, String shutdownSecurityToken) {
        try {
            URL url = new URL("http://localhost:" + port + "/shutdown?token=" + shutdownSecurityToken);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.getResponseCode();
            logger.info("Shutting down " + url + ": " + connection.getResponseMessage());
            Thread.sleep(1000L); // delay to ensure process releases ports
        } catch (SocketException e) {
            logger.debug("Not running");
            // Okay - the server is not running
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Interrupted while trying to shutdown existing instance", e);
        }
    }

}
