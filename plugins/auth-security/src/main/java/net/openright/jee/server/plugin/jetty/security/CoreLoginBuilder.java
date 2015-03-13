package net.openright.jee.server.plugin.jetty.security;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.servlet.http.HttpServlet;

import net.openright.jee.server.plugin.jetty.security.AbstractCoreLoginAuthenticator.RemoveFromLoginServiceOnHttpSessionClose;

import org.apache.http.client.HttpClient;
import org.eclipse.jetty.security.authentication.LoginAuthenticator;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;

@SuppressWarnings("unchecked")
public abstract class CoreLoginBuilder<T extends CoreLoginBuilder<T>> {
    private static final Executor executor = Executors.newSingleThreadExecutor(); // NOSONAR

    private String logoutUrl;
    private CoreLoginConfig config;

    private boolean initialized;

    public CoreLoginBuilder() {
        super();
    }

    public void initializeFor(WebAppContext webAppContext) throws Exception {

        verifyNotInitialized();
        
        initLoginBuilder();

        LoginAuthenticator authenticator = getLoginAuthenticator();

        webAppContext.getSecurityHandler().setAuthenticator(authenticator);
        webAppContext.getServletContext().addListener(new RemoveFromLoginServiceOnHttpSessionClose(authenticator));

        webAppContext.addServlet(new ServletHolder(getLogoutServlet()), logoutUrl);
    }

    protected void verifyNotInitialized() {
        if (initialized) {
            throw new IllegalStateException("Already initialized");
        } else {
            initialized = true;
        }
    }

    protected HttpServlet getLogoutServlet() {
        return new CoreLogoutServlet(null, getLogoutHandlers());
    }

    protected abstract List<CoreLogoutHandler> getLogoutHandlers();

    protected abstract CoreLoginAuthenticator getLoginAuthenticator();

    protected abstract void initLoginBuilder() throws Exception;

    public T withHttpClient(HttpClient httpClient) {
        this.getConfig().setHttpUtil(new HttpUtil(httpClient));
        return (T) this;
    }

    public T withLandingUrl(String url) {
        this.getConfig().setLandingUrl(url);
        return (T) this;
    }

    public T withLogoutUrl(String url) {
        this.logoutUrl = url;
        return (T) this;
    }

    public T withRegistrationUrl(String url) {
        this.getConfig().setRegistrationUrl(url);
        return (T) this;
    }

    public T withAuthenticationEventListener(AuthenticationEventListener listener) {
        class AsyncAuthenticationEventListener implements AuthenticationEventListener {
            private AuthenticationEventListener eventListener;

            public AsyncAuthenticationEventListener(AuthenticationEventListener listener) {
                this.eventListener = listener;
            }

            @Override
            public void onLogin(UserPrincipalInfo userPrincipal, AuthenticationEvent evt) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        eventListener.onLogin(userPrincipal, evt);
                    }
                });
            }

            @Override
            public void onLogout(UserPrincipalInfo userPrincipal, AuthenticationEvent evt) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        eventListener.onLogout(userPrincipal, evt);
                    }
                });
            }

        }
        this.getConfig().setEventListener(new AsyncAuthenticationEventListener(listener));
        return (T) this;
    }

    public CoreLoginConfig getConfig() {
        return config;
    }

    public void setConfig(CoreLoginConfig config) {
        this.config = config;
    }

}