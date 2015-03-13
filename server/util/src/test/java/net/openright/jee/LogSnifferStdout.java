package net.openright.jee;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Catches calls to stdout/stderr and throws exception if called. Used to verify that people
 * aren't unecessarily outputting stuff to stderr/stdout during unit tests
 */
public abstract class LogSnifferStdout extends PrintStream {

    private static final PrintStream realStdout = System.out; // NOSONAR
    private static final PrintStream realStderr = System.err; // NOSONAR

    private PrintStream underlyingPrintStream;
    private MyWrappedOutputStream wrapper;

    public interface FlushHandler {
        void flush(ByteArrayOutputStream stream);
    }

    static class MyWrappedOutputStream extends PrintStream {
        boolean inFlush = false;
        private ByteArrayOutputStream outputStream;
        private FlushHandler flushHandler;

        MyWrappedOutputStream(FlushHandler flushHandler) {
            super(new ByteArrayOutputStream(), true);
            this.flushHandler = flushHandler;
            outputStream = (ByteArrayOutputStream) super.out;
        }

        @Override
        public synchronized void flush() {
            super.flush();
            if (outputStream.size() > 0) {
                try {
                    flushHandler.flush(outputStream);
                } finally {
                    outputStream.reset();
                }
            }
        }
    }

    private LogSnifferStdout(FlushHandler flushHandler, PrintStream stream) {
        super(new MyWrappedOutputStream(flushHandler), false);
        this.wrapper = (MyWrappedOutputStream) super.out;
        this.underlyingPrintStream = stream;
    }

    protected abstract void reset();

    public synchronized static Closeable init() {
        class StdFlushHandler implements FlushHandler {
            private PrintStream originalStream;

            StdFlushHandler(PrintStream originalStream) {
                this.originalStream = originalStream;
            }

            @Override
            public void flush(ByteArrayOutputStream outputStream) {
                try {
                    outputStream.writeTo(originalStream);
                    originalStream.flush();
                    throw new IllegalStateException("Ikke lov med print til stdout/stderr i test!");
                } catch (IOException e) {
                    throw new IllegalStateException("Ikke lov med print til stdout/stderr i test!", e);
                }

            }
        }

        return init(new StdFlushHandler(System.out), new StdFlushHandler(System.err));
    }

    public static Closeable init(FlushHandler outFlushHandler, FlushHandler errFlushHandler) {
        final LogSnifferStdout outWrapper = new LogSnifferStdout(outFlushHandler, System.out) {
            @Override
            protected void reset() {
                System.setOut(super.underlyingPrintStream);
            }
        };
        final LogSnifferStdout errWrapper = new LogSnifferStdout(errFlushHandler, System.err) {
            @Override
            protected void reset() {
                System.setErr(super.underlyingPrintStream);
            }
        };

        System.setOut(outWrapper);
        System.setErr(errWrapper);

        return new Closeable() {
            @Override
            public void close() throws IOException {
                outWrapper.reset();
                errWrapper.reset();
            }
        };

    }

    /**
     * Get the real stdout
     */
    public static PrintStream getRealStdout() {
        return realStdout;
    }

    /**
     * Get the real stderr
     */
    public static PrintStream getRealStderr() {
        return realStderr;
    }

    public synchronized static void clearStreams() {
        if (System.out instanceof LogSnifferStdout) {
            ((LogSnifferStdout) System.out).clear();
        }
        if (System.err instanceof LogSnifferStdout) {
            ((LogSnifferStdout) System.err).clear();
        }
    }

    void clear() {
        wrapper.outputStream.reset();
    }

    public synchronized static void flushStreams() {
        System.out.flush();
        System.err.flush();
    }
}
