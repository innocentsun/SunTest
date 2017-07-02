package com.sun.utils.toast;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Toast;

import com.sun.base.SunApplication;
import com.sun.logger.SLog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * toast工具类
 * 1、显示时间长短只有两种选择：Toast.LENGTH_SHORT和Toast.LENGTH_LONG
 * 2、防止重复点击、重复显示
 * 3、工具类可以确保在ui线程显示
 * 4、支持传入字符串资源id
 * 5、解决连续两次点击的内容不同的时候toast会偏移的情况
 * <p/>
 * toast特性备忘：
 * 1、toast与activity无关，内部采用WindowManager实现
 * 2、多次show同一toast，会以新设的duration为准重新开始倒计时
 * 3、makeToast与Toast.setText是绑定使用的，若不用makeToast请也不要使用Toast.setText，否则会出现crash
 * 4、自定义toast采取new Toast的方式而不采用makeToast的方式，这时候记得重写setText，防止出现crash
 * 5、Toast的队列里面有最多只能有50条，包含系统toast
 * 6、若系统对该app的toast开关关闭，可以用反射打开？
 * 7、toast最好统一在ui线程的消息列表里面处理（此线程生命周期长），在非ui线程处理需要非ui线程有looper支持，但如果looper结束的话，容易引起toast永远不消失
 * 8、设置toast的宽度为全屏只需在gravity里面加上Gravity.FILL_HORIZONTAL
 *
 * @author fredliao
 */

public class CommonToast {
    private static final String TAG = "CommonToast";
    private static Handler mHandler = new Handler(Looper.getMainLooper());
    private static Toast mToast;
    private static ToastProperty mToastProperty;

    /**
     * 短时间显示toast
     *
     * @param text ，显示文本
     */
    public static void showToastShort(String text) {
        showToast(text, Toast.LENGTH_SHORT);
    }

    /**
     * 长时间显示toast
     *
     * @param text ，显示文本
     */
    public static void showToastLong(String text) {
        showToast(text, Toast.LENGTH_LONG);
    }

    /**
     * 长时间显示toast
     *
     * @param resId ，文本资源id
     */
    public static void showToastLong(int resId) {
        showToast(SunApplication.getAppContext().getResources().getString(resId), Toast
                .LENGTH_LONG);
    }

    /**
     * 短时间显示toast
     *
     * @param resId ，文本资源id
     */
    public static void showToastShort(int resId) {
        showToast(SunApplication.getAppContext().getResources().getString(resId), Toast
                .LENGTH_SHORT);
    }

    /**
     * 显示toast，防止重复显示
     *
     * @param text     ，文本内容
     * @param duration ，显示时间长短
     */
    private static void showToast(final CharSequence text, final int duration) {
        showToast(ToastProperty.TYPE_TEXT, text, duration, ToastProperty.DEFAULT_DRAWABLE,
                ToastProperty.DEFAULT_GRAVITY, ToastProperty.DEFAULT_X, ToastProperty.DEFAULT_Y);
    }

    public static void showToastShort(CharSequence text, int gravity, int x, int y) {
        showToast(ToastProperty.TYPE_TEXT, text, Toast.LENGTH_SHORT, ToastProperty
                .DEFAULT_DRAWABLE, gravity, x, y);
    }

    public static void showToastShort(int resId, int gravity, int x, int y) {
        showToast(ToastProperty.TYPE_TEXT, SunApplication.getAppContext().getString(resId), Toast
                .LENGTH_SHORT, ToastProperty.DEFAULT_DRAWABLE, gravity, x, y);
    }

    public static void showToastLong(CharSequence text, int gravity, int x, int y) {
        showToast(ToastProperty.TYPE_TEXT, text, Toast.LENGTH_LONG, ToastProperty
                .DEFAULT_DRAWABLE, gravity, x, y);
    }

    public static void showToastLong(int resId, int gravity, int x, int y) {
        showToast(ToastProperty.TYPE_TEXT, SunApplication.getAppContext().getString(resId), Toast
                .LENGTH_LONG, ToastProperty.DEFAULT_DRAWABLE, gravity, x, y);
    }

    public static void showToastShort(String text, Drawable drawable, int gravity, int xOffset,
                                      int yOffset) {
        showToast(ToastProperty.TYPE_IMAGE, text, Toast.LENGTH_SHORT, drawable, gravity, xOffset,
                yOffset);
    }

    public static void showToastShort(String text, Drawable drawable) {
        showToast(ToastProperty.TYPE_IMAGE, text, Toast.LENGTH_SHORT, drawable, Gravity
                .CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 0);
    }

