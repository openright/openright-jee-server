package net.openright.server.plugin.auth.saml;

import java.io.IOException;
import java.security.Principal;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.openright.jee.server.plugin.jetty.security.AbstractCoreLoginAuthenticator;
import net.openright.jee.server.plugin.jetty.security.CoreLoginConfig;

import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.authentication.DeferredAuthentication;
import org.eclipse.jetty.security.authentication.SessionAuthentication;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.UserIdentity;

class SamlLoginAuthenticator extends AbstractCoreLoginAuthenticator {

    private static final String AUTH_METHOD = "SAML";

    private final SamlConfiguration config;

    protected SamlLoginAuthenticator(SamlConfiguration config) {
        this.config = config;
    }

    protected Principal createPrincipal(SamlHttpAuthorizer auth) {
        return new SamlUserPrincipal(auth);
    }

    @Override
    public String getAuthMethod() {
        return AUTH_METHOD;
    }

    @Override
    protected Authentication tryAuthenticateUser(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        SamlHttpAuthorizer auth = createAuthorizer();

        if (!auth.validateRequest(request, response)) {
            return Authentication.SEND_FAILURE;
        } else {
            String userId = auth.getRemoteUserId();
            UserIdentity login = login(userId, null, request);

            if (login == null) {
                login = createUnregisteredAuthenticatedUserIdentity(createPrincipal(auth));
                return handleUnregisteredAuthenticatedUser(request, login);
            } else {
                HttpSession session = request.getSession(false);
                return (Authentication) session.getAttribute(SessionAuthentication.__J_AUTHENTICATED);
            }
        }
    }

    private SamlHttpAuthorizer createAuthorizer() {
        return new SamlHttpAuthorizer(config);
    }

    @Override
    public Authentication validateRequest(
            ServletRequest req
            , ServletResponse resp
            , boolean mandatory) throws ServerAuthException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        try {
            // google returns parameter error if not happy with authorization
            if (req.getParameter("error") != null) {
                resp.getWriter().println(req.getParameter("error"));
                return Authentication.SEND_FAILURE;
            }

            if (!mandatory) {
                return new DeferredAuthentication(this);
            }

            Authentication authentication = getAuthenticationFromSession(request);

            if (authentication == null) {
                return authenticateUser(request, response);
            } else {
                return verifyAuthenticatedUser(request, response, authentication);
            }

        } catch (ServletException | IOException e) {
            throw new ServerAuthException(e);
        }

    }

    @Override
    protected CoreLoginConfig getConfig() {
        return config;
    }

    @Override
    public boolean accept(HttpServletRequest req) {
        return req.getHeader(config.getSamlReferenceHttpHeaderName()) != null;
    }
}
