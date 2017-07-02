package com.sun.imagecache;

import android.graphics.Bitmap;

import java.util.List;

public abstract class BaseGetThumbnail {
    public abstract Bitmap getThumbnail(String url, ImageCacheRequestListener listener);

    public void downloadThumbnails(List<String> urls, ThumbnailListDownloadListener downloadListener) {
        if (urls == null || urls.size() == 0) {
            if (downloadListener != null) {
                downloadListener.onProgress(ThumbnailListDownloadListener.CODE_INVALID_INPUT, 0);
            }
        } else {
            DownloadImagesImpl downloadImpl = new DownloadImagesImpl(urls, downloadListener);
            downloadImpl.startDownload(this);
        }
    }

    public void downloadThumbnails(DownloadImagesImpl downloadImpl){
        if(downloadImpl != null){
            downloadImpl.startDownload(this);
        }
    }
}
