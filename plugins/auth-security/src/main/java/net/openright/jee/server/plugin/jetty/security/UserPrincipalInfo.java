package net.openright.jee.server.plugin.jetty.security;

import java.io.Serializable;
import java.security.Principal;
import java.time.Duration;
import java.util.Collection;

public interface UserPrincipalInfo extends Principal, Serializable  {

    String getRemoteUserId();

    Integer getInternalUserId();

    String getEmail();

    String getLastName();

    String getFirstName();

    boolean isExpired(Duration duration);

    void resetLoadTime();

    boolean hasFeature(String feature);

    Collection<String> getPermissions();
    

}
