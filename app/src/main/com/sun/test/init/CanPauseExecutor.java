package com.sun.test.init;

import com.sun.base.SunApplication;
import com.sun.utils.Utils;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CanPauseExecutor implements ICanPause {

    public static final int SPLIT_DELAY_TIME = 200;
    private static ThreadPoolExecutor sExecutor = new ThreadPoolExecutor(1, 5,
            2L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(1000),
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(null, r, "init-thread", 1024 * 512);
                }
            });

    public CanPauseExecutor() {
        sExecutor.allowCoreThreadTimeOut(true);
    }

    @Override
    public void pause() {
        // 暂停功能在子线程实现的不好。先预留接口
    }

    @Override
    public void resume() {
    }

    public void postTask(ArrayList<InitTask> taskList) {
        if (Utils.isEmpty(taskList)) {
            return;
        }
        for (InitTask task : taskList) {
            sExecutor.execute(task);
        }
    }

    public void postTaskSplit(ArrayList<InitTask> taskList, int splitUnit) {
        if (Utils.isEmpty(taskList)) {
            return;
        }
        for (int i = 0; i < taskList.size(); i++) {
            final InitTask curTask = taskList.get(i);
            SunApplication.postDelayed(new Runnable() {
                @Override
                public void run() {
                    sExecutor.execute(curTask);
                }
            }, i / splitUnit * SPLIT_DELAY_TIME);
        }
    }
}
