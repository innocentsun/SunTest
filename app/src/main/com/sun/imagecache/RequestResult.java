package com.sun.imagecache;

import android.graphics.Bitmap;

public class RequestResult {
    public Bitmap mBitmap;

    public String mUrl;

    public RequestResult(Bitmap bitmap, String url) {
        mBitmap = bitmap;
        mUrl = url;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public String getUrl() {
        return mUrl;
    }
}
