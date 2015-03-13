package net.openright.server.plugin.auth.saml;

import java.util.Collections;

import net.openright.jee.server.plugin.jetty.security.UserPrincipalImpl;

public class SamlUserPrincipal extends UserPrincipalImpl {


    protected SamlUserPrincipal(SamlHttpAuthorizer auth) {
        super(null
                , auth.getRemoteUserId()
                , null
                , null
                , null
                , null
                , Collections.emptyList()
                , Collections.emptyList());
    }

    @Override
    public String getName() {
        return super.getRemoteUserName();
    }

}
