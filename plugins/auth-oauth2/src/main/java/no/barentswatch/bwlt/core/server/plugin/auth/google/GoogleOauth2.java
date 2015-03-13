package no.barentswatch.bwlt.core.server.plugin.auth.google;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @see https://developers.google.com/accounts/docs/OAuth2Login
 */
class GoogleOauth2 {

    static final String ACCESS_TOKEN = "access_token";

    /**
     * @see https://developers.google.com/accounts/docs/OAuth2Login#sendauthrequest (anti-forgery state token)
     */
    private static final String GOOGLE_AUTH_ANTI_FORGERY_STATE_TOKEN = "google.auth.antiforgery.token";
    private static final String GOOGLE_AUTH_ANTI_FORGERY_STATE_PARAM = "state";
    static final String GOOGLE_AUTH_ACCESS_TOKEN = "google.auth.access.token";

    private static final Logger log = LoggerFactory.getLogger(GoogleOauth2.class);

    private JsonObject userJson;
    private String accessToken;
    private boolean authContinue;

    private Oauth2LoginConfig config;

    protected GoogleOauth2(Oauth2LoginConfig config) {
        this.config = config;
    }

    protected boolean validateRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        authContinue = false;

        if (req.getParameter("error") != null) {
            resp.getWriter().println(req.getParameter("error"));
            return false;
        }

        String code = req.getParameter("code");

        if (code == null) {
            redirectUserToGoogleAuthenticationPage(req, resp);
            authContinue = true;
            return false;
        } else {
            authContinue = verifyUserIsAuthorized(req, resp, code);
            return authContinue;
        }
    }

    public boolean isAuthContinue() {
        return authContinue;
    }

    protected boolean verifyUserIsAuthorized(HttpServletRequest req, HttpServletResponse resp, String code) throws IOException {
        try {
            if (!verifyAntiForgeryToken(req, resp)) {
                return false;
            }

            String redirectUrl = getRedirectUri(req);
            this.accessToken = getAccessTokenFromGoogle(code, redirectUrl);

            req.getSession().setAttribute(GOOGLE_AUTH_ACCESS_TOKEN, this.accessToken);

            String userInfo = getUserInfo(accessToken);

            JsonReader jsonReader = Json.createReader(new StringReader(userInfo));
            this.userJson = jsonReader.readObject();
            return true;
        } catch (IOException | URISyntaxException e) {
            // Failed to login to google, just log error
            log.warn("Failed to log in for user [code=" + code + "]", e);
            // don' send back stacktrace, just generic error
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unable to login");
            return false;
        }
    }

    protected boolean verifyAntiForgeryToken(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String expectedAntiForgeryToken = (String) req.getSession(false).getAttribute(GOOGLE_AUTH_ANTI_FORGERY_STATE_TOKEN);
        req.getSession(false).removeAttribute(GOOGLE_AUTH_ANTI_FORGERY_STATE_TOKEN);

        String actualToken = req.getParameter(GOOGLE_AUTH_ANTI_FORGERY_STATE_PARAM);

        if (expectedAntiForgeryToken == null || !expectedAntiForgeryToken.equals(actualToken)) {
            log.warn("Invalid request - antiforgery token mismatch: actual token from request [" + actualToken
                    + "], did not match expected token");
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unable to login");
            return false;
        }
        return true;
    }

    public JsonObject getRemoteUserObject() {
        return userJson;
    }

    public String getRemoteAccessToken() {
        return accessToken;
    }

    public String getRemoteUserId() {
        return userJson == null ? null : userJson.getString("sub");
    }

    public String getRemoteUserEmail() {
        return userJson == null ? null : userJson.getString("email");
    }

    public boolean isRemoteUserEmailVerified() {
        return userJson == null ? false : userJson.getBoolean("verified_email");
    }

    public String getRemoteUserGivenName() {
        return userJson == null ? null : userJson.getString("given_name");
    }

    public String getRemoteUserFamilyName() {
        return userJson == null ? null : userJson.getString("family_name");
    }

    protected String getAccessTokenFromGoogle(String code, String redirectUri) throws IOException {
        // get the access token by post to Google
        Map<String, String> params = new LinkedHashMap<>();
        params.put("code", code);
        params.put("client_id", config.getClientId());
        params.put("client_secret", config.getClientSecret());
        params.put("redirect_uri", redirectUri);
        params.put("grant_type", "authorization_code");

        String body = post(config.getOpenId().getTokenEndpoint(), params);

        JsonReader jsonReader = Json.createReader(new StringReader(body));
        String accessToken = jsonReader.readObject().getString(ACCESS_TOKEN);
        return accessToken;
    }

    protected void redirectUserToGoogleAuthenticationPage(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String comebackUrl = getRedirectUri(req);
        String antiForgeryToken = createAntiForgeryToken();
        req.getSession().setAttribute(GOOGLE_AUTH_ANTI_FORGERY_STATE_TOKEN, antiForgeryToken);

        StringBuilder oauthUrl = new StringBuilder(config.getOpenId().getAuthorizationEndpoint())
                .append("?client_id=").append(config.getClientId()) /*
                                                                * the client id from the api console registration
                                                                */
                .append("&response_type=code")
                .append("&scope=openid%20email") /* scope is the api permissions we are requesting */
                .append("&redirect_uri=" + comebackUrl) /*
                                                         * the servlet that google redirects to after
                                                         * authorization
                                                         */
                .append("&state=" + antiForgeryToken)
                .append("&access_type=online") /*
                                                * offline = asking to access to user's data
                                                * while they are not signed in
                                                * online = online when they can log in themselves
                                                */
                .append("&approval_prompt=auto"); /*
                                                   * this requires them to verify which account to use,
                                                   * if they are already signed in
                                                   */

        resp.sendRedirect(oauthUrl.toString());
    }

    private String createAntiForgeryToken() {
        byte[] bytes = new byte[32];
        config.getSecureRandom().nextBytes(bytes);
        String antiForgeryToken = Base64.getUrlEncoder().encodeToString(bytes);
        return antiForgeryToken;
    }

    protected String getUserInfo(String accessToken) throws IOException, URISyntaxException {
        URI uri = new URIBuilder(config.getOpenId().getUserInfoEndpoint())
                .setParameter(ACCESS_TOKEN, accessToken)
                .build();
        return execute(new HttpGet(uri));
    }

    protected String post(String url, Map<String, String> formParameters) throws IOException {
        HttpPost request = new HttpPost(url);

        List<NameValuePair> nvps = new ArrayList<NameValuePair>();

        for (String key : formParameters.keySet()) {
            nvps.add(new BasicNameValuePair(key, formParameters.get(key)));
        }
        request.setEntity(new UrlEncodedFormEntity(nvps));

        return execute(request);
    }

    // makes request and checks response code for 200
    protected String execute(HttpRequestBase request) throws IOException {
        return config.getHttpUtil().execute(request);
    }

    protected String getRedirectUri(HttpServletRequest request) {
        String port = ("http".equals(request.getScheme()) && request.getServerPort() == 80)
                || ("https".equals(request.getScheme()) && request.getServerPort() == 443)
                ? ""
                : ":" + request.getServerPort();

        // redirects to root of webapp (context path)
        String uri = request.getScheme()
                + "://"
                + request.getServerName()
                + port
                + request.getContextPath() + "/";

        return uri;
    }

}
