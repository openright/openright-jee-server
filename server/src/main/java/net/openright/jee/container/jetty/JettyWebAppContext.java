package net.openright.jee.container.jetty;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import net.openright.jee.BuildInfo;

import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppClassLoader;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class JettyWebAppContext extends WebAppContext {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final CountDownLatch startupLatch = new CountDownLatch(1);

    private String webAppLocationPattern;

    /**
     * Create a webapp context for a given war/webapp location.
     * 
     * @param contextPath
     *            - for where the webapp is hosted ("e.g. at root '/' or a sub-path '/cgi-bin/proxy/')
     * @param webAppLocationPattern
     *            - a pattern to match webapp location. used to find web.xml from classpath in proper war file. May be a
     *            regex.
     * @throws IOException
     */
    public JettyWebAppContext(String contextPath, String webAppLocationPattern) throws IOException {
        this.webAppLocationPattern = webAppLocationPattern;
        initConfigurations();

        initContext(contextPath);
        initStartupListener();

        getServletHandler().setStartWithUnavailable(false);

        setErrorHandler(initErrorHandler());
    }

    protected ErrorHandler initErrorHandler() throws IOException {
        class WebAppErrorPageErrorHandler extends ErrorPageErrorHandler {
            @Override
            protected void writeErrorPageBody(HttpServletRequest request, Writer writer, int code, String message, boolean showStacks)
                    throws IOException {
                String uri = request.getRequestURI();

                writeErrorPageMessage(request, writer, code, message, uri);
                if (showStacks)
                    writeErrorPageStacks(request, writer);
            }
        };
        ErrorHandler errorHandler = new WebAppErrorPageErrorHandler();
        
        errorHandler.setShowStacks(BuildInfo.isDevelopmentMode());
        
        return errorHandler;
    }

    protected Configuration[] createConfigurations() {
        Configuration[] configurations = new Configuration[] {
                new WebInfConfiguration()
                , new WebXmlConfiguration()
                , new AnnotationConfiguration()
                // , new MetaInfConfiguration() // skip - leter etter webfragment.xml i jars i WEB-INF/lib/*.jar
                // , new JettyWebXmlConfiguration() // skip - leser jetty-web.xml o.l.
                // , new FragmentConfiguration() // skip - leser web fragments fra jars i WEB-INF/lib/*.jar
        };
        return configurations;
    }

    protected void initAttributesAndProperties() {
        // turn off file listing
        setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");

        System.getProperty("org.apache.jasper.compiler.disablejsr199", Boolean.toString(isDisableJsr199()));

        super.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
                getScanContainerIncludePattern());

        super.setAttribute("org.eclipse.jetty.server.webapp.WebInfIncludeJarPattern",
                getScanWarLibIncludePattern());
    }

    /**
     * disable jsr199 compiler, use apache-jsp (eclipse edt) compiler for compiling jsps.
     * avoids having to have jdk to run.
     * To enable, the apache-jsp compiler must be on classpath for server, otherwise ignored.
     */
    protected boolean isDisableJsr199() {
        return true;
    }

    /** Servlet 3.1: Only scan relevant classes and jars in WAR for annotations */
    protected String getScanWarLibIncludePattern() {
        return ".*/WEB-INF/classes/"
                + "|.*/WEB-INF/lib/.*-webapp[\\.]*\\.jar$"
                + "|.*/WEB-INF/lib/jersey-[^/]*\\.jar$"
                + "|.*/WEB-INF/lib/jackson-[^/]*\\.jar$"
                + "|.*/WEB-INF/lib/[^/]*taglibs.*\\.jar$";
    }

    /** Servlet 3.1: Only scan relevant jars and classes for annotations */
    protected String getScanContainerIncludePattern() {
        return ".*/classes/.*"
                + "|.*-webapp[^/]*\\.jar"
                + "|.*/[^/]*servlet-api-[^/]*\\.jar$"
                + "|.*/javax.servlet.jsp.jstl-.*\\.jar$"
                + "|.*/jersey-[^/]*\\.jar$"
                + "|.*/jackson-[^/]*\\.jar$"
                + "|.*/[^/]*taglibs.*\\.jar$";
    }

    private void initConfigurations() {
        initAttributesAndProperties();
        setConfigurations(createConfigurations());
    }

    protected void initContext(String contextPath) throws IOException {
        String warFil = BuildInfo.getLocation(getClass());
        log.info("Using war = " + warFil);

        if (isDevelopment()) {
            // started from IDE or maven
            initContextForIde(contextPath);
        } else {
            initContextForServer(contextPath);
        }

        File webappdir = initTempDirectory();
        if (!(webappdir.exists() || webappdir.mkdirs())) {
            throw new IllegalArgumentException("Webappdir " + webappdir + " could not be created");
        }
        setTempDirectory(webappdir);

    }

    protected File initTempDirectory() {
        return new File("./webapps");
    }

    protected void initContextForServer(String contextPath) throws IOException {
        setContextPath(contextPath);
        setWar(getWarFile(webAppLocationPattern));
        setClassLoader(new WebAppClassLoader(getClass().getClassLoader(), this));
    }

    protected String getWarFile(String pattern) {
        String wars = System.getProperty("app.war.files", "");
        for (String candidate : Arrays.asList(wars.split(":"))) {
            log.info("Looking at war file {}", candidate);
            if (candidate.matches(pattern)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Return location of webapp in development (source files, resources)
     * 
     * @param webAppLocationPattern
     */
    protected abstract String getWebDevHome(String webAppLocationPattern);

    protected void initContextForIde(String contextPath) {
        setContextPath(contextPath);

        File webappProject = new File(getWebDevHome(webAppLocationPattern));
        if (webappProject.exists()) {
            initWarForIde(webappProject);
        } else {
            throw new IllegalArgumentException("Cannot find webapp project.  Expecting it to be in " + webappProject
                    + ", relative to current working directory: " + new File(".").getAbsolutePath());
        }
    }

    protected void initWarForIde(File webappProject) {
        File srcWarPath = new File(webappProject, "/src/main/webapp");
        if (srcWarPath.exists()) {
            // running in relative-to-own module
            setWar(srcWarPath.getPath());
        } else {
            // war in different module
            setWar(webappProject.getPath());
        }

        File targetClasses = new File(webappProject, "/target/classes");
        if (targetClasses.exists()) {
            // targetClasses in own module
            setExtraClasspath(targetClasses.getPath());
        }
    }

    public boolean isDevelopment() throws IOException {
        // naive check if running in maven folder or in IDE
        File current = new File(new File(".").getCanonicalPath());
        boolean iEclipse = new File(current, "target/classes").exists();
        boolean iMavenBygg = new File(current, "classes").exists() && current.getParent().contains("target");
        return iEclipse || iMavenBygg;
    }

    private void initStartupListener() {
        addLifeCycleListener(new AbstractLifeCycle.AbstractLifeCycleListener() {
            @Override
            public void lifeCycleFailure(LifeCycle event, Throwable cause) {
                startupLatch.countDown();
            }

            @Override
            public void lifeCycleStarted(LifeCycle event) {
                startupLatch.countDown();
            }
        });

    }

    public void verifyStartup() throws InterruptedException {
        if (!startupLatch.await(1, TimeUnit.MINUTES)) {
            throw new IllegalStateException("Timeout at startup");
        }
        if (getUnavailableException() != null) {
            throw new IllegalStateException("WebAppContext could not be started", getUnavailableException());
        }
        if (!isAvailable()) {
            throw new IllegalStateException("WebAppContext is not accessible");
        }
        verifyServletsAreAllRunning();
    }

    public void verifyServletsAreAllRunning() {
        ServletHolder[] servlets = getServletHandler().getServlets();
        if (servlets == null) {
            throw new IllegalStateException("No servlets configured for WebAppContext");
        }
        for (ServletHolder s : servlets) {
            if (s.isFailed() || s.getUnavailableException() != null) {
                throw new IllegalStateException("servlet [" + s.getName() + "] failed", s.getUnavailableException());
            } else if (s.isStopped()) {
                throw new IllegalStateException("servlet [" + s.getName() + "] has stopped", s.getUnavailableException());
            } else if (!s.isAvailable()) {
                throw new IllegalStateException("servlet [" + s.getName() + "] is unavailable", s.getUnavailableException());
            }
        }
    }

    public void unpack() throws Exception { // NOSONAR
        new WebInfConfiguration().preConfigure(this);
    }

}
