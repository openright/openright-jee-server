package net.openright.jee.server.plugin.jetty.security;

import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.http.client.HttpClient;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.security.authentication.DeferredAuthentication;
import org.eclipse.jetty.security.authentication.LoginAuthenticator;
import org.eclipse.jetty.security.authentication.SessionAuthentication;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Authentication.User;
import org.eclipse.jetty.server.UserIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractCoreLoginAuthenticator extends CoreLoginAuthenticator {

    private static final String UNREGISTERED_ROLE = "UNREGISTERED";
    private static final Logger log = LoggerFactory.getLogger(AbstractCoreLoginAuthenticator.class);

    protected UserIdentity createUnregisteredAuthenticatedUserIdentity(Principal principal) {
        Subject subject = new Subject(true, new HashSet<>(Arrays.asList(principal)), Collections.emptySet(),
                Collections.emptySet());

        String[] roles = new String[] { UNREGISTERED_ROLE };

        UserIdentity unregisteredUserIdentity = getLoginService().getIdentityService().newUserIdentity(subject, principal,
                roles);
        return unregisteredUserIdentity;
    }

    protected void handleOnLogin(UserIdentity userIdentity, AuthenticationEvent authenticationEvent) {
        Principal userPrincipal = userIdentity.getUserPrincipal();
        if (userPrincipal instanceof UserPrincipalInfo) {
            getConfig().getEventListener().onLogin((UserPrincipalInfo) userPrincipal, authenticationEvent);
        }
    }

    protected boolean dispatchToLandingUrl(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(request.getContextPath() + getConfig().getLandingUrl());
        return true;
    }

    protected boolean dispatchToRegistrationUrl(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        if (DeferredAuthentication.isDeferred(response)) {
            // if not able to send redirect
            return false;
        }
        if (!request.getRequestURI().endsWith(getRegistrationUrlPath())) {
            response.sendRedirect(request.getContextPath() + getRegistrationUrlPath());
            return true;
        } else {
            return false;
        }
    }

    protected abstract CoreLoginConfig getConfig();

    public HttpClient getHttpClient() {
        return getConfig().getHttpUtil().getHttpClient();
    }

    protected String getRegistrationUrlPath() {
        return getConfig().getRegistrationUrl();
    }

    protected boolean dispatchResponseAfterAuthentication(HttpServletRequest request, HttpServletResponse response,
            Authentication.User authenticateUser) throws ServletException, IOException {
        if (!DeferredAuthentication.isDeferred(response)) {
            if (authenticateUser instanceof UnregisteredSessionAuthentication) {
                handleOnLogin(authenticateUser.getUserIdentity(), new AuthenticationEvent(request));
                return dispatchToRegistrationUrl(request, response);
            } else if (authenticateUser instanceof SessionAuthentication) {
                handleOnLogin(authenticateUser.getUserIdentity(), new AuthenticationEvent(request));
                return dispatchToLandingUrl(request, response);
            } else {
                // carry on
            }
        }
        return false;
    }

    protected Authentication getAuthenticationFromSession(HttpServletRequest request) {
        return (Authentication) request.getSession().getAttribute(SessionAuthentication.__J_AUTHENTICATED);
    }

    protected Authentication handleUnregisteredAuthenticatedUser(HttpServletRequest request, UserIdentity login) throws ServletException,
            IOException {
        UnregisteredSessionAuthentication unregAuth = new UnregisteredSessionAuthentication(getAuthMethod(), login);
        request.getSession(false).setAttribute(SessionAuthentication.__J_AUTHENTICATED, unregAuth);
        log.debug("user needs to register first {}", unregAuth);
        return unregAuth;
    }

    @Override
    public UserIdentity login(String username, Object password, ServletRequest request) {

        UserIdentity user = super.login(username, password, request);

        if (user != null) {
            HttpSession session = ((HttpServletRequest) request).getSession(true);
            Authentication cached = new SessionAuthentication(getAuthMethod(), user, password);
            setAuthenticationInSession(session, cached);
        }

        return user;

    }

    protected void logoutUser(HttpSession session, Authentication authentication) {
        log.debug("auth revoked {}", authentication);
        session.removeAttribute(SessionAuthentication.__J_AUTHENTICATED);
    }

    @Override
    public boolean secureResponse(ServletRequest request, ServletResponse response, boolean mandatory, User validatedUser)
            throws ServerAuthException {
        return true;
    }

    protected void setAuthenticationInSession(HttpSession session, Authentication cached) {
        session.setAttribute(SessionAuthentication.__J_AUTHENTICATED, cached);
    }

    protected Authentication authenticateUser(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException {
        Authentication auth = tryAuthenticateUser(request, response);
        if (auth instanceof Authentication.User) {
            Authentication.User authUser = (Authentication.User) auth;
            if (dispatchResponseAfterAuthentication(request, response, authUser)) {
                return new JustAuthenticated(authUser);
            }
        }
        return auth;
    }

    protected abstract Authentication tryAuthenticateUser(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException;

    protected Authentication
            verifyAuthenticatedUser(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
                    throws ServletException, IOException {
        if (authentication instanceof Authentication.User && getLoginService() != null) {

            Authentication.User authUser = (Authentication.User) authentication;
            UserIdentity userIdentity = authUser.getUserIdentity();

            if (!getLoginService().validate(userIdentity)) {
                if (authentication instanceof UnregisteredSessionAuthentication) {
                    if (dispatchToRegistrationUrl(request, response)) {
                        return new JustAuthenticated(authUser);
                    }
                } else {
                    getLoginService().logout(userIdentity);
                    HttpSession session = request.getSession(false);
                    logoutUser(session, authentication);
                    return Authentication.UNAUTHENTICATED;
                }
            } else {
                return authentication;
            }
        }
        // everything Ok
        return authentication;
    }

    protected static final class RemoveFromLoginServiceOnHttpSessionClose implements HttpSessionListener {
        private LoginAuthenticator authenticator;

        RemoveFromLoginServiceOnHttpSessionClose(LoginAuthenticator authenticator) {
            this.authenticator = authenticator;
        }

        @Override
        public void sessionCreated(HttpSessionEvent se) {
        }

        @Override
        public void sessionDestroyed(HttpSessionEvent se) {
            HttpSession session = se.getSession();
            SessionAuthentication auth = (SessionAuthentication) session.getAttribute(SessionAuthentication.__J_AUTHENTICATED);
            if (auth != null && authenticator.getLoginService() != null) {
                UserIdentity userIdentity = auth.getUserIdentity();
                authenticator.getLoginService().logout(userIdentity);
            }
        }
    }

    protected static class UnregisteredSessionAuthentication extends SessionAuthentication {

        public UnregisteredSessionAuthentication(String method, UserIdentity userIdentity) {
            super(method, userIdentity, null);
        }

    }

    /** Represents a user which has just been authenticated and redirected to appropriate landing or registration page. */
    protected static class JustAuthenticated extends UserAuthentication implements Authentication.ResponseSent {
        JustAuthenticated(String method, UserIdentity userIdentity) {
            super(method, userIdentity);
        }

        public JustAuthenticated(Authentication.User auth) {
            super(auth.getAuthMethod(), auth.getUserIdentity());
        }
    }

    @Override
    public abstract boolean accept(HttpServletRequest req);

}