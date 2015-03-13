package net.openright.jee.server.plugin.jetty.security;


public interface AuthenticationEventListener {

    void onLogin(UserPrincipalInfo userPrincipal, AuthenticationEvent evt);
    
    void onLogout(UserPrincipalInfo userPrincipal, AuthenticationEvent evt);
}
