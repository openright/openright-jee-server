package no.barentswatch.bwlt.core.server.plugin.auth.google;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.openright.jee.server.plugin.jetty.security.CoreLogoutHandler;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

public class Oauth2LogoutHandler extends CoreLogoutHandler {

    private Oauth2LoginConfig config;

    public Oauth2LogoutHandler() {
        // No google logout
        this.config = null;
    }

    public Oauth2LogoutHandler(Oauth2LoginConfig config) {
        this.config = config;
    }

    protected void signout(String accessToken) throws ClientProtocolException, IOException {
        HttpPost post = new HttpPost(getConfig().getOpenId().getRevocationEndpoint());
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("token", accessToken));
        post.setEntity(new UrlEncodedFormEntity(nvps));

        getConfig().getHttpUtil().execute(post);
    }

    @Override
    protected boolean remoteLogout(HttpServletRequest req, HttpServletResponse resp) throws ClientProtocolException, IOException {
        String accessToken = (String) req.getSession().getAttribute(GoogleOauth2.GOOGLE_AUTH_ACCESS_TOKEN);
        if (accessToken != null && getConfig() != null) {
            signout(accessToken);
            return true;
        }
        return false;
    }

    @Override
    public Oauth2LoginConfig getConfig() {
        return config;
    }
}
