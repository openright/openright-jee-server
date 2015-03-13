package net.openright.jee.server.plugin.jetty.security;

import java.util.List;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Authentication.User;

public class CombinedLoginAuthenticator extends CoreLoginAuthenticator {

    private final List<CoreLoginAuthenticator> loginAuthenticators;

    public CombinedLoginAuthenticator(List<CoreLoginAuthenticator> authenticators) {
        this.loginAuthenticators = authenticators;
    }

    @Override
    public String getAuthMethod() {
        return "Combined";
    }

    @Override
    public Authentication validateRequest(ServletRequest request, ServletResponse response, boolean mandatory) throws ServerAuthException {
        HttpServletRequest req = (HttpServletRequest) request;
        for (CoreLoginAuthenticator a : loginAuthenticators) {
            if (a.accept(req)) {
                return a.validateRequest(request, response, mandatory);
            }
        }
        throw new IllegalArgumentException("No authenticator present");
    }

    @Override
    public boolean secureResponse(ServletRequest request, ServletResponse response, boolean mandatory, User validatedUser)
            throws ServerAuthException {
        return true;
    }

    @Override
    public boolean accept(HttpServletRequest req) {
        return true;
    }

    @Override
    public void setConfiguration(AuthConfiguration configuration) {
        super.setConfiguration(configuration);
        for (CoreLoginAuthenticator authenticator : loginAuthenticators) {
            authenticator.setConfiguration(configuration);
        }
    }
}
