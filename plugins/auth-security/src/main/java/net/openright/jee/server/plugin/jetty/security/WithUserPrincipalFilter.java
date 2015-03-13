package net.openright.jee.server.plugin.jetty.security;

import java.io.IOException;
import java.security.Principal;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

public class WithUserPrincipalFilter implements javax.servlet.Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        Principal userPrincipal = ((HttpServletRequest) request).getUserPrincipal();

        if (userPrincipal instanceof UserPrincipalInfo) {
            try {
                UserPrincipalInfo up = (UserPrincipalInfo) userPrincipal;
                ThreadLocalSecurityContext.runWithUser(
                        () -> {
                            doFilterWithUserPrincipalInContext(request, response, chain, up);
                        }
                        , up);
            } catch (Exception e) {
                throw new ServletException(e);
            }
        } else {
            chain.doFilter(request, response);
        }

    }

    protected void doFilterWithUserPrincipalInContext(ServletRequest request, ServletResponse response, final FilterChain chain, UserPrincipalInfo up)
            throws IOException,
            ServletException {
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

}
