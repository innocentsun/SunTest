package com.sun.test.activity;

import android.os.Bundle;

import com.sun.base.BaseActivity;
import com.sun.test.R;

/**
 * Created by sunhzchen on 2017/1/4.
 * 所有业务相关Activity都继承于此
 */

public class CommonActivity extends BaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enterAnimation();
    }

    @Override
    public void finish() {
        super.finish();
        exitAnimation();
    }

    protected void enterAnimation() {
        overridePendingTransition(R.anim.push_left_in, 0);
    }

    protected void exitAnimation() {
        overridePendingTransition(0, R.anim.push_right_out);
    }
}
