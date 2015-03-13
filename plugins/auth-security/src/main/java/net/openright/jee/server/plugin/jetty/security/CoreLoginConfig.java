package net.openright.jee.server.plugin.jetty.security;


public class CoreLoginConfig {

    /** Url to go to after successful login. */
    private String landingUrl;
    /** Url to go to if user not fully registered (but can be authenticated) */
    private String registrationUrl;
    private HttpUtil httpUtil;
    private AuthenticationEventListener eventListener = new NoopAuthenticationEventListener();

    public CoreLoginConfig() {
        super();
    }

    public String getLandingUrl() {
        return landingUrl;
    }

    public void setLandingUrl(String landingUrl) {
        this.landingUrl = landingUrl;
    }

    public String getRegistrationUrl() {
        return registrationUrl;
    }

    public void setRegistrationUrl(String registrationUrl) {
        this.registrationUrl = registrationUrl;
    }

    public HttpUtil getHttpUtil() {
        return httpUtil;
    }

    public void setHttpUtil(HttpUtil httpUtil) {
        this.httpUtil = httpUtil;
    }

    public AuthenticationEventListener getEventListener() {
        return eventListener;
    }

    public void setEventListener(AuthenticationEventListener eventListener) {
        this.eventListener = eventListener;
    }
}