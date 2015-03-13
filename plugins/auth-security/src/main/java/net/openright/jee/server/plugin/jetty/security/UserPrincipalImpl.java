package net.openright.jee.server.plugin.jetty.security;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

public class UserPrincipalImpl implements UserPrincipalInfo {
    private final String remoteUserId;

    private OffsetDateTime loadTime = OffsetDateTime.now();

    private String userName;

    private String lastName;

    private String firstName;

    private String email;

    private Integer internalUserId;

    private Collection<String> permissions;

    private Collection<String> featureToggles;

    public UserPrincipalImpl(Integer internalUserId, String remoteUserId, String userName) {
        this(internalUserId, remoteUserId, userName, null, null, null, Collections.emptyList(), Collections.emptyList());
    }

    public UserPrincipalImpl(Integer internalUserId, String remoteUserId, String userName, String email, String firstName, String lastName
            , Collection<String> permissions
            , Collection<String> featureToggles) {
        this.internalUserId = internalUserId;
        this.remoteUserId = remoteUserId;
        this.userName = userName;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.permissions = Collections.unmodifiableCollection(permissions);
        this.featureToggles = Collections.unmodifiableCollection(featureToggles);
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public Integer getInternalUserId() {
        return internalUserId;
    }

    @Override
    public Collection<String> getPermissions() {
        return permissions;
    }

    @Override
    public boolean hasFeature(String feature) {
        return featureToggles.contains(feature);
    }

    /**
     * Remote id of user.
     */
    @Override
    public String getRemoteUserId() {
        return remoteUserId;
    }

    public String getRemoteUserName() {
        return userName;
    }

    @Override
    public String getName() {
        return getRemoteUserId();
    }

    @Override
    public boolean isExpired(Duration duration) {
        return !loadTime.plus(duration).isAfter(OffsetDateTime.now());
    }

    @Override
    public void resetLoadTime() {
        this.loadTime = OffsetDateTime.now();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<id=" + remoteUserId + ", name=" + userName + ", loadTime=" + loadTime + ">";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !getClass().equals(obj.getClass())) {
            return false;
        }
        UserPrincipalImpl other = (UserPrincipalImpl) obj;
        return Objects.equals(remoteUserId, other.remoteUserId)
                && Objects.equals(userName, other.userName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(remoteUserId, userName);
    }
}