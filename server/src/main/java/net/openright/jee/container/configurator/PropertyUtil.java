package net.openright.jee.container.configurator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map.Entry;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Wrap calls to System.get/setProperty so they can be filtered. */
public abstract class PropertyUtil {

    private static final Logger log = LoggerFactory.getLogger(PropertyUtil.class);

    public static Properties getProperties() {
        return System.getProperties();
    }

    public static void setProperties(Properties props) {
        System.setProperties(props);
    }

    public static String getProperty(String key) {
        return getProperty(key, null);
    }

    public static String getProperty(String key, String defaultValue) {
        return System.getProperty(key, defaultValue);
    }

    public static void setProperty(String key, String value) {
        System.setProperty(key, value);
    }

    public static void clearProperty(String key) {
        System.clearProperty(key);
    }

    public static boolean hasProperty(String key) {
        return getProperties().containsKey(key);
    }

    public static String getRequiredProperty(String key) {
        String val = getProperty(key);
        if (val == null) {
            throw new IllegalStateException("Missing property for key: " + key);
        }
        return val;
    }

    public static Properties getEnvironmentProperties() {
        Properties properties = new Properties();
        for (Entry<String, String> env : System.getenv().entrySet()) {
            String key = environmentNameToPropertyName(env.getKey());
            String value = env.getValue();
            properties.put(key, value);
        }
        return properties;
    }

    private static String environmentNameToPropertyName(String key) {
        return key.toLowerCase().replaceAll("_", "."); // NOSONAR
    }

    public static Properties loadPropertiesFromFile(File file) {
        try (Reader reader = new InputStreamReader(new FileInputStream(file), "UTF-8")) {
            Properties propertiesFromFile = new Properties();
            propertiesFromFile.load(reader);
            return propertiesFromFile;
        } catch (IOException e) {
            log.error("Could not load properties " + file, e);
            return new Properties();
        }
    }
}
