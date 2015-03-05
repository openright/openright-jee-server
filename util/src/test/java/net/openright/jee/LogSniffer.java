package net.openright.jee;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.LogManager;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import net.openright.jee.LogSnifferStdout.FlushHandler;

import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.bridge.SLF4JBridgeHandler;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;

public class LogSniffer implements TestRule {

    private final Map<ILoggingEvent, Boolean> logbackAppender = new LinkedHashMap<>();
    private final Level minimumLevel;
    private final boolean denyOthersWhenMatched;

    static {
        initLogSystem();
    }

    public static void initLogSystem() {
        // java.util.logging krever litt ekstra for at SLF4J skal kunne fange det opp
        LogManager.getLogManager().reset();
        SLF4JBridgeHandler.install();
        java.util.logging.Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME).setLevel(java.util.logging.Level.FINEST);
    }

    public LogSniffer() {
        this(Level.INFO);
    }

    public LogSniffer(Level minimumLevel) {
        this(minimumLevel, true);
    }

    public LogSniffer(Level minimumLevel, boolean denyOthersWhenMatched) {
        this.minimumLevel = minimumLevel;
        this.denyOthersWhenMatched = denyOthersWhenMatched;
        installTurboFilter(getLoggerContext());
    }

    private LoggerContext getLoggerContext() {
        return (LoggerContext) LoggerFactory.getILoggerFactory();
    }

    private void installTurboFilter(LoggerContext lc) {
        lc.addTurboFilter(new TurboFilter() {

            @Override
            public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] argArray, Throwable t) {

                // TODO: (FC) fix annet sted
                // unødvendig log output fra hibernate
                if (format != null && format.contains("HHH000387")) {
                    return FilterReply.NEUTRAL;
                }

                // TODO (TLE) fix et annet sted, undertrykker output fra Jetty (WebServerSimulator bruker static server
                // så tilfeldig hvilken test som får feilmeldingene)
                if (format != null && (format.contains("Empty contextPath") || format.contains(" contextPath ends with /"))) {
                    return FilterReply.NEUTRAL;
                }

                if (format != null && level != null && level.isGreaterOrEqual(minimumLevel)) {
                    LoggingEvent loggingEvent = new LoggingEvent(Logger.FQCN, logger, level, format, t, argArray);
                    logbackAppender.put(loggingEvent, Boolean.FALSE);
                    return denyOthersWhenMatched ? FilterReply.DENY : FilterReply.NEUTRAL;
                }
                return FilterReply.NEUTRAL;
            }
        });
    }

    private int countLogbackEntries(String substring, Class<? extends Throwable> t, Level level) {
        int count = 0;
        for (ILoggingEvent loggingEvent : logbackAppender.keySet()) {
            if (eventMatches(loggingEvent, substring, t, level)) {
                count++;
            }
        }
        return count;
    }

    private void markEntryAsserted(String substring, Class<? extends Throwable> t, Level level) {
        for (ILoggingEvent loggingEvent : new ArrayList<>(logbackAppender.keySet())) {
            if (eventMatches(loggingEvent, substring, t, level)) {
                logbackAppender.put(loggingEvent, Boolean.TRUE);
            }
        }
    }

    private boolean eventMatches(ILoggingEvent loggingEvent, String substring, Class<? extends Throwable> t, Level level) {
        if (substring != null && !loggingEvent.getFormattedMessage().contains(substring)) {
            return false;
        }
        if (t != null
                && (loggingEvent.getThrowableProxy() == null
                        || loggingEvent.getThrowableProxy().getClassName() == null
                        || !loggingEvent.getThrowableProxy().getClassName().equals(t.getName()))) {
            return false;
        }
        return level == null || (level == loggingEvent.getLevel());
    }

    public void clearLog() {
        flushStdPrintStream();
        this.logbackAppender.clear();
    }

    private boolean hasLogEntry(String substring, Class<? extends Throwable> t, Level level) {
        return countLogbackEntries(substring, t, level) > 0;
    }

    public int countEntries(String substring) {
        flushStdPrintStream();
        return countLogbackEntries(substring, null, null);
    }

    @Override
    public Statement apply(final Statement base, final Description description) {

        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try (Closeable ignored = LogSnifferStdout.init(
                        new StdErrOutFlushToLogbackFlushHandler(Level.WARN),
                        new StdErrOutFlushToLogbackFlushHandler(Level.ERROR))) {

                    base.evaluate();

                    LogSnifferStdout.flushStreams();
                    assertNoErrorsOrWarnings();
                } finally {
                    getLoggerContext().resetTurboFilterList();
                }
            }
        };
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (Entry<ILoggingEvent, Boolean> entry : logbackAppender.entrySet()) {
            ILoggingEvent event = entry.getKey();
            Boolean verified = entry.getValue();
            if (!verified) {
                buf.append(event.getLevel() + ":" + event.getFormattedMessage()).append('\n');
            }
        }
        return buf.toString();
    }

    private void flushStdPrintStream() {
        LogSnifferStdout.flushStreams();
    }

    public int countErrors() {
        int logErrors = 0;
        for (Map.Entry<ILoggingEvent, Boolean> entry : logbackAppender.entrySet()) {
            if (entry.getKey().getLevel().equals(Level.ERROR) && entry.getValue() != Boolean.TRUE) {
                logErrors++;
            }
        }
        return logErrors;
    }

    private int countWarnings() {
        int logWarnings = 0;
        for (Map.Entry<ILoggingEvent, Boolean> entry : logbackAppender.entrySet()) {
            if (entry.getKey().getLevel().equals(Level.WARN) && entry.getValue() != Boolean.TRUE) {
                logWarnings++;
            }
        }
        return logWarnings;
    }

    public void assertNoErrorsOrWarnings() {
        flushStdPrintStream();
        assertNoErrors();
        assertNoWarnings();
    }

    public void assertNoLogEntries() {
        flushStdPrintStream();
        if (!logbackAppender.isEmpty()) {
            throw new AssertionError("Skulle ikke hatt noe i loggen, var " + this);
        }
    }

    public void assertNoErrors() {
        flushStdPrintStream();
        if (countErrors() > 0) {
            throw new AssertionError("Skulle ikke hatt feil, følgende ERROR ikke verifisert: " + this);
        }
    }

    public void assertNoWarnings() {
        flushStdPrintStream();
        if (countWarnings() > 0) {
            throw new AssertionError("Skulle ikke hatt feil, følgende WARN ikke verifisert: " + this);
        }
    }

    public void assertHarErrorMelding(String substring, Class<? extends Throwable> t) {
        flushStdPrintStream();
        if (!hasLogEntry(substring, t, Level.ERROR)) {
            throw new AssertionError(String.format(
                    "Fant ikke loggmelding som matcher [%s], for exception [%s], med level ERROR.  Har [%s]", substring, t, this));
        }
        markEntryAsserted(substring, t, Level.ERROR);
    }

    public void assertHarWarnMelding(String regexp, Class<? extends Throwable> t) {
        flushStdPrintStream();
        if (!hasLogEntry(regexp, t, Level.WARN)) {
            throw new AssertionError(String.format("Fant ikke loggmelding som matcher [%s], for exception [%s], med level WARN.  Har [%s]",
                    regexp, t, this));
        }
        markEntryAsserted(regexp, t, Level.WARN);
    }

    public void assertHarErrorMelding(String substring) {
        flushStdPrintStream();
        assertHarErrorMelding(substring, null);
    }

    public void assertHarWarnMelding(String substring) {
        flushStdPrintStream();
        assertHarWarnMelding(substring, null);
    }

    public void assertHarInfoMelding(String regexp) {
        flushStdPrintStream();
        if (!hasLogEntry(regexp, null, Level.INFO)) {
            throw new AssertionError(String.format("Fant ikke loggmelding som matcher [%s], med level INFO.  Har uverifisert meldinger: [%s]", regexp, this));
        }
        markEntryAsserted(regexp, null, Level.INFO);
    }

    public void assertHarDebugMelding(String regexp) {
        flushStdPrintStream();
        if (!hasLogEntry(regexp, null, Level.DEBUG)) {
            throw new AssertionError(String.format(
                    "Fant ikke loggmelding som matcher [%s], med level DEBUG. Har [%s]", regexp, this));
        }
        markEntryAsserted(regexp, null, Level.DEBUG);
    }

    class StdErrOutFlushToLogbackFlushHandler implements FlushHandler {

        private final Logger logger = getLoggerContext().getLogger(LogSniffer.class);
        private Level level;

        public StdErrOutFlushToLogbackFlushHandler(Level level) {
            this.level = level;
        }

        @Override
        public void flush(ByteArrayOutputStream stream) {
            String string = stream.toString();
            if (level == Level.WARN) {
                logger.warn(string);
            } else if (level == Level.ERROR) {
                logger.error(string);
            }
        }
    }
}
