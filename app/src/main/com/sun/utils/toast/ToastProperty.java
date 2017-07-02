package com.sun.utils.toast;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Toast;

import com.sun.base.SunApplication;
import com.sun.test.R;
import com.sun.utils.Constants;

/**
 * Created by fredliao on 2016/8/26.
 */
class ToastProperty {

    public final static int TYPE_TEXT = 1;
    public final static int TYPE_IMAGE = 2;
    public final static int TYPE_INDICATOR = 3;

    final static int DEFAULT_Y = SunApplication.getAppContext().getResources().getDimensionPixelSize(
            R.dimen.toast_y_offset);
    final static int DEFAULT_GRAVITY = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
    final static int DEFAULT_X = 0;
    final static Drawable DEFAULT_DRAWABLE = null;

    private int type;
    private CharSequence text = Constants.EMPTY;
    private int duration = Toast.LENGTH_SHORT;
    private int gravity = ToastProperty.DEFAULT_GRAVITY;
    private int xOffset = ToastProperty.DEFAULT_X;
    private int yOffset = ToastProperty.DEFAULT_Y;
    private Object extra = null;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public CharSequence getText() {
        return text;
    }

    public void setText(CharSequence text) {
        this.text = text;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getGravity() {
        return gravity;
    }

    public void setGravity(int gravity) {
        this.gravity = gravity;
    }

    public int getxOffset() {
        return xOffset;
    }

    public void setxOffset(int xOffset) {
        this.xOffset = xOffset;
    }

    public int getyOffset() {
        return yOffset;
    }

    public void setyOffset(int yOffset) {
        this.yOffset = yOffset;
    }

    public Object getExtra() {
        return extra;
    }

    public void setExtra(Object extra) {
        this.extra = extra;
    }

    @Override
    public boolean equals(Object input) {
        if (input instanceof ToastProperty) {
            if (input == this) {
                return true;
            }

            return ((ToastProperty) input).getType() == this.type
                    && TextUtils.equals(((ToastProperty) input).getText(), this.text)
                    && ((ToastProperty) input).getDuration() == this.duration
                    && ((ToastProperty) input).getGravity() == this.gravity
                    && ((ToastProperty) input).getxOffset() == this.xOffset
                    && ((ToastProperty) input).getyOffset() == this.yOffset
                    && isExtraEquals(((ToastProperty) input).getExtra(), this.extra);
        }
        return false;
    }

    private boolean isExtraEquals(Object first, Object second) {
        return first == null ? second == null : first.equals(second);
    }

    static class Builder {
        private ToastProperty toastProperty = new ToastProperty();

        ToastProperty build() {
            return toastProperty;
        }

        Builder setType(int type) {
            toastProperty.setType(type);
            return this;
        }

        Builder setText(CharSequence text) {
            toastProperty.setText(text);
            return this;
        }

        Builder setGravity(int gravity) {
            toastProperty.setGravity(gravity);
            return this;
        }

        Builder setDuration(int duration) {
            toastProperty.setDuration(duration);
            return this;
        }

        Builder setXOffset(int xOffset) {
            toastProperty.setxOffset(xOffset);
            return this;
        }

        Builder setYOffset(int yOffset) {
            toastProperty.setyOffset(yOffset);
            return this;
        }

        Builder setExtra(Object extra) {
            toastProperty.setExtra(extra);
            return this;
        }
    }

    static class ImageExtra {
        int drawableHashcode;
        Drawable drawable;

        @Override
        public boolean equals(Object input) {
            return input instanceof ImageExtra && (input == this || ((ImageExtra) input).drawableHashcode == this.drawableHashcode);
        }
    }

    static class IndicatorExtra {
        String imageUrl;
        int arrowX;

        @Override
        public boolean equals(Object input) {
            if (input instanceof IndicatorExtra) {
                if (input == this) {
                    return true;
                }

                return TextUtils.equals(((IndicatorExtra) input).imageUrl, this.imageUrl)
                        && ((IndicatorExtra) input).arrowX == this.arrowX;
            }
            return false;
        }
    }
}
