package net.openright.server.plugin.auth.saml;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.openright.jee.server.plugin.jetty.security.CoreLoginConfig;
import net.openright.jee.server.plugin.jetty.security.CoreLogoutHandler;

import org.apache.http.client.ClientProtocolException;

public class SamlLogoutHandler extends CoreLogoutHandler {

    private SamlConfiguration config;

    public SamlLogoutHandler(SamlConfiguration config) {
        this.config = config;
    }

    @Override
    protected boolean remoteLogout(HttpServletRequest req, HttpServletResponse resp) throws ClientProtocolException, IOException {
        if (req.getHeader(config.getSamlReferenceHttpHeaderName()) != null) {
            resp.sendRedirect(resp.encodeRedirectURL(config.getSsoLogoutUrl()));
            return true;
        } else {
            return false;
        }
    }

    @Override
    public CoreLoginConfig getConfig() {
        return config;
    }
}
