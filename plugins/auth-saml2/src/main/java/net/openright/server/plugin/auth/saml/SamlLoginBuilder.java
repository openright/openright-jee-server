package net.openright.server.plugin.auth.saml;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import net.openright.jee.server.plugin.jetty.security.CoreLoginAuthenticator;
import net.openright.jee.server.plugin.jetty.security.CoreLoginBuilder;
import net.openright.jee.server.plugin.jetty.security.CoreLogoutHandler;

import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.parse.XMLParserException;

public class SamlLoginBuilder extends CoreLoginBuilder<SamlLoginBuilder> {

    private final SamlConfiguration config;

    public SamlLoginBuilder(SamlConfiguration config) {
        this.config = config;
    }

    @Override
    public SamlConfiguration getConfig() {
        return config;
    }

    @Override
    protected void initLoginBuilder() throws ConfigurationException, XMLParserException {
        SamlHelper.init();
    }

    @Override
    protected CoreLoginAuthenticator getLoginAuthenticator() {
        return new SamlLoginAuthenticator(getConfig());
    }

    @Override
    protected List<CoreLogoutHandler> getLogoutHandlers() {
        return Arrays.asList(new SamlLogoutHandler(getConfig()));
    }

    public SamlLoginBuilder withAssertionServer(String assertionServer) {
        getConfig().setAssertionServer(assertionServer);
        return this;
    }

    public SamlLoginBuilder withSsoLogoutUrl(String ssoLogoutUrl) throws URISyntaxException {
        new URI(ssoLogoutUrl);
        getConfig().setSsoLogoutUrl(ssoLogoutUrl);
        return this;
    }

}