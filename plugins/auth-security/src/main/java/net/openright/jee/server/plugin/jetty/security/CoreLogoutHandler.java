package net.openright.jee.server.plugin.jetty.security;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.ClientProtocolException;

public abstract class CoreLogoutHandler {

    protected abstract boolean remoteLogout(HttpServletRequest req, HttpServletResponse resp) throws ClientProtocolException, IOException;

    public abstract CoreLoginConfig getConfig();

}
