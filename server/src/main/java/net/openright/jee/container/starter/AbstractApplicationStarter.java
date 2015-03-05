package net.openright.jee.container.starter;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NamingException;

import net.openright.jee.BuildInfo;
import net.openright.jee.container.configurator.BasicConfigurator;
import net.openright.jee.container.configurator.LogConfigurator;
import net.openright.jee.container.configurator.PropertyUtil;
import net.openright.jee.container.jetty.JettyServerStarter;
import net.openright.jee.container.jetty.JettyWebAppContext;
import net.openright.jee.container.jetty.ShutdownHandler;

import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.MultiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import ch.qos.logback.classic.LoggerContext;

import com.codahale.metrics.servlets.PingServlet;

/**
 * This class bootstraps Jetty and starts the application.
 * Classes used by this must only exist in the server module, or
 * the classpath will not be correct at runtime.
 */
public abstract class AbstractApplicationStarter {

    protected void start(String[] args) throws Exception { // NOSONAR
        setSystemDefaults();
        if (args.length < 1) {
            stopWithError("Failed: Missing command");
        }
        String cmd = args[0];

        MDC.put("app.run.command", cmd);
        new LogConfigurator().initLog(new File("conf/logback-config.xml"));

        if ("extract".equals(cmd)) {
            // brukes fra configure.sh
            extractWarFile();
            return;
        }

        getLogger().info(String.format("**** %s running command [%s] ****", BuildInfo.getAppName(), cmd));
        getLogger().info(
                String.format("%s v.%s , fra %s", BuildInfo.getAppName(), BuildInfo.getAppVersjon(), BuildInfo.getLocation(getClass())));

        // TODO: (FC) read as char array instead of String (so it can be erased after use)
        char[] masterKey = new CommandLineInterpreter().withMaskInput(false).withWaitForInput(false).readLineFromConsoleAsArray(null);
        BasicConfigurator basicConfigurator = this.initConfiguration(args, masterKey);

        // merk: kun start kalles i servermiljø. restart benyttes kun i Eclipse
        switch (cmd) {
            case "start":
                this.start(basicConfigurator);
                break;
            case "stop":
                this.stop();
                break;
            case "restart":
                this.stop();
                this.start(basicConfigurator);
                break;
            default:
                stopWithError("Failed: unknown command '" + cmd + "' " + usage());
                break;
        }
    }

    protected abstract void extractWarFile() throws Exception; // NOSONAR

    public static void setSystemDefaults() {
        //
    }

    private BasicConfigurator initConfiguration(String[] args, char[] masterKey)
            throws IOException, SQLException,
            PropertyVetoException,
            NamingException {
        long start = System.currentTimeMillis();

        BasicConfigurator basicConfigurator = createConfiguration(getDefaultConfigurationFiles(), masterKey);
        initConfiguration(basicConfigurator, args, masterKey);

        getLogger().debug("Server configured in " + (System.currentTimeMillis() - start) + "ms");
        return basicConfigurator;
    }

    protected abstract BasicConfigurator createConfiguration(List<File> defaultConfigurationFiles, char[] masterKey) throws IOException;

    protected List<File> getDefaultConfigurationFiles() {
        String configFile = PropertyUtil.getProperty("app.propertyfile", "app.properties");
        List<File> files = new LinkedList<>();
        if (configFile != null) {
            files.add(assertExists(configFile));
        }
        return files;
    }

    protected void initConfiguration(BasicConfigurator basicConfigurator, @SuppressWarnings("unused") String[] args,
            @SuppressWarnings("unused") char[] masterKey) throws SQLException,
            PropertyVetoException,
            NamingException {
        basicConfigurator.readConfiguration();
        basicConfigurator.verifyConfiguration();
        basicConfigurator.initConfiguration();
        basicConfigurator.completeConfiguration();
        basicConfigurator.setApplicationName(BuildInfo.getAppName());
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(AbstractApplicationStarter.class);
    }

    protected static File assertExists(final String filename) {
        File file = new File(filename);
        if (!file.exists()) {
            String melding = "Missing " + filename
                    + ". Run command: \n\t extract\n and verify config in " + filename + " is correct.";
            stopWithError(melding);
        }
        return file;
    }

    private static String usage() {
        return "Usage: <cmd> [COMMAND] [OPTION]..." +
                "\n\n\tCOMMAND:\n" +
                "\t\tstart\n" +
                "\t\tstop\n" +
                "\t\trestart\n";
    }

    private void stop() throws SocketTimeoutException {
        ShutdownHandler.attemptShutdownFromClient(BasicConfigurator.getServerPort(), getShutdownToken());
    }

    protected void start(BasicConfigurator basicConfigurator) throws NamingException, IOException, SQLException, PropertyVetoException {
        long start = System.currentTimeMillis();
        Thread.setDefaultUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler());

        int serverPort = BasicConfigurator.getServerPort();
        try {
            JettyServerStarter jettyServerStarter = createServerStarter(serverPort, createWebAppContext(basicConfigurator));
            jettyServerStarter.startServer();
            Runtime.getRuntime().addShutdownHook(new MyShutdownHook(basicConfigurator, jettyServerStarter));

            boolean dev = BuildInfo.isDevelopmentMode();
            String verFormat = String.format("v.%s", BuildInfo.getAppVersjon());
            getLogger().info("******** Started {} ({}): port={}. {} ********", BuildInfo.getAppName(), getContextPath(), serverPort,
                    (dev ? "(DEVELOPMENT)" : verFormat));
            getLogger().debug("{} ({}) started in {}ms", BuildInfo.getAppName(), getContextPath(), (System.currentTimeMillis() - start));
        } catch (Exception e) {
            Throwable t = getFirstCause(e);
            String feilmelding = "Failed: Could not start " + BuildInfo.getAppName() + " (port " + serverPort
                    + ". See log for more) : " + t.getMessage();
            getLogger().error(feilmelding, t);
            stopWithError(feilmelding);
        }
    }

    protected JettyServerStarter createServerStarter(int serverPort, JettyWebAppContext webApp) throws Exception { // NOSONAR
        return new JettyServerStarter(webApp, serverPort);
    }

    protected JettyWebAppContext createWebAppContext(BasicConfigurator configurator) throws IOException {
        String contextPath = getContextPath();
        JettyWebAppContext webAppContext = createWebAppContext(contextPath);
        configurator.configureServletContext(webAppContext.getServletContext());
        initPingServlet(webAppContext);
        return webAppContext;
    }

    protected void initPingServlet(JettyWebAppContext webAppContext) {
        webAppContext.addServlet(new ServletHolder(new PingServlet()), "/ping");
    }

    private String getContextPath() {
        return PropertyUtil.getProperty("app.http.contextpath", "/");
    }

    protected abstract JettyWebAppContext createWebAppContext(String contextPath) throws IOException;

    private Throwable getFirstCause(Exception e) {
        Throwable t = e;
        if (e.getCause() != null) {
            if (e.getCause() instanceof MultiException) {
                MultiException exceptions = (MultiException) e.getCause();
                t = getOriginalCause(exceptions.getThrowable(0));
            } else {
                t = getOriginalCause(e);
            }
        }
        return t;
    }

    private Throwable getOriginalCause(Throwable e) {
        while (e.getCause() != null) {
            e = getOriginalCause(e.getCause());
        }
        return e;
    }

    private static String getShutdownToken() {
        return JettyServerStarter.getShutdownToken();
    }

    private static void stopWithError(String feilmelding) {
        System.err.println(feilmelding); // NOSONAR
        System.err.println(usage()); // NOSONAR
        System.exit(1);// NOSONAR
    }

    private static class DefaultUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            LoggerFactory.getLogger(DefaultUncaughtExceptionHandler.class).error("Unknown fault in thread " + t.getName(), e);
        }
    }

    private static class MyShutdownHook extends Thread {
        private final JettyServerStarter jettyServerStarter;
        private final BasicConfigurator configurator;

        private MyShutdownHook(BasicConfigurator configurator, JettyServerStarter jettyServerStarter) {
            this.configurator = configurator;
            this.jettyServerStarter = jettyServerStarter;
        }

        @Override
        public synchronized void start() {
            LoggerFactory.getLogger(MyShutdownHook.class).info("Stopping {}...", BuildInfo.getAppName());
            configurator.stop();
            if (LoggerFactory.getILoggerFactory() instanceof LoggerContext) {
                ((LoggerContext) LoggerFactory.getILoggerFactory()).stop();
            }
            jettyServerStarter.stopServer(2000);
        }
    }

}
