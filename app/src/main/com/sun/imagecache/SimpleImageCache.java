package com.sun.imagecache;

import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.LruCache;

import com.sun.base.MemoryWarningManager;
import com.sun.utils.BitmapUtils;

/**
 * 简易版图片内存缓存，和SimpleImageManger的文件缓存对应。
 */
public class SimpleImageCache {

    private static final int CACHE_MEMORY_RATIO = 20;
    private LruCache<String, Bitmap> mCache;

    private static class InstanceHolder {
        private static SimpleImageCache sInstance = new SimpleImageCache();
    }

    public static SimpleImageCache getInstance() {
        return InstanceHolder.sInstance;
    }

    private SimpleImageCache() {
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheSize = maxMemory / CACHE_MEMORY_RATIO;
        mCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };

        MemoryWarningManager.getInstance().register(new MemoryWarningManager.IMemoryWarningListener() {
            @Override
            public void onMemoryWarning() {
                mCache.evictAll();
            }
        });
    }

    public void getBitmap(final String url, final ImageCacheRequestListener listener) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        Bitmap bitmap = mCache.get(url);
        if (BitmapUtils.isValidBitmap(bitmap)) {
            notifyCompleted(new RequestResult(bitmap, url), listener);
            return;
        }
        SimpleImageManager.getInstance().getThumbnail(url, new ImageCacheRequestListener() {
            @Override
            public void requestCompleted(RequestResult request) {
                mCache.put(url, request.getBitmap());
                SimpleImageManager.requestCompleted(listener, request);
            }

            @Override
            public void requestCancelled(String url) {
                SimpleImageManager.requestCancelled(listener, url);
            }

            @Override
            public void requestFailed(String url) {
                SimpleImageManager.requestFailed(listener, url);
            }
        });
    }

    private void notifyCompleted(RequestResult result, ImageCacheRequestListener listener) {
        if (listener == null) {
            return;
        }
        listener.requestCompleted(result);
    }
}
