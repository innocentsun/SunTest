package com.sun.imagecache;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.sun.base.SunApplication;
import com.sun.base.ThreadManager;
import com.sun.logger.SLog;
import com.sun.utils.ExternalStorageUtil;
import com.sun.utils.TimeUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.zip.GZIPInputStream;

/**
 * 简易版图片库，作为Fresco图片库的功能补充，主要用于子进程或其他简单的图片缓存任务。
 */
public class SimpleImageManager extends BaseGetThumbnail {
    private static final String TAG = "SimpleImageManager";
    private static final String IMAGE_FOLDER = "/files/image";
    private static final String TMP_FILE_EXT = ".tmp";
    private static final String FILE_SCHEME = "file://";
    private static final String KEY_DIVIDER = "@";
    private static final long MAX_CACHE_SIZE = 10 * 1024 * 1024;
    private static final int TIME_OUT = 8000;
    private static boolean sHaveChecked = false;
    private static ArrayList<String> sDownloadingUrl = new ArrayList<>();

    private static Comparator<File> sTimeComparator = new Comparator<File>() {

        @Override
        public int compare(File firstFile, File secondFile) {
            long first = firstFile.lastModified();
            long second = secondFile.lastModified();
            if (first == second) {
                return 0;
            } else if (first > second) {
                return 1;
            } else {
                return -1;
            }
        }
    };

    private static class InstanceHolder {

        private static SimpleImageManager sInstance = new SimpleImageManager();
    }
    public static SimpleImageManager getInstance() {
        return InstanceHolder.sInstance;
    }

    @Override
    public Bitmap getThumbnail(String url, ImageCacheRequestListener listener) {
        Bitmap bitmap = getFromCache(url);
        if (null != bitmap) {
            requestCompleted(listener, new RequestResult(bitmap, url));
            return bitmap;
        }

        getFromNetAsync(listener, url);
        return null;
    }

    private static void getFromNetAsync(final ImageCacheRequestListener listener, final String url) {
        ThreadManager.getInstance().execute(new Runnable() {

            @Override
            public void run() {
                Bitmap netBitmap = getFromNet(url);
                if (netBitmap == null) {
                    requestFailed(listener, url);
                } else {
                    requestCompleted(listener, new RequestResult(netBitmap, url));
                }
            }
        });
    }

    private static Bitmap getFromCache(String url) {
        if (!sHaveChecked) {
            checkCacheSize();
        }
        File file = getCacheFile(url);
        if (file != null && file.exists()) {
            if (file.length() <= 0) {
                file.delete();
                return null;
            }
            file.setLastModified(System.currentTimeMillis());
            return decodeFile(file.toString());
        }
        return null;
    }

    private static Bitmap getFromNet(String url) {
        File file = downloadImage(url);
        if (file != null) {
            return decodeFile(file.toString());
        } else {
            return null;
        }

    }

    private static File downloadImage(String url) {
        if (TextUtils.isEmpty(url) || isUrlInvalid(url)) {
            return null;
        }
        File cacheFile = getCacheFile(url);
        if (cacheFile == null) {
            return null;
        } else if (cacheFile.exists()) {
            return cacheFile;
        }
        checkCacheDirectory();
//        if (!NetworkUtil.isNetworkActive()) {
//            return null;
//        }

        File downFile = new File(cacheFile.getPath() + TMP_FILE_EXT);
        if (downFile.exists()) {
            downFile.delete();
        }
        if (sDownloadingUrl.contains(url)) {
            return null;
        }
        sDownloadingUrl.add(url);
        File retFile = downloadImage(url, cacheFile, downFile);
        sDownloadingUrl.remove(url);
        return retFile;
    }

    private static File downloadImage(String url, File cacheFile, File downloadFile) {
        if (url.startsWith(FILE_SCHEME)) {
            return extractImageFromFile(url, cacheFile);
        } else {
            return downloadImageByHttp(url, cacheFile, downloadFile);
        }
    }

    private static File extractImageFromFile(String url, File cacheFile) {
        String filePath = parseFilePathFromUrl(url);
        Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(filePath, MediaStore.Images.Thumbnails.MINI_KIND);
        ExternalStorageUtil.saveBitmapFile(bitmap, cacheFile.getAbsolutePath(), 100);
        return cacheFile;
    }

