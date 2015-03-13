package net.openright.jee.server.plugin.jetty.security;


public class SecurityContext {

    public static UserPrincipalInfo currentUser(){
        return ThreadLocalSecurityContext.currentUser();
    }
}
