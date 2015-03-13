package net.openright.jee.container.jetty.status;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.regex.Pattern;

import net.openright.jee.container.jetty.JettyWebAppContext;

import org.eclipse.jetty.webapp.WebAppContext;

/** Plugin status and admin war on a separate port. */
public class StatusAndAdminWebAppContext extends JettyWebAppContext {

    private static final String STATUS_WEBAPP_WAR = ".+[\\/]openright-jee-status-webapp.+\\.(jar|war).*";

    private static final String WEB_INF_WEB_XML = "WEB-INF/web.xml";

    @SuppressWarnings("unused")
    private WebAppContext appWebAppContext;

    public StatusAndAdminWebAppContext(WebAppContext appWebAppContext, String contextPath) throws IOException {
        super(contextPath, STATUS_WEBAPP_WAR);
        this.appWebAppContext = appWebAppContext;

        // This webapp doesn't have any CDI beans just now (if in the future it has, remove the below workaround)
        // however due to the following bug, it makes the application fail
        // https://issues.jboss.org/browse/WELD-1771

        // workaround (should be obsolete after Weld 2.2.7 / Weld 3.0.0)
        // @see http://www.eclipse.org/jetty/documentation/current/using-annotations.html
        this.setAttribute("org.eclipse.jetty.containerInitializerExclusionPattern",
                "org.jboss.weld.*");
    }

    @Override
    protected File initTempDirectory() {
        return new File("./webapps/status-webapp");
    }

    @Override
    protected String getWebDevHome(String webAppPattern) {
        String path = "../status-webapp";
        if (new File(path).exists()) {
            // running locally
            return path;
        } else {
            return getWebAppHomeThroughDependency(webAppPattern);
        }
    }

    /**
     * Stupid code to scan and find war hosting the web.xml for status webapp on the classpath when in IDE.
     */
    protected String getWebAppHomeThroughDependency(String webAppNamePattern) {

        ClassLoader classLoader = getClass().getClassLoader();
        try {
            Enumeration<URL> systemResources = classLoader.getResources(WEB_INF_WEB_XML);
            Pattern pattern = Pattern.compile(webAppNamePattern);

            while (systemResources.hasMoreElements()) {

                URL webXml = systemResources.nextElement();
                String webXmlExternalForm = webXml.toExternalForm();
                log.debug("Checking web.xml: {}", webXml);

                if (pattern.matcher(webXmlExternalForm).matches()) {
                    log.info("Using web.xml = " + webXml);
                    String webAppDep = webXmlExternalForm.replace(WEB_INF_WEB_XML, "");

                    if (webAppDep.indexOf(".war") > 0) {
                        webAppDep = webAppDep.substring(0, webAppDep.indexOf(".war") + 4);
                    } else if (webAppDep.indexOf(".jar") > 0) {
                        webAppDep = webAppDep.substring(0, webAppDep.indexOf(".jar") + 4);
                    } else {
                        throw new IllegalArgumentException("Unknown dependency war, must be (jar|war): " + webAppDep);
                    }

                    return webAppDep.replaceFirst("jar:file:", "");
                }
            }

            throw new IllegalArgumentException("Could not find any resources: " + WEB_INF_WEB_XML);

        } catch (IOException e) {
            throw new IllegalArgumentException("Could not find any resources: " + WEB_INF_WEB_XML, e);
        }

    }

}
