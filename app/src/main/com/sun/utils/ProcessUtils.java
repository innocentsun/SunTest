package com.sun.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.text.TextUtils;

import com.sun.base.SunApplication;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;

/**
 * Created by sunhzchen on 2017/1/4.
 * 进程相关工具类
 */

public class ProcessUtils {

    public static String getProcessName() {
        String processName = "";
        BufferedReader mBufferedReader = null;
        try {
            File file = new File("/proc/" + android.os.Process.myPid() + "/" + "cmdline");
            mBufferedReader = new BufferedReader(new FileReader(file));
            String line = mBufferedReader.readLine();
            if (!TextUtils.isEmpty(line)) {
                processName = line.trim();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            if (mBufferedReader != null) {
                try {
                    mBufferedReader.close();
                } catch (Exception ignored) {
                }
            }
        }
        if (isValidProcessName(processName)) {
            return processName;
        } else {
            return getProcessNameBySystemService();
        }
    }

    private static boolean isValidProcessName(String processName) {
        return !TextUtils.isEmpty(processName) && processName.startsWith(SunApplication
                .getAppContext().getPackageName());
    }

    private static String getProcessNameBySystemService() {
        String processName = "";
        int pid = android.os.Process.myPid();
        ActivityManager mActivityManager = (ActivityManager) SunApplication.getAppContext()
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processList = mActivityManager
                .getRunningAppProcesses();
        if (processList != null) {
            for (ActivityManager.RunningAppProcessInfo appProcess : processList) {
                if (appProcess.pid == pid) {
                    processName = appProcess.processName;
                    break;
                }
            }
        }
        return processName;
    }
}
