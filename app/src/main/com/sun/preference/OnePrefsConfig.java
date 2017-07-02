package com.sun.preference;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class OnePrefsConfig {

    static long sUnitWaitTime = 50;
    static long sMainThreadWaitTime = 300;
    static long sNonMainThreadWaitTime = 1000;
    private static ExecutorService sExecutorService;
    private static final int THREAD_BACKGROUND_PRIORITY = Thread.NORM_PRIORITY - 1;
    private static final AtomicInteger HTTP_COUNTER = new AtomicInteger(1);

    public static void setUnitWaitTime(long unitWaitTime) {
        sUnitWaitTime = unitWaitTime;
    }

    public static void setMainThreadWaitTime(long mainThreadWaitTime) {
        sMainThreadWaitTime = mainThreadWaitTime;
    }

    public static void setNonMainThreadWaitTime(long nonMainThreadWaitTime) {
        sNonMainThreadWaitTime = nonMainThreadWaitTime;
    }

    public static void setExecutorService(ExecutorService executorService) {
        if (executorService == null) {
            throw new NullPointerException("ExecutorService should not be null!");
        }
        synchronized (OnePrefsConfig.class) {
            sExecutorService = executorService;
        }
    }

    static ExecutorService getConfigExecutorService() {
        synchronized (OnePrefsConfig.class) {
            if (sExecutorService == null) {
                sExecutorService = Executors.newFixedThreadPool(3, new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(null, r, "Pref-Thread-" + HTTP_COUNTER.getAndIncrement());
                        thread.setPriority(THREAD_BACKGROUND_PRIORITY);
                        return thread;
                    }
                });
            }
            return sExecutorService;
        }
    }
}
