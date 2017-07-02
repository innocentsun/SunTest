package com.sun.utils.toast;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.sun.test.R;

/**
 * Created by fredliao on 2016/4/20.
 */
class ImageTextToast extends Toast {
    private TextView mTextView;
    private ImageView mImageView;

    ImageTextToast(Context context) {
        super(context);
        View view = LayoutInflater.from(context).inflate(R.layout.layout_toast_image_text, null);
        mTextView = (TextView) view.findViewById(R.id.textview);
        mImageView = (ImageView) view.findViewById(R.id.imageview);
        setView(view);
    }

    @Override
    public void setText(CharSequence s) {
        // 重载下这个方法，不让父类出错，因为父类的setText方法是和makeText配套的
        mTextView.setText(s);
    }

    public void setImage(Drawable drawable) {
        mImageView.setImageDrawable(drawable);
    }

    public void setImage(int resId) {
        mImageView.setImageResource(resId);
    }
}
