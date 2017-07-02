package com.sun.base;

import android.os.Handler;
import android.os.HandlerThread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 业务线程管理类，将所有的业务线程使用该类管理，实现线程的复用。并为以后统一优化提供先决条件
 *
 * @author connorlu
 */
public class ThreadManager {
    //Java Thread的后台线程优先级, 映射到 android.os.Process.THREAD_PRIORITY_BACKGROUND. 映射关系参考 kNiceValues at vm/Thread.c
    public static final int THREAD_BACKGROUND_PRIORITY = Thread.NORM_PRIORITY - 1;
    public static final String GLOABL_HANDLER_THREAD = "global_handler_thread";

    public static final ExecutorService HTTP_Executor = Executors.newFixedThreadPool(10, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(null, r, "Http-Thread-" + httpCounter.getAndIncrement(), 1024 * 64);
            thread.setPriority(THREAD_BACKGROUND_PRIORITY);
            return thread;
        }
    });

    private final static AtomicInteger httpCounter = new AtomicInteger(1);

    private static ThreadManager instance;
    private static ThreadPoolExecutor executor;
    private static AtomicInteger counter;

    private static HandlerThread handlerThread;
    private static Handler globalThreadHandler;

    private static final int CORE_THREADS = 4;

    private ThreadManager() {
        counter = new AtomicInteger(1);
        executor = new ThreadPoolExecutor(CORE_THREADS, Integer.MAX_VALUE,
                2L, TimeUnit.SECONDS,
//				new LinkedBlockingQueue<Runnable>(),
                new SynchronousQueue<Runnable>(),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(null, r, "thread-manager-" + counter.getAndIncrement(), 1024 * 64);
                        thread.setPriority(THREAD_BACKGROUND_PRIORITY);
                        return thread;
                    }
                });
    }

    /**
     * 返回线程池实例
     *
     * @return
     */
    public static ThreadManager getInstance() {
        if (instance == null) {
            synchronized (ThreadManager.class) {
                if (instance == null) {
                    instance = new ThreadManager();
                }
            }
        }
        return instance;
    }

    /**
     * 在后台线程中执行一个Runnable
     *
     * @param command
     */
    public void execute(Runnable command) {
//		//动态调整线程池的核心个数，以让任务被尽快安排
//		int activeCount = executor.getActiveCount();
//		if (activeCount < MAX_THREADS) {
//			executor.setCorePoolSize(activeCount + 1);
//		}
        try {
            executor.execute(command);
        } catch (OutOfMemoryError e) {
            System.gc();
        }
    }

    public ThreadPoolExecutor getExecutor() {
        return executor;
    }

    private void ensureHandlerCreated() {
        synchronized (ThreadManager.class) {
            if (globalThreadHandler == null) {
                handlerThread = new HandlerThread(GLOABL_HANDLER_THREAD);
                handlerThread.start();
                globalThreadHandler = new Handler(handlerThread.getLooper());
            }
        }
    }

    public void post(Runnable runnable) {
        ensureHandlerCreated();
        globalThreadHandler.post(runnable);
    }

    public void postDelayed(Runnable runnable, long delayMillis) {
        ensureHandlerCreated();
        globalThreadHandler.postDelayed(runnable, delayMillis);
    }
}
