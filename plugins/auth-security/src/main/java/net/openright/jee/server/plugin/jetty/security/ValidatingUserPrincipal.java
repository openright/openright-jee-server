package net.openright.jee.server.plugin.jetty.security;

import java.security.Principal;
import java.time.Duration;

public interface ValidatingUserPrincipal extends Principal {

    boolean isExpired(Duration cacheExpiry);

}
