package com.sun.test.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.sun.test.R;
import com.sun.utils.Utils;
import com.sun.views.ScratchView;

/**
 * Created by sunhzchen on 2017/1/4.
 * 刮奖效果
 */

public class ScratchViewActivity extends CommonActivity implements ScratchView
        .OnEraseStatusChangedListener {

    private TextView mTextView;
    private ScratchView mScratchView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scratch_view);
        initView();
    }

    private void initView() {
        mTextView = (TextView) findViewById(R.id.tv_scratch_view);
        mTextView.setText(Utils.getString(R.string.app_name));
        mScratchView = (ScratchView) findViewById(R.id.scratch_scratch_view);
        mScratchView.setEraseStatusChangedListener(this);
        findViewById(R.id.btn_reset_scratch_view).setOnClickListener(v -> mScratchView.reset());
        findViewById(R.id.btn_clear_scratch_view).setOnClickListener(v -> mScratchView.clear());
    }

    @Override
    public void onProgress(int percent) {
        mTextView.setText(Utils.getString(R.string.current_percent, percent));
    }

    @Override
    public void onCompleted(View view) {
        Toast.makeText(this, Utils.getString(R.string.three_dogs), Toast.LENGTH_SHORT).show();
    }
}