    public static void showToastLong(String text, Drawable drawable) {
        showToast(ToastProperty.TYPE_IMAGE, text, Toast.LENGTH_LONG, drawable, Gravity
                .CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 0);
    }

    public static void showToastLong(String text, Drawable drawable, int gravity, int xOffset,
                                     int yOffset) {
        showToast(ToastProperty.TYPE_IMAGE, text, Toast.LENGTH_LONG, drawable, gravity, xOffset,
                yOffset);
    }

    private static void showToast(final int type, final CharSequence text, final int duration,
                                  final Drawable drawable, final int gravity, final int xOffset,
                                  final int yOffset) {
        if (TextUtils.isEmpty(text)) {
            return;
        }

        ToastProperty toastProperty = makeToastProperty(type, text, duration, drawable, gravity,
                xOffset, yOffset);
        showToastHandler(toastProperty);
    }

    private static void showToastHandler(final ToastProperty toastProperty) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mToast == null) {
                    showNewToast(toastProperty);
                } else if (isSameToast(toastProperty)) {
                    showCurrentToast();
                } else {
                    replaceCurrentToast(toastProperty);
                }
            }
        });
    }

    private static ToastProperty makeToastProperty(int type, CharSequence text, int duration,
                                                   Drawable drawable, int gravity, int xOffset,
                                                   int yOffset) {
        Object extra = null;
        if (type == ToastProperty.TYPE_IMAGE) {
            ToastProperty.ImageExtra imageExtra = new ToastProperty.ImageExtra();
            imageExtra.drawableHashcode = drawable.hashCode();
            imageExtra.drawable = drawable;
            extra = imageExtra;
        }

        return new ToastProperty.Builder().setType(type).setText(text).setDuration(duration)
                .setGravity(gravity).setXOffset(xOffset).setYOffset(yOffset).setExtra(extra)
                .build();
    }

    private static boolean isSameToast(ToastProperty toastProperty) {
        return toastProperty.equals(mToastProperty);
    }

    private static void showNewToast(ToastProperty toastProperty) {
        mToastProperty = toastProperty;
        mToast = createToast(toastProperty);
        try {
            mToast.show();
        } catch (Exception ex) {
            SLog.e("CommonToast", "showNewToast error, exception = " + printStack(ex));
        }
    }

    private static String printStack(Throwable t) {
        if (t == null) return null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            try {
                t.printStackTrace();//输出到system.err
                t.printStackTrace(new PrintStream(baos));
            } finally {
                baos.close();
            }
        } catch (IOException e) {

        }

        return baos.toString();
    }

    private static Toast createToast(ToastProperty toastProperty) {
        Toast toast;
        switch (toastProperty.getType()) {
            case ToastProperty.TYPE_TEXT:
                toast = createTextToast();
                break;
            case ToastProperty.TYPE_IMAGE:
                toast = createImageToast(toastProperty);
                break;
            default:
                toast = createTextToast();
                break;
        }

        toast.setDuration(mToastProperty.getDuration());
        toast.setText(mToastProperty.getText());
        toast.setGravity(mToastProperty.getGravity(), mToastProperty.getxOffset(), mToastProperty
                .getyOffset());

        return toast;
    }

    @NonNull
    private static Toast createTextToast() {
        return new TextToast(SunApplication.getAppContext());
    }

    @NonNull
    private static Toast createImageToast(ToastProperty toastProperty) {
        ImageTextToast imageTextToast = new ImageTextToast(SunApplication.getAppContext());
        ToastProperty.ImageExtra imageExtra = (ToastProperty.ImageExtra) toastProperty.getExtra();
        imageTextToast.setImage(imageExtra.drawable);
        imageExtra.drawable = null;
        return imageTextToast;
    }

    private static void replaceCurrentToast(ToastProperty toastProperty) {
        mToast.cancel();
        mToast = null;
        mToastProperty = toastProperty;
        showNewToast(toastProperty);
    }

    private static void showCurrentToast() {
        // 以前会判断当前的toast是否正在显示，现在不判断了，Toast内部的实现会处理这种情况，参考NotificationManagerService.mService
        // .enqueueToast()方法的实现
        mToast.setText(mToastProperty.getText());
        mToast.show();
    }

    /**
     * 隐藏Toast
     */
    public static void forceHideToast() {
        if (mToast != null) {
            mToast.cancel();
            //解决：【其他】退出app,toast一直不消失，再点击icon启动，app无法启动  ID： 51827531
            mHandler.removeCallbacksAndMessages(null);
        }
    }
}
