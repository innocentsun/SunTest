package com.sun.test.init.task;

import android.content.Context;
import android.content.Intent;

import com.sun.base.SunApplication;
import com.sun.test.R;
import com.sun.test.activity.MainActivity;
import com.sun.test.init.InitTask;
import com.sun.utils.AppUtils;

/**
 * Created by sunhzchen on 2017/1/10.
 * 图标初始化
 */

public class CreateShortcutInitTask extends InitTask {

    private static final String DUPLICATE = "duplicate";
    private static final String ACTION_INSTALL_SHORTCUT = "com.android.launcher.action" + "" +
            ".INSTALL_SHORTCUT";
    private static final String ACTION_UNINSTALL_SHORTCUT = "com.android.launcher.action" + "" +
            ".UNINSTALL_SHORTCUT";

    public CreateShortcutInitTask(int threadStrategy, int triggerEvent) {
        super(threadStrategy, triggerEvent);
    }

    @Override
    protected void execute() {
        // 检查是否是第一次启动，并检查是否有快捷方式，没有则创建
        createShortcut(SunApplication.getAppContext());
    }

    private void createShortcut(final Context context) {
        //只有在第一次的时候才执行，否则return
        if (!AppUtils.isFirstLaunch(context)) {
            return;
        }
        AppUtils.setFirstLaunchTime(context);
        //先删除，然后再延迟创建
        createOrDeleteShortcut(context, false);
        SunApplication.postDelayed(new Runnable() {
            @Override
            public void run() {
                createOrDeleteShortcut(context, true);
                AppUtils.setFirstLaunch(context);
            }
        }, 100);
    }

    private static void createOrDeleteShortcut(Context context, boolean isInstall) {

        Intent shortcutIntent;
        shortcutIntent = new Intent(Intent.ACTION_MAIN);
        shortcutIntent.setClassName(context, MainActivity.class.getName());
        shortcutIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        Intent installIntent = new Intent();
        installIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        installIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, context.getResources().getString(R
                .string.app_name));
        installIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource
                .fromContext(context, R.mipmap.ic_launcher));
        installIntent.putExtra(DUPLICATE, false);

        if (isInstall) {
            shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            installIntent.setAction(ACTION_INSTALL_SHORTCUT);
        } else {
            shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            installIntent.setAction(ACTION_UNINSTALL_SHORTCUT);
        }
        context.sendOrderedBroadcast(installIntent, null);
    }
}
