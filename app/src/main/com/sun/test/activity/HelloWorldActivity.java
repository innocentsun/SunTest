package com.sun.test.activity;

import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.sun.logger.SLog;
import com.sun.test.R;

/**
 * Created by sunhzchen on 2017/1/4.
 * hello world展示
 */

public class HelloWorldActivity extends CommonActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hello_world);

        initView();
    }

    private void initView() {
        TextView textView = (TextView) findViewById(R.id.textview);
        Button button = (Button) findViewById(R.id.btn_jump);
        new Thread(() -> {
            SLog.e(this, "Thread");
        }).start();

        ((Runnable) () -> {
            SLog.e("TAG", "runnable");
        }).run();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        SLog.e("TAG", "onConfigurationChanged : orientation = " + getResources().getConfiguration
                ().orientation + ";isLandscape = " + (getResources().getConfiguration()
                .orientation == Configuration.ORIENTATION_LANDSCAPE) + ";isPortrait = " +
                (getResources().getConfiguration().orientation == Configuration
                        .ORIENTATION_PORTRAIT));
    }
}
