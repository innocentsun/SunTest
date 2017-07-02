package com.sun.test.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.sun.test.R;
import com.sun.views.ScrollTextView;

import java.util.ArrayList;

/**
 * Created by sunhzchen on 2017/1/4.
 * 轮播滚动测试
 */

public class ONAEnterTipsActivity extends CommonActivity implements View.OnClickListener {

    private ScrollTextView mTextView;
    private ArrayList<String> mStringList;
    private boolean mIsStopped = false;
    private int mIndex = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ona_enter_tips);
        initData();
        init();
    }

    private void initData() {
        mStringList = new ArrayList<>();
        mStringList.add(getString(R.string.scroll_text_one));
        mStringList.add(getString(R.string.scroll_text_two));
        mStringList.add(getString(R.string.scroll_text_three));
    }

    private void init() {
        mTextView = (ScrollTextView) findViewById(R.id.tv_tips);
        mTextView.setText(mStringList);
        mTextView.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Toast.makeText(this, mStringList.get(mTextView.getCurrentIndex()), Toast.LENGTH_SHORT).show();
        mIsStopped = !mIsStopped;
    }
}
