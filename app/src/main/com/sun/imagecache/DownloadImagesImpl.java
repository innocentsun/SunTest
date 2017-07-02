package com.sun.imagecache;

import java.util.List;

public class DownloadImagesImpl implements ImageCacheRequestListener {
    private int mRemain;
    private ThumbnailListDownloadListener downloadListener;
    private List<String> mUrlList;
    private final int mTotal;

    public DownloadImagesImpl(List<String> urlList, ThumbnailListDownloadListener downloadListener) {
        mTotal = urlList.size();
        this.mUrlList = urlList;
        this.downloadListener = downloadListener;
        mRemain = urlList.size();
    }

    public void startDownload(BaseGetThumbnail getThumbnailImpl) {
        for (String url : mUrlList) {
            getThumbnailImpl.getThumbnail(url, this);
        }
    }

    private int getPercent() {
        if (mRemain > 0) {
            return (int) ((mTotal - mRemain) * 100f / mTotal);
        } else {
            return 100;
        }
    }

    @Override
    public void requestCompleted(RequestResult request) {
        synchronized (this) {
            if (mRemain > 0) {
                mRemain--;
            }
        }

        if (downloadListener != null) {
            downloadListener.onProgress(ThumbnailListDownloadListener.CODE_OK, getPercent());
        }
    }

    @Override
    public void requestCancelled(String url) {
        if (downloadListener != null) {
            downloadListener.onProgress(ThumbnailListDownloadListener.CODE_CANCELLED, getPercent());
        }
    }

    @Override
    public void requestFailed(String url) {
        if (downloadListener != null) {
            downloadListener.onProgress(ThumbnailListDownloadListener.CODE_FAIL, getPercent());
        }
    }
}
