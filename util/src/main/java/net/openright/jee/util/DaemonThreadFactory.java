package net.openright.jee.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class DaemonThreadFactory implements ThreadFactory {

    private AtomicInteger counter = new AtomicInteger();
    private final String threadNamePrefix;

    public static ThreadFactory createThreadFactory(String threadNamePrefix){
        return new DaemonThreadFactory(threadNamePrefix);
    }

    private DaemonThreadFactory(String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, threadNamePrefix + "-" + counter.incrementAndGet());
        t.setDaemon(true);
        return t;
    }

}
