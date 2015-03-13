package net.openright.jee.server.plugin.jetty.security;

import java.io.IOException;
import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;
import javax.sql.DataSource;

import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.MappedLoginService;
import org.eclipse.jetty.server.UserIdentity;

/**
 * Custom {@link LoginService} which uses a DataSource for loading users
 */
public class DataSourceLoginService extends MappedLoginService {

    private final DataSource dataSource;
    private final String lookupUserSql;

    /**
     * @param lookupUserSql
     *            - select string for picking up user
     */
    public DataSourceLoginService(DataSource ds, String lookupUserSql) {
        this.dataSource = ds;
        this.lookupUserSql = lookupUserSql;
    }

    protected DataSource getDataSource() {
        return dataSource;
    }

    @Override
    protected UserIdentity loadUser(String userid) {
        try (Connection conn = getDataSource().getConnection();
                PreparedStatement stmt = conn.prepareStatement(lookupUserSql);) {

            stmt.setString(1, userid);

            try (ResultSet rs = stmt.executeQuery();) {
                if (rs.next()) {
                    return userFound(conn, rs);
                } else {
                    return userNotFound(conn, userid);
                }
            }
        } catch (SQLException e) {
            throw new IllegalArgumentException("Unable to get connection to lookup user");
        }
    }

    @Override
    protected void loadUsers() throws IOException {
        // Not implemented. use to precache users
    }

    protected UserIdentity userNotFound(@SuppressWarnings("unused") Connection conn, @SuppressWarnings("unused") String userid)
            throws SQLException {
        return null;
    }

    protected UserIdentity userFound(@SuppressWarnings("unused") Connection conn, ResultSet rs) throws SQLException {
        Principal principal = initPrincipal(rs);
        Subject subject = initSubject(principal, rs);
        String[] roles = initRoles(subject, principal);
        return initUserIdentity(principal, subject, roles);
    }

    protected UserIdentity initUserIdentity(Principal principal, Subject subject, String[] roles) {
        UserIdentity userIdentity = getIdentityService().newUserIdentity(subject, principal, roles);
        _users.put(principal.getName(), userIdentity);
        return userIdentity;
    }

    protected UserPrincipalImpl initPrincipal(ResultSet rs) throws SQLException {
        return new UserPrincipalImpl(rs.getInt(1), rs.getString(2), rs.getString(3));
    }

    protected String[] initRoles(@SuppressWarnings("unused") Subject subject, @SuppressWarnings("unused") Principal principal) {
        return new String[0];
    }

    protected Subject initSubject(Principal principal, @SuppressWarnings("unused") ResultSet rs) throws SQLException {
        Set<Object> pubCredentials = getPublicCredentials(principal);
        Set<Object> privCredentials = getPrivateCredentials(principal);
        return initSubject(principal, pubCredentials, privCredentials);
    }

    protected Subject initSubject(Principal principal, Set<Object> pubCredentials, Set<Object> privCredentials) {
        return new Subject(true, new HashSet<>(Arrays.asList(principal)), pubCredentials, privCredentials);
    }

    protected Set<Object> getPublicCredentials(@SuppressWarnings("unused") Principal principal) {
        return Collections.emptySet();
    }

    protected Set<Object> getPrivateCredentials(@SuppressWarnings("unused") Principal principal) {
        return Collections.emptySet();
    }

}
