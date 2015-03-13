package net.openright.jee.server.plugin.jetty.security;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.http.client.ClientProtocolException;

public class CoreLogoutServlet extends HttpServlet {

    protected final String postLogoutUrl;
    private final List<CoreLogoutHandler> logoutHandlers;

    public CoreLogoutServlet(String postLogoutUrl, CoreLogoutHandler logoutHandler) {
        this(postLogoutUrl, Arrays.asList(logoutHandler));
    }

    public CoreLogoutServlet(String postLogoutUrl, List<CoreLogoutHandler> logoutHandlers) {
        this.postLogoutUrl = postLogoutUrl;
        this.logoutHandlers = logoutHandlers;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doExecute(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doExecute(req, resp);
    }

    protected void doExecute(HttpServletRequest req, HttpServletResponse resp) throws ClientProtocolException, IOException,
            ServletException {
        HttpSession session = req.getSession();

        Principal userPrincipal = req.getUserPrincipal();

        try {
            for (CoreLogoutHandler logout : logoutHandlers) {
                if (logout.remoteLogout(req, resp)) {
                    if (userPrincipal instanceof UserPrincipalInfo) {
                        logout.getConfig().getEventListener().onLogout((UserPrincipalInfo) userPrincipal, new AuthenticationEvent(req));
                    }
                    break;
                }
            }
        } finally {
            session.invalidate();
        }

        if (!resp.isCommitted()) {
            if (postLogoutUrl != null) {
                req.getRequestDispatcher(postLogoutUrl).forward(req, resp);
            } else {
                resp.setContentType("text/plain");
                PrintWriter writer = resp.getWriter();
                writer.write("User logged out");
                writer.flush();
            }
        }
    }

}
