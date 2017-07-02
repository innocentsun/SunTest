package com.sun.base;

import android.app.Application;
import android.content.Context;
import android.os.Handler;

import com.sun.test.init.LaunchInitManager;
import com.sun.utils.ProcessUtils;

/**
 * Created by sunhzchen on 2017/1/4.
 * 全局Application
 */

public class SunApplication extends Application {

    private static SunApplication sContext;
    private boolean mIsMainProcess;
    private String mProcessName;
    public static Handler sHandler = new Handler();

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        sContext = this;
        mProcessName = ProcessUtils.getProcessName();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mIsMainProcess = mProcessName.equals(getPackageName());
        //所有APP的初始化操作写在这个类的这个方法里面
        LaunchInitManager.onApplicationCreate(this, mIsMainProcess);
    }

    @Override
    public void onTerminate() {
        //所有APP的反初始化操作写在这个类的这个方法里面
        LaunchInitManager.onAppExit();
        super.onTerminate();
    }

    public static SunApplication getAppContext() {
        return sContext;
    }

    public boolean isMainProcess() {
        return mIsMainProcess;
    }

    public String getProcessName() {
        return mProcessName;
    }

    public static void post(Runnable runnable) {
        sHandler.post(runnable);
    }

    public static void postDelayed(Runnable runnable, long delayMillis) {
        sHandler.postDelayed(runnable, delayMillis);
    }

    public static void removeCallbacks(Runnable runnable) {
        sHandler.removeCallbacks(runnable);
    }
}
