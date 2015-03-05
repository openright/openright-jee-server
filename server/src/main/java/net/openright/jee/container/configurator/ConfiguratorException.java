package net.openright.jee.container.configurator;

public class ConfiguratorException extends RuntimeException {
    public static ConfiguratorException databaseConnectionFeil(String message) {
        return new ConfiguratorException("CFG-001: " + message);
    }

    public static ConfiguratorException databaseKonfigurasjonsFeil(String message) {
        return new ConfiguratorException("CFG-002: " + message);
    }

    public static ConfiguratorException jmxKonfigurasjonsFeil(String message) {
        return new ConfiguratorException("CFG-003: " + message);
    }
    
    private ConfiguratorException(String message, Throwable cause) {
        super(message, cause);
    }

    private ConfiguratorException(String message) {
        super(message);
    }

}
