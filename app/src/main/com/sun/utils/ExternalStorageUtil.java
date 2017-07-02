package com.sun.utils;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;

import com.sun.base.SunApplication;
import com.sun.logger.SLog;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by sunhzchen on 2017/1/11.
 * 外存工具类
 */

public class ExternalStorageUtil {

    private static final String TAG = "ExternalStorageUtil";

    /**
     * 保存图片到外存
     *
     * @param bm
     * @throws Exception
     */
    public static boolean saveBitmapFile(Bitmap bm, String path, int quality) {
        boolean ret = false;
        if (bm != null) {
            if (quality > 100) quality = 100;
            File myCaptureFile = new File(path);
            BufferedOutputStream bos = null;
            try {
                bos = new BufferedOutputStream(new FileOutputStream(myCaptureFile));
                bm.compress(Bitmap.CompressFormat.JPEG, quality, bos);

                ExternalStorageUtil.notifyFileAdd(path);

                ret = true;
            } catch (FileNotFoundException e) {
                SLog.e(TAG, e);
            } catch (Exception e) {
                myCaptureFile.delete();
                SLog.e(TAG, e);
            } finally {
                if (bos != null) {
                    try {
                        bos.flush();
                        bos.close();
                    } catch (IOException e) {
                    }
                }

            }
        }
        return ret;
    }


    /**
     * 通知系统，这样新增的图片可以及时在相册中看到
     */
    public static void notifyFileAdd(String filePath) {
        Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        scanIntent.setData(Uri.fromFile(new File(filePath)));
        SunApplication.getAppContext().sendBroadcast(scanIntent);
    }

}
