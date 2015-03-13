package net.openright.jee.container.configurator;

import java.io.File;
import java.util.logging.LogManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

public class LogConfigurator {

    public void initLog(File file) {
        verifyFileExists(file);
        LoggerFactory.getLogger(LogConfigurator.class).info("Loaded logback-config.xml from " + file.getAbsolutePath());

        installJavaUtilLoggingForSLF4J();
        // assume SLF4J is bound to logback in the current environment
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(lc);
            // the context was probably already configured by default configuration rules
            lc.reset();
            configurator.doConfigure(file);
        } catch (JoranException je) {
            handleConfigurationException(je, lc);
            throw new IllegalArgumentException("Could not configure logback for " + file); // NOSONAR
        }
        handleConfigurationException(lc);

        if (!isDevelopmentMode()) {
            // Turn off console logging in prod as it outputs lots of noise
            Appender<ILoggingEvent> appender = lc.getLogger(Logger.ROOT_LOGGER_NAME).getAppender("CONSOLE");
            appender.stop();
        }
    }

    protected void handleConfigurationException(LoggerContext lc) {
        StatusPrinter.printInCaseOfErrorsOrWarnings(lc);
    }

    protected void handleConfigurationException(@SuppressWarnings("unused") JoranException je, LoggerContext lc) {
        StatusPrinter.printInCaseOfErrorsOrWarnings(lc);
    }

    protected void installJavaUtilLoggingForSLF4J() {
        // java.util.logging needs a bit extra to be caught by SLF4J
        LogManager.getLogManager().reset();
        SLF4JBridgeHandler.install();
        java.util.logging.Logger.getLogger("global").setLevel(java.util.logging.Level.FINEST); // NOSONAR
    }

    private boolean isDevelopmentMode() {
        return getClass().getProtectionDomain().getCodeSource().getLocation().toExternalForm().contains("target/classes");
    }

    private static void verifyFileExists(File file) {
        if (!file.exists()) {
            throw new IllegalArgumentException("Could not find [" + file.getAbsolutePath() + "], Cannot initialize logging.");
        }
    }
}
