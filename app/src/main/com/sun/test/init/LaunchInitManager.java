package com.sun.test.init;


import android.app.Application;
import android.util.SparseArray;

import com.sun.base.ActivityListManager;
import com.sun.base.SunApplication;
import com.sun.logger.SLog;
import com.sun.test.init.task.CreateShortcutInitTask;
import com.sun.utils.Utils;

import java.util.ArrayList;

/**
 * 管理app初始化的类
 */
public class LaunchInitManager {

    private static final String TAG = "LaunchInitManager";
    public static final int TRIGGER_EVENT_APP_CREATE = 1;
    private static final int TRIGGER_EVENT_FIRST_CREATE = 2;
    public static final int TRIGGER_EVENT_FIRST_RESUME = 3;
    private static final int TRIGGER_EVENT_HOME_RESUME = 4;
    private static final int TRIGGER_EVENT_HOME_IDLE = 5;

    private static final int MAIN_THREAD_TASK = 0;
    public static final int SUB_THREAD_TASK = 1;

    private static final int FORCE_INIT_DELAY_MILLIS = 10000;
    private static final int TASK_LIST_SPLIT_UNIT = 2;

    private static boolean sIsFirstCreateExe = false;
    private static boolean sIsFirstResumeExe = false;
    private static boolean sIsHomeResumeExe = false;
    public static boolean sIsHomeIdleExe = false;

    private static boolean isExited = false;        // 标示app是否反初始化完毕
    private static CanPauseHandler sCanPauseHandler = new CanPauseHandler();
    private static CanPauseExecutor sCanPauseExecutor = new CanPauseExecutor();

    private static SparseArray<SparseArray<ArrayList<InitTask>>> sPostTaskSparseArray = new SparseArray<>();
    private static SparseArray<SparseArray<ArrayList<InitTask>>> sInstantTaskSparseArray = new SparseArray<>();
    static {
        addPostInitTasks();
        addInstantInitTasks();
    }

    private static void addPostInitTasks() {
        // App Create

        // First Resume

        // Home Create

        // Home Resume

        // Home Idle
        addPostInitTask(new CreateShortcutInitTask(SUB_THREAD_TASK, TRIGGER_EVENT_HOME_IDLE));
    }

    private static void addInstantInitTasks() {
        // App Create

        // First Resume

        // Home Create

        // Home Resume

        // Home Idle

    }

    private static void addPostInitTask(InitTask task) {
        addInitTask(task, sPostTaskSparseArray);
    }

    private static void addInstantInitTask(InitTask task) {
        addInitTask(task, sInstantTaskSparseArray);
    }

    private static void addInitTask(InitTask task, SparseArray<SparseArray<ArrayList<InitTask>>> sparseArray) {
        if (!task.isProcessMatch()) {
            return;
        }
        SparseArray<ArrayList<InitTask>> taskArray = sparseArray.get(task.mTriggerEvent);
        if (taskArray == null) {
            taskArray = new SparseArray<>();
            sparseArray.put(task.mTriggerEvent, taskArray);
        }
        ArrayList<InitTask> taskList = taskArray.get(task.mThreadStrategy);
        if (taskList == null) {
            taskList = new ArrayList<>();
            taskArray.put(task.mThreadStrategy, taskList);
        }
        taskList.add(task);
    }

    public static void onApplicationCreate(Application application, boolean isMainProc) {
        onAppInit(application, isMainProc);
        executeTask(TRIGGER_EVENT_APP_CREATE);
        if(!isMainProc) {
            SubProcessLaunchInitManager.doSubProcessLaunchInit();
        }
    }

    public static void onFirstCreate() {
        if (sIsFirstCreateExe) {
            return;
        }
        synchronized (LaunchInitManager.class) {
            if (sIsFirstCreateExe) {
                return;
            }
            sIsFirstCreateExe = true;
        }
        executeTask(TRIGGER_EVENT_FIRST_CREATE);
    }

    public static void onFirstResume() {
        if (sIsFirstResumeExe) {
            return;
        }
        synchronized (LaunchInitManager.class) {
            if (sIsFirstResumeExe) {
                return;
            }
            sIsFirstResumeExe = true;
        }
        SunApplication.postDelayed(new Runnable() {
            @Override
            public void run() {
                //如果主进程被强制杀掉，系统会主动拉起最顶层的Activity，而不是HomeActivity。
                //这种情况，只能靠延时触发了。
                onHomeResume();
                onHomeIdle();
            }
        }, FORCE_INIT_DELAY_MILLIS);
        executeTask(TRIGGER_EVENT_FIRST_RESUME);
    }

    public static void onHomeResume() {
        if (sIsHomeResumeExe) {
            return;
        }
        synchronized (LaunchInitManager.class) {
            if (sIsHomeResumeExe) {
                return;
            }
            sIsHomeResumeExe = true;
        }
        resumeInit();
        executeTask(TRIGGER_EVENT_HOME_RESUME);
    }

    public static void onHomeIdle() {
        if (sIsHomeIdleExe) {
            return;
        }
        synchronized (LaunchInitManager.class) {
            if (sIsHomeIdleExe) {
                return;
            }
            sIsHomeIdleExe = true;
        }
        executeInstantTask(TRIGGER_EVENT_HOME_IDLE);
        executeHomeIdlePostTask();
    }

    private static void executeHomeIdlePostTask() {
        SparseArray<ArrayList<InitTask>> taskArray = sPostTaskSparseArray.get(TRIGGER_EVENT_HOME_IDLE);
        if (Utils.isEmpty(taskArray)) {
            return;
        }
        sCanPauseHandler.postTask(taskArray.get(MAIN_THREAD_TASK));
        sCanPauseExecutor.postTaskSplit(taskArray.get(SUB_THREAD_TASK), TASK_LIST_SPLIT_UNIT);
    }

    private static void executeTask(int triggerEvent) {
        executeInstantTask(triggerEvent);
        executePostTask(triggerEvent);
    }

    private static void executeInstantTask(int triggerEvent) {
        SparseArray<ArrayList<InitTask>> taskArray = sInstantTaskSparseArray.get(triggerEvent);
        if (Utils.isEmpty(taskArray)) {
            return;
        }
        ArrayList<InitTask> taskList = taskArray.get(MAIN_THREAD_TASK);
        if (Utils.isEmpty(taskList)) {
            return;
        }
        for (InitTask task : taskList) {
            task.run();
        }
    }

    private static void executePostTask(int triggerEvent) {
        SparseArray<ArrayList<InitTask>> taskArray = sPostTaskSparseArray.get(triggerEvent);
        if (Utils.isEmpty(taskArray)) {
            return;
        }
        sCanPauseHandler.postTask(taskArray.get(MAIN_THREAD_TASK));
        sCanPauseExecutor.postTask(taskArray.get(SUB_THREAD_TASK));
    }

    /**
     * 这些是一定要在application里面初始化的。
     */
    private static void onAppInit(final Application application, final boolean isMainProcess) {

    }

    public static void pauseInit() {
        sCanPauseHandler.pause();
        sCanPauseExecutor.pause();
    }

    public static void resumeInit() {
        sCanPauseHandler.resume();
        sCanPauseExecutor.resume();
    }

    /**
     * 当应用程序退出时执行的反初始化操作
     */
    public static void onAppExit() {
        SLog.e(TAG, "-----------onAppExit---------");
        synchronized (LaunchInitManager.class) {
            if (isExited) {
                return;
            }
            isExited = true;
        }
        SLog.finish();
        ActivityListManager.releaseAllActivity();
        SunApplication.postDelayed(new Runnable() {
            @Override
            public void run() {
                System.exit(0);
            }
        }, 100);
    }

}
