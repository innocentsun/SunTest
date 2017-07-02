package com.sun.test.activity;

import android.os.Bundle;
import android.widget.TextView;

import com.sun.test.R;
import com.sun.utils.AppUtils;
import com.sun.utils.Utils;

/**
 * Created by sunhzchen on 2017/1/5.
 * 测试SP
 */

public class TestSharedPreferencesActivity extends CommonActivity {

    public static final String SP_SAVED_NUMBER = "sp_saved_number";

    private int mSavedNumber;
    private TextView mTextView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shared_preferences);
        initView();
    }

    private void initView() {
        mTextView = (TextView) findViewById(R.id.tv_display);
        mSavedNumber = AppUtils.getValueFromPreferences(SP_SAVED_NUMBER, 0);
        mTextView.setText(Utils.getString(R.string.sp_saved_number, mSavedNumber));
        findViewById(R.id.btn_add).setOnClickListener(v -> mTextView.setText(Utils.getString(R
                .string.sp_saved_number, ++mSavedNumber)));
        findViewById(R.id.btn_remove).setOnClickListener(v -> mTextView.setText(Utils.getString(R
                .string.sp_saved_number, --mSavedNumber)));
    }

    @Override
    public void finish() {
        AppUtils.setValueToPreferences(SP_SAVED_NUMBER, mSavedNumber);
        super.finish();
    }
}
