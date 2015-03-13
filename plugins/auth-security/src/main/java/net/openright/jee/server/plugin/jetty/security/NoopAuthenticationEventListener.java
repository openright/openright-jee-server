package net.openright.jee.server.plugin.jetty.security;

public class NoopAuthenticationEventListener implements AuthenticationEventListener {

    @Override
    public void onLogin(UserPrincipalInfo userPrincipal, AuthenticationEvent event) {
    }

    @Override
    public void onLogout(UserPrincipalInfo userPrincipal, AuthenticationEvent event) {
    }

}