    private static String parseFilePathFromUrl(String thumbNailUrl) {
        int start = thumbNailUrl.indexOf(KEY_DIVIDER) + 1;
        return thumbNailUrl.substring(start);
    }

    private static File downloadImageByHttp(String url, File cacheFile, File downFile) {
        HttpURLConnection con = null;
        BufferedInputStream bis = null;
        FileOutputStream ops = null;
        try {
            con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Accept-Encoding", "gzip");
            con.setConnectTimeout(TIME_OUT);
            if ("gzip".equals(con.getContentEncoding())) {
                bis = new BufferedInputStream(new GZIPInputStream(con.getInputStream()));
            } else {
                bis = new BufferedInputStream(con.getInputStream());
            }
            ops = new FileOutputStream(downFile);
            byte[] buffer = new byte[8 * 1024];
            int size;
            while ((size = bis.read(buffer)) != -1) {
                ops.write(buffer, 0, size);
            }
            ops.flush();
            downFile.renameTo(cacheFile);
            return cacheFile;
        } catch (Exception e) {
            SLog.e(TAG, e, "download image error");
            return null;
        } finally {
            try {
                if (bis != null) {
                    bis.close();
                }
            } catch (IOException ioe) {
            }
            try {
                if (ops != null) {
                    ops.close();
                }
            } catch (IOException ioe) {
            }
            if (con != null) {
                con.disconnect();
            }
        }
    }

    private static File getCacheFile(String url) {
        // use the hash code of url as cache file name.
        if (url == null) {
            return null;
        }
        int fileName = url.hashCode();
        String rootPath = getRootPath();
        if (rootPath == null) {
            return null;
        }
        return new File(rootPath + "/" + fileName + ".jpg");
    }

    private static String getRootPath() {
        try {
            return Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/"
                    + SunApplication.getAppContext().getPackageName() + IMAGE_FOLDER;
        } catch (Exception e) {
        }
        return null;
    }

    private static void checkCacheDirectory() {
        try {
            // root directory
            String rootPath = getRootPath();
            if (rootPath == null) {
                return;
            }
            File rootFolder = new File(rootPath);
            if (!rootFolder.exists()) {
                rootFolder.mkdirs();
            }
        } catch (Exception e) {
        }
    }

    private static void checkCacheSize() {
        sHaveChecked = true;

        ThreadManager.getInstance().postDelayed(new Runnable() {
            @Override
            public void run() {
                String rootPath = getRootPath();
                if (rootPath == null) {
                    return;
                }
                File rootFile = new File(rootPath);
                if (!rootFile.exists()) {
                    return;
                }
                File[] images = rootFile.listFiles();
                if (images != null) {
                    long totalSize = 0;
                    for (File image : images) {
                        totalSize += image.length();
                    }
                    // cut cache to half if needed
                    if (totalSize >= MAX_CACHE_SIZE) {
                        clearCache(images);
                    }
                }
            }
        }, TimeUtils.ONE_MINUTE);
    }

    private static void clearCache(File[] images) {
        try {
            Arrays.sort(images, sTimeComparator);
        } catch (Exception e) {
        }

        int count = images.length;
        count /= 2;
        for (int i = 0; i < count; i++) {
            images[i].delete();
        }
    }

    private static Bitmap decodeFile(String path) {
        return decodeFile(path, null);
    }

    private static synchronized Bitmap decodeFile(String path, BitmapFactory.Options opts) {
        try {
            return BitmapFactory.decodeFile(path, opts);
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    private static boolean isUrlInvalid(String url) {
        return !isUrlValid(url);
    }

    private static boolean isUrlValid(String url) {
        return url != null && (url.startsWith("http") || url.startsWith("https") || url.startsWith(FILE_SCHEME));
    }

    // callback
    public static void requestCompleted(ImageCacheRequestListener listener, RequestResult request) {
        if (listener != null) {
            listener.requestCompleted(request);
        }
    }

    public static void requestFailed(ImageCacheRequestListener listener, String url) {
        if (listener != null) {
            listener.requestFailed(url);
        }
    }

    public static void requestCancelled(ImageCacheRequestListener listener, String url) {
        if (listener != null) {
            listener.requestCancelled(url);
        }
    }

}
