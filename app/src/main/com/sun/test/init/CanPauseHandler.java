package com.sun.test.init;

import android.os.Handler;
import android.os.Message;

import com.sun.utils.Utils;

import java.util.ArrayList;

public class CanPauseHandler extends Handler implements ICanPause {
    private static final int MESSAGE_EXECUTE_TASK = 5001;
    private final ArrayList<InitTask> mMainInitTaskQueue = new ArrayList<>();
    private boolean mIsPause = false;

    @Override
    public void pause() {
        mIsPause = true;
    }

    @Override
    public void resume() {
        mIsPause = false;
        executeNext();
    }

    public void executeNext() {
        sendEmptyMessage(MESSAGE_EXECUTE_TASK);
    }

    public void postTask(ArrayList<InitTask> taskList) {
        if (Utils.isEmpty(taskList)) {
            return;
        }
        synchronized (mMainInitTaskQueue) {
            mMainInitTaskQueue.addAll(taskList);
            executeNext();
        }
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg.what != MESSAGE_EXECUTE_TASK) {
            super.handleMessage(msg);
        } else {
            if (mIsPause) {
                return;
            }
            synchronized (mMainInitTaskQueue) {
                if (Utils.isEmpty(mMainInitTaskQueue)) {
                    return;
                }
                InitTask task = mMainInitTaskQueue.remove(0);
                task.run();
                executeNext();
            }
        }
    }
}
