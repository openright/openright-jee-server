package no.barentswatch.bwlt.core.server.plugin.auth.google;

import java.io.IOException;
import java.security.Principal;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.openright.jee.server.plugin.jetty.security.AbstractCoreLoginAuthenticator;
import net.openright.jee.server.plugin.jetty.security.CoreLoginBuilder;
import net.openright.jee.server.plugin.jetty.security.CoreLoginConfig;

import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.authentication.DeferredAuthentication;
import org.eclipse.jetty.security.authentication.SessionAuthentication;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.UserIdentity;

class GoogleOauth2LoginAuthenticator extends AbstractCoreLoginAuthenticator {

    private static final String AUTH_METHOD = "GOOGLEOAUTH2";

    public static CoreLoginBuilder<Oauth2LoginBuilder> create() {
        return new Oauth2LoginBuilder();
    }

    private final Oauth2LoginConfig config;

    protected GoogleOauth2LoginAuthenticator(Oauth2LoginConfig config) {
        this.config = config;
    }

    protected Principal createPrincipal(GoogleOauth2 auth) {
        return new GoogleOauth2UserPrincipal(auth);
    }

    @Override
    public String getAuthMethod() {
        return AUTH_METHOD;
    }

    @Override
    protected Authentication tryAuthenticateUser(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        GoogleOauth2 googleAuth = createAuthorizer();

        if (DeferredAuthentication.isDeferred(response)) {
            // must be able to send redirect, otherwise just fail auth
            return Authentication.UNAUTHENTICATED;
        }

        if (!googleAuth.validateRequest(request, response)) {
            if (!googleAuth.isAuthContinue()) {
                return Authentication.NOT_CHECKED;
            } else {
                return Authentication.SEND_CONTINUE;
            }
        } else {
            String userId = googleAuth.getRemoteUserId();
            UserIdentity login = login(userId, null, request);

            if (login == null) {
                login = createUnregisteredAuthenticatedUserIdentity(createPrincipal(googleAuth));
                return handleUnregisteredAuthenticatedUser(request, login);
            } else {
                HttpSession session = request.getSession(false);
                return (Authentication) session.getAttribute(SessionAuthentication.__J_AUTHENTICATED);
            }
        }
    }

    private GoogleOauth2 createAuthorizer() {
        return new GoogleOauth2(config);
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
        return true;
    }

}
