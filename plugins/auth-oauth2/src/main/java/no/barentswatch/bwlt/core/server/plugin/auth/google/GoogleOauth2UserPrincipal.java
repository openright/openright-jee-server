package no.barentswatch.bwlt.core.server.plugin.auth.google;

import java.util.Collections;

import net.openright.jee.server.plugin.jetty.security.UserPrincipalImpl;

public class GoogleOauth2UserPrincipal extends UserPrincipalImpl {

    private String accessToken;

    protected GoogleOauth2UserPrincipal(GoogleOauth2 auth) {
        super(null
                , auth.getRemoteUserId()
                , auth.getRemoteUserEmail()
                , auth.getRemoteUserEmail()
                , auth.getRemoteUserGivenName()
                , auth.getRemoteUserFamilyName()
                , Collections.emptyList()
                , Collections.emptyList());
        this.accessToken = auth.getRemoteAccessToken();
    }

    public String getAccessToken() {
        return accessToken;
    }

    @Override
    public String getName() {
        return super.getRemoteUserName();
    }

}
