package net.openright.jee.container.configurator;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.naming.NamingException;
import javax.servlet.ServletContext;

import net.openright.jee.BuildInfo;
import net.openright.jee.security.MasterKeyCrypto;

import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicConfigurator implements SecuredInput {
    public static final String APP_HTTP_PORT = "app.http.port";
    private static final String INSTALL_LOCATION = "app.install.location";

    public static final String APPLICATION_NAME = "app.name";

    private static final Logger log = LoggerFactory.getLogger(BasicConfigurator.class);

    private final Map<String, Properties> propertySources = new HashMap<>();
    private final List<String> propertyPriority = new ArrayList<>();

    private final File passwordFile = new File(".password");
    private byte[] masterKey;
    private JmxConfigurator jmxConfigurator;
    private final List<File> applicationPropertyFiles;

    {
        propertySources.put("system property", PropertyUtil.getProperties());
        propertyPriority.add("system property");
    }

    public BasicConfigurator(List<File> propertyFiles, char[] masterKey) throws IOException {
        this.applicationPropertyFiles = propertyFiles;
        this.masterKey = masterKey != null ? MasterKeyCrypto.convertCharToBytesUTF(masterKey) : null;
        if (masterKey != null) {
            Arrays.fill(masterKey, ' ');
        }

        for (File props : this.applicationPropertyFiles) {
            log.info(String.format("Reading configuration from %s", props.getAbsolutePath()));
            mergePropertiesIntoSystem(props.getName(), PropertyUtil.loadPropertiesFromFile(props));
        }

        if (passwordFile.exists()) {
            mergePropertiesIntoSystem("<secrets>", PropertyUtil.loadPropertiesFromFile(passwordFile));
        }
        if (!BuildInfo.isDevelopmentMode()) {
            // starter jmx - må startes før database connections ellers vil default jmx initialiseres.
            this.jmxConfigurator = JmxConfigurator.getJmxConfigurator();
        }
    }

    public void readConfiguration() throws SQLException, PropertyVetoException {
        initBasicProperties();
        mergePropertiesIntoSystem("environment", PropertyUtil.getEnvironmentProperties());
    }

    private void initBasicProperties() {
        PropertyUtil.setProperty("hostname", getHostName());
        PropertyUtil.setProperty("pid", getPid());
        String installLocation = getClass().getProtectionDomain().getCodeSource().getLocation().toExternalForm();
        log.debug(INSTALL_LOCATION + ": {}", installLocation);
        PropertyUtil.setProperty(INSTALL_LOCATION, installLocation);
    }

    public static int getServerPort() {
        String property = PropertyUtil.getProperty(APP_HTTP_PORT);
        if (property == null) {
            throw new IllegalStateException("Property \"" + APP_HTTP_PORT + "\" is not set.");
        }
        return Integer.parseInt(property);
    }

    public void verifyConfiguration() {
        // TEMPLATE METHOD
    }

    public void configureServletContext(ServletContext servletContext) {
        // add application configured properties to servletcontext
        Properties props = new Properties();
        for (File f : this.applicationPropertyFiles) {
            if (addToServletContext(f)) {
                log.debug("Loading propertyfile: {}", f.getAbsolutePath());
                props.putAll(PropertyUtil.loadPropertiesFromFile(f));
            }
        }
        // since we've not started, yet delegate to underlying context handler
        ContextHandler contextHandler = WebAppContext.getContextHandler(servletContext);
        for (Entry<Object, Object> entry : props.entrySet()) {
            contextHandler.setInitParameter((String) entry.getKey(), (String) entry.getValue());
        }
    }

    protected boolean addToServletContext(File f) {
        return f.getName().matches("app.properties");
    }

    private void mergePropertiesIntoSystem(String propertySource, Properties additionalProperties) {
        propertySources.put(propertySource, additionalProperties);
        propertyPriority.add(propertySource);

        Properties properties = new Properties();
        properties.putAll(additionalProperties);
        properties.putAll(PropertyUtil.getProperties());
        PropertyUtil.setProperties(properties);
    }

    String getPropertySource(String key) {
        for (String propertySource : propertyPriority) {
            if (propertySources.get(propertySource).containsKey(key)) {
                return propertySource;
            }
        }
        return "property ikke i database og ikke overskrevet";
    }

    static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "ukjent";
        }
    }

    static String getPid() {
        return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
    }

    public void initConfiguration() throws NamingException {
    }

    public void completeConfiguration() {
        clearMasterKey();
    }

    private void clearMasterKey() {
        if (this.masterKey != null) {
            Arrays.fill(this.masterKey, (byte) 0);
            this.masterKey = null;
        }
    }

    @Override
    public String decrypt(String input) {
        if (input != null && input.startsWith("CRYPT:")) {
            input = input.replace("CRYPT:", "");
            return this.masterKey != null ? MasterKeyCrypto.decrypt(input, this.masterKey) : input;
        }
        return input;
    }

    public void stop() {
        if (this.jmxConfigurator != null) {
            this.jmxConfigurator.stop();
        }
    }

    public void setApplicationName(String navn) {
        PropertyUtil.setProperty(APPLICATION_NAME, navn);
    }

}
