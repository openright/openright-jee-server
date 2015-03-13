package no.barentswatch.bwlt.core.server.plugin.auth.google;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import net.openright.jee.server.plugin.jetty.security.CoreLoginAuthenticator;
import net.openright.jee.server.plugin.jetty.security.CoreLoginBuilder;
import net.openright.jee.server.plugin.jetty.security.CoreLogoutHandler;
import net.openright.jee.server.plugin.jetty.security.HttpUtil;

import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Oauth2LoginBuilder extends CoreLoginBuilder<Oauth2LoginBuilder> {
    private static final Logger log = LoggerFactory.getLogger(Oauth2LoginBuilder.class);

    private final Oauth2LoginConfig config = new Oauth2LoginConfig();
    /** Defaults to google's openid discovery endpoint. */

    private String discoveryDocumentUrl = "https://accounts.google.com/.well-known/openid-configuration";

    @Override
    public Oauth2LoginConfig getConfig() {
        return config;
    }

    @Override
    protected void initLoginBuilder() throws IOException, URISyntaxException {
        if (config.getOpenId() == null) {
            config.setOpenId(new OpenIdDiscovery(config.getHttpUtil()).getDiscoveryDocument(discoveryDocumentUrl));
        }
        log.info("Initialized Oauth2 OpenId Discovery Document: " + config.getOpenId());
    }

    @Override
    protected CoreLoginAuthenticator getLoginAuthenticator() {
        return new GoogleOauth2LoginAuthenticator(getConfig());
    }

    @Override
    protected List<CoreLogoutHandler> getLogoutHandlers() {
        return Arrays.asList(new Oauth2LogoutHandler(getConfig()));
    }

    public Oauth2LoginBuilder withDefaultHttpClient() {
        this.getConfig().setHttpUtil(new HttpUtil(HttpClientBuilder
                .create()
                .setUserAgent("")
                .setMaxConnPerRoute(Integer.valueOf(System.getProperty("auth.google.http.conn.max", "5")))
                .build()));
        return this;
    }

    public Oauth2LoginBuilder withDiscoveryDocumentUrl(String url) {
        this.discoveryDocumentUrl = url;
        return this;
    }

    public Oauth2LoginBuilder withDiscoveryDocument(OpenIdDiscoveryDocument doc) {
        this.config.setOpenId(doc);
        return this;
    }

    public Oauth2LoginBuilder withOauth2ClientId(String clientId) {
        this.config.setClientId(clientId);
        return this;
    }

    public Oauth2LoginBuilder withOauth2ClientSecret(String clientSecret) {
        this.config.setClientSecret(clientSecret);
        return this;
    }
}