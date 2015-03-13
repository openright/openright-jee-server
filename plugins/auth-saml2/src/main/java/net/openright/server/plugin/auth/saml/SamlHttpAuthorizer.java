package net.openright.server.plugin.auth.saml;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.parse.XMLParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SamlHttpAuthorizer {

    private static final Logger log = LoggerFactory.getLogger(SamlHttpAuthorizer.class);

    private SamlConfiguration config;
    private String remoteUserId;
    private final String assertionHeaderName;

    protected SamlHttpAuthorizer(SamlConfiguration config) {
        this.config = config;
        this.assertionHeaderName = config.getSamlReferenceHttpHeaderName();
    }

    protected boolean validateRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String header = req.getHeader(assertionHeaderName);
        if (header == null) {
            return false;
        }
        if (header.startsWith(config.getAssertionServer())) {
            return verifyUserIsAuthorized(req, resp);
        }
        log.warn("Header (" + assertionHeaderName + ") doesn't point to configured assertion server (" + config.getAssertionServer() + ")");
        return false;
    }

    protected boolean verifyUserIsAuthorized(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String assertionString = getUserInfo(req.getHeader(assertionHeaderName));
            Assertion assertion = SamlHelper.parseAssertion(assertionString);
            remoteUserId = SamlHelper.getAttributeString(config.getSamlRemoteUserIdAttributeName(),assertion);
            return true;
        } catch (IOException | URISyntaxException | XMLParserException | UnmarshallingException e) {
            // Failed to login, just log error
            log.warn("Failed to log in for user", e);
            // don' send back stacktrace, just generic error
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unable to login");
            return false;
        }
    }

    public String getRemoteUserId() {
        return remoteUserId;
    }

    protected String getUserInfo(String samlAssertionURI) throws IOException, URISyntaxException {
        URI uri = new URIBuilder(samlAssertionURI).build();
        return config.getHttpUtil().execute(new HttpGet(uri));
    }
}
