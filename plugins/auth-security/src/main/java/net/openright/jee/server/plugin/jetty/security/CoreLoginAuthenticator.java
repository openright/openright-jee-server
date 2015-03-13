package net.openright.jee.server.plugin.jetty.security;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.security.authentication.LoginAuthenticator;

public abstract class CoreLoginAuthenticator extends LoginAuthenticator {

    public abstract boolean accept(HttpServletRequest req);

}