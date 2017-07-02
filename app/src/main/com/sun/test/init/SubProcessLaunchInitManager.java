package com.sun.test.init;

/**
 * 子进程的初始化操作
 * Created by peterzkli on 2016/12/19.
 */

public class SubProcessLaunchInitManager {

    public static void doSubProcessLaunchInit() {
        doSubProcessInitTask();
    }

    private static void doSubProcessInitTask() {
        LaunchInitManager.onFirstCreate();
        LaunchInitManager.onFirstResume();
        LaunchInitManager.onHomeResume();
        LaunchInitManager.onHomeIdle();
    }

}
