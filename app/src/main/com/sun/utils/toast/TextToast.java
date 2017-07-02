package com.sun.utils.toast;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.sun.test.R;

/**
 * Created by fredliao on 2016/4/20.
 */
class TextToast extends Toast {
    private TextView mTextView;

    TextToast(Context context) {
        super(context);
        View view = LayoutInflater.from(context).inflate(R.layout.layout_toast_text, null);
        mTextView = (TextView) view.findViewById(R.id.textview);
        setView(view);
    }

    @Override
    public void setText(CharSequence s) {
        // 重载下这个方法，不让父类出错，因为父类的setText方法是和makeText配套的
        mTextView.setText(s);
    }
}
