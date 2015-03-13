package net.openright.jee.container.configurator;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.rmi.registry.LocateRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JmxConfigurator {
    private static final Logger log = LoggerFactory.getLogger(JmxConfigurator.class);

    private static final String DEFAULT_DOMAIN = "net.openright.jee";

    private static JmxConfigurator instance;

    private final Map<String, Object> env = new HashMap<>();

    private final int port;

    private JMXConnectorServer cs;

    public static synchronized JmxConfigurator getJmxConfigurator() {
        if (instance == null) {
            instance = startJmxAgent();
        }
        return instance;
    }

    private JmxConfigurator(int port) throws IOException {
        this.port = port;
        String passwordFile = PropertyUtil.getProperty("com.sun.management.jmxremote.password.file");
        String accessFile = PropertyUtil.getProperty("com.sun.management.jmxremote.access.file");

        PropertyUtil.setProperty("java.rmi.server.randomIDs", "true");
        // Provide SSL-based RMI socket factories.
        //
        // The protocol and cipher suites to be enabled will be the ones
        // defined by the default JSSE implementation and only server
        // authentication will be required.
        //
        SslRMIClientSocketFactory csf = new SslRMIClientSocketFactory();
        SslRMIServerSocketFactory ssf = new SslRMIServerSocketFactory();
        env.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, csf);
        env.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE, ssf);

        env.put("com.sun.management.jmxremote.password.file", passwordFile);
        env.put("com.sun.management.jmxremote.access.file", accessFile);

        LocateRegistry.createRegistry(port);
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        // Se http://docs.oracle.com/javase/8/docs/technotes/guides/management/agent.html#gdfvq
        // Monitoring applications through a firewall
        String serviceUrl = "service:jmx:rmi://localhost:" +
                (port + 1) + // RMIService, RMIConnection port
                "/jndi/rmi://localhost:" +
                (port) // RMIRegistry
                + "/jmxrmi";
        
        log.info("JMX: Trying to connect to registry : {}", serviceUrl);
        
        JMXServiceURL url = new JMXServiceURL(serviceUrl);
        cs = JMXConnectorServerFactory.newJMXConnectorServer(url, env, mbs);

    }

    private void start() throws IOException {
        cs.start();
        log.info("JMX remote interface started at port [" + port + "]");
    }

    public synchronized void stop() {
        try {
            cs.stop();
        } catch (IOException e) {
            log.warn("Caught IO exception while closing JMX interface", e);
        }
        log.info("JMX remote interface stopped for port [" + port + "]");
        stopJmx();
    }

    private static void stopJmx() {
        removeAllMBeans(DEFAULT_DOMAIN);
        instance = null;
    }

    private static JmxConfigurator startJmxAgent() {
        int jmxPort = BasicConfigurator.getServerPort() + 1;
        JmxConfigurator jmxConfigurator;
        try {
            jmxConfigurator = new JmxConfigurator(jmxPort);
            jmxConfigurator.start();
            return jmxConfigurator;
        } catch (IOException e) {
            // do not throw exception, but log error, as it may fail when running over vpn
            log.error("Could not start JMX on port " + jmxPort, e);
            return null;
        }
    }

    public static void removeAllMBeans(String mbeanQuery) {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        Set<ObjectName> names = mbs.queryNames(createMBeanObjectNameFullyQualified(mbeanQuery), null);
        for (ObjectName objName : names) {
            try {
                mbs.unregisterMBean(objName);
            } catch (Exception ignored) { // NOSONAR
            }
        }
    }

    public static void removeAllMBeans() {
        removeAllMBeans(DEFAULT_DOMAIN + ":*");
    }

    private static ObjectName createMBeanObjectNameFullyQualified(String navn) {
        try {
            return new ObjectName(navn);
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException("Invalid mbean name [" + navn + "]", e);
        }
    }
}
