package no.barentswatch.bwlt.core.server.plugin.auth.google;

import java.security.SecureRandom;

import net.openright.jee.server.plugin.jetty.security.CoreLoginConfig;

class Oauth2LoginConfig extends CoreLoginConfig {

    private OpenIdDiscoveryDocument openId;
    private String clientId;
    private String clientSecret;

    private final SecureRandom secureRandom;

    public Oauth2LoginConfig() {
        this.secureRandom = new SecureRandom();
    }

    public SecureRandom getSecureRandom() {
        return secureRandom;
    }

    public OpenIdDiscoveryDocument getOpenId() {
        return openId;
    }

    public void setOpenId(OpenIdDiscoveryDocument openId) {
        this.openId = openId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

}