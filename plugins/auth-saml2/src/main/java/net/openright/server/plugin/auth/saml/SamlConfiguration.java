package net.openright.server.plugin.auth.saml;

import net.openright.jee.server.plugin.jetty.security.CoreLoginConfig;

public class SamlConfiguration extends CoreLoginConfig {

    private String assertionServer;
    private String logoutUrl;
    private String ssoLogoutUrl;
    private final String samlIdHeaderName;
    private final String userIdAttributeName;

    /**
     * @param samlIdHeaderName
     *            - name of http header which contains id of or token for authenticated user, and which can be used to
     *            get the SAML Assertion.
     * @param userIdAttributeName
     *            - name of attribute in SAML assertion which contains remote user id.
     */
    public SamlConfiguration(String samlIdHeaderName, String userIdAttributeName) {
        this.samlIdHeaderName = samlIdHeaderName;
        this.userIdAttributeName = userIdAttributeName;
    }

    public String getAssertionServer() {
        return assertionServer;
    }

    public void setAssertionServer(String assertionServer) {
        this.assertionServer = assertionServer;
    }

    public String getLogoutUrl() {
        return logoutUrl;
    }

    public void setLogoutUrl(String logoutUrl) {
        this.logoutUrl = logoutUrl;
    }

    public String getSsoLogoutUrl() {
        return ssoLogoutUrl;
    }

    public void setSsoLogoutUrl(String ssoLogoutUrl) {
        this.ssoLogoutUrl = ssoLogoutUrl;
    }

    /** header which contains reference which can be used to lookup SAML assertion. */
    public String getSamlReferenceHttpHeaderName() {
        return this.samlIdHeaderName;
    }

    /** Field in SAML assertion which contains user id. */
    public String getSamlRemoteUserIdAttributeName() {
        return userIdAttributeName;
    }

}