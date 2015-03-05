package net.openright.jee.container.jetty;

import java.lang.management.ManagementFactory;

import net.openright.jee.container.configurator.JmxConfigurator;
import net.openright.jee.container.configurator.PropertyUtil;
import net.openright.jee.container.jetty.status.StatusApplication;

import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.rewrite.handler.RedirectRegexRule;
import org.eclipse.jetty.rewrite.handler.RewriteHandler;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.ConnectorStatistics;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.codahale.metrics.jetty9.InstrumentedConnectionFactory;

public class JettyServerStarter {
    private static final String JETTY_JMX_BEANS_DOMAIN = "org.eclipse.jetty";
    private static Logger log = LoggerFactory.getLogger(JettyServerStarter.class);

    private static final String DEFAULT_CONNECTOR_NAME = "MyApplication";

    public static String getShutdownToken() {
        return PropertyUtil.getProperty("server.shutdowntoken",
                "lajhssdssadasdf6566788<'kgldfkah1633alsdfwealgo4wafdassdfvdfhawfAFDf44llfa");
    }

    private MBeanContainer mBeanContainer;
    private final Server server;
    private final JettyWebAppContext webAppContext;

    public JettyServerStarter(JettyWebAppContext webAppContext, int serverPort) throws Exception { // NOSONAR
        this.webAppContext = webAppContext;
        this.server = createServer(webAppContext, serverPort);
    }

    public void startServer() throws Exception {// NOSONAR
        server.start();
        webAppContext.verifyStartup();
    }

    protected Server createServer(JettyWebAppContext appContext, int serverPort) throws Exception {// NOSONAR
        String contextPath = appContext.getContextPath();

        Server nyServer = new Server();

        nyServer.setStopAtShutdown(true);
        nyServer.setStopTimeout(3000);
        mBeanContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
        mBeanContainer.setDomain(JETTY_JMX_BEANS_DOMAIN);
        nyServer.addBean(mBeanContainer);

        ServerConnector appServerConnector = createServerConnector(nyServer, serverPort);
        bindConnectorToWebAppContext(appServerConnector, webAppContext, DEFAULT_CONNECTOR_NAME);

        nyServer.addConnector(appServerConnector);
        HandlerList handlers = new HandlerList();

        WebAppContext statusWebAppContext = new StatusApplication(nyServer, appContext)
                .createStatusWebAppContext("/", serverPort);

        handlers.setHandlers(new Handler[] {
                statusWebAppContext
                , appContext
                , new ShutdownHandler(getShutdownToken())
                , createRewriteHandler("^/(?!status.*)", contextPath)
        });

        RequestLogHandler requestLogHandler = createRequestLogHandler();
        requestLogHandler.setHandler(handlers);

        StatisticsHandler statisticsHandler = new StatisticsHandler();
        statisticsHandler.setHandler(requestLogHandler);
        nyServer.setHandler(statisticsHandler);

        return nyServer;
    }

    /**
     * Create a server connector on the given port.
     */
    protected ServerConnector createServerConnector(Server server, int serverPort) {
        HttpConfiguration config = createHttpConfiguration();

        HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(config);

        ConnectionFactory connFactory = new InstrumentedConnectionFactory(httpConnectionFactory, new Timer());

        ServerConnector appServerConnector = new ServerConnector(server, connFactory);
        appServerConnector.setPort(serverPort);
        appServerConnector.setReuseAddress(true);
        appServerConnector.addBean(new ConnectorStatistics());
        return appServerConnector;
    }

    protected HttpConfiguration createHttpConfiguration() {
        HttpConfiguration config = new HttpConfiguration();
        config.addCustomizer(new ForwardedRequestCustomizer());
        config.setSendServerVersion(false);
        config.setSendXPoweredBy(false);
        return config;
    }

    private RewriteHandler createRewriteHandler(String redirectFrom, String redirectTo) {
        RewriteHandler rewriteHandler = new RewriteHandler();
        rewriteHandler.setRewriteRequestURI(true);
        rewriteHandler.setRewritePathInfo(true);
        
        
        RedirectRegexRule redirectRule = new RedirectRegexRule();
        redirectRule.setRegex(redirectFrom);
        redirectRule.setReplacement(redirectTo);
        rewriteHandler.addRule(redirectRule);
        return rewriteHandler;
    }

    private static RequestLogHandler createRequestLogHandler() {
        RequestLogHandler requestLogHandler = new RequestLogHandler();
        NCSARequestLog requestLog = new NCSARequestLog("logs/yyyy_mm_dd.request.log");
        requestLog.setPreferProxiedForAddress(true);
        requestLog.setRetainDays(3);
        requestLog.setAppend(true);
        requestLog.setLogTimeZone("UTC+01:00");
        requestLog.setExtended(false);
        requestLogHandler.setRequestLog(requestLog);
        return requestLogHandler;
    }

    public int getServerPort() {
        return ((ServerConnector) server.getConnectors()[0]).getLocalPort();
    }

    WebAppContext getWebAppContext() {
        return webAppContext;
    }

    public void stopServer(int gracefulTimeout) {
        // controlled shutdown
        try {
            JmxConfigurator.removeAllMBeans();
            mBeanContainer.destroy();
            JmxConfigurator.removeAllMBeans(JETTY_JMX_BEANS_DOMAIN + ":*");
            if (gracefulTimeout > 0) {
                server.setStopTimeout(gracefulTimeout);
            }
            server.stop();
            server.join();
        } catch (Exception e) {
            log.error("feil ved stop av server:" + e.getMessage());
        }
    }

    /**
     * Only this webApp will be served with the given connector.
     * Ensures the webapp only responds on the connectors port.
     * 
     * @param connectorName
     *            - Name used to bind the connector to the webapp
     * @see http://www.eclipse.org/jetty/documentation/current/serving-webapp-from-particular-port.html
     */
    public static void bindConnectorToWebAppContext(ServerConnector connector, WebAppContext webApp, String connectorName) {
        connector.setName(connectorName);
        webApp.setVirtualHosts(new String[] { "@" + connectorName });
    }
}
