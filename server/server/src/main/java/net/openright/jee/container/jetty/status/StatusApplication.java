package net.openright.jee.container.jetty.status;

import java.io.IOException;

import javax.servlet.ServletContext;

import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.codahale.metrics.servlets.AdminServlet;
import com.codahale.metrics.servlets.HealthCheckServlet;
import com.codahale.metrics.servlets.MetricsServlet;

public class StatusApplication {

    private static final String STATUS_CONNECTOR_NAME = "MyApplication-Status";

    private WebAppContext mainApplication;
    private Server server;

    public StatusApplication(Server server, WebAppContext mainApplication) {
        this.server = server;
        this.mainApplication = mainApplication;
    }

    public WebAppContext createStatusWebAppContext(String contextPath, int serverPort) throws IOException {
        StatusAndAdminWebAppContext webApp = initStatusWebAppContext(contextPath);
        if (webApp != null) {
            ServerConnector statusServerConnector = createServerConnector(server, serverPort + 5);
            server.addConnector(statusServerConnector);

            statusServerConnector.setName(STATUS_CONNECTOR_NAME);
            webApp.setVirtualHosts(new String[] { "@" + STATUS_CONNECTOR_NAME });
        }
        return webApp;
    }

    protected StatusAndAdminWebAppContext initStatusWebAppContext(String contextPath) throws IOException {
        final MetricRegistry metricRegistry = new MetricRegistry();
        final HealthCheckRegistry healthCheckRegistry = new HealthCheckRegistry();

        JmxReporter jmxReporter = JmxReporter.forRegistry(metricRegistry).build();
        jmxReporter.start();
        
        StatusAndAdminWebAppContext statusWebAppContext = new StatusAndAdminWebAppContext(mainApplication, contextPath);
        statusWebAppContext.addServlet(new ServletHolder(new AdminServlet()), "/status/*");

        bindMetricsAndHealthRegistry(mainApplication, metricRegistry, healthCheckRegistry);
        bindMetricsAndHealthRegistry(statusWebAppContext, metricRegistry, healthCheckRegistry);

        metricRegistry.register("jvm/gc", new GarbageCollectorMetricSet());
        metricRegistry.register("jvm/memory", new MemoryUsageGaugeSet());
        metricRegistry.register("jvm/thread-states", new ThreadStatesGaugeSet());
        metricRegistry.register("jvm/fd/usage", new FileDescriptorRatioGauge());

        return statusWebAppContext;

    }

    protected ServerConnector createServerConnector(Server server, int serverPort) {
        HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(createHttpConfiguration());
        httpConnectionFactory.getHttpConfiguration().setSendServerVersion(false);

        ServerConnector appServerConnector = new ServerConnector(server, httpConnectionFactory);
        appServerConnector.setPort(serverPort);
        appServerConnector.setReuseAddress(true);
        return appServerConnector;
    }
    
    protected HttpConfiguration createHttpConfiguration() {
        HttpConfiguration config = new HttpConfiguration();
        config.addCustomizer(new ForwardedRequestCustomizer());
        config.setSendServerVersion(false);
        config.setSendXPoweredBy(false);
        return config;
    }

    protected void bindMetricsAndHealthRegistry(WebAppContext appContext, final MetricRegistry metricRegistry,
            final HealthCheckRegistry healthCheckRegistry) {
        ServletContext adminServletContext = appContext.getServletContext();
        adminServletContext.setAttribute(MetricsServlet.METRICS_REGISTRY, metricRegistry);
        adminServletContext.setAttribute(HealthCheckServlet.HEALTH_CHECK_REGISTRY, healthCheckRegistry);
    }

}
