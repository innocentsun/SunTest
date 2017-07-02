package com.sun.base;

import android.util.SparseArray;

import com.sun.utils.AppUtils;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ListenerManager<T> {

    public interface INotifyCallback<T> {

        void onNotify(T listener);
    }

    private static final int MAX_PRIORITY = 10;
    private static final int MIN_PRIORITY = -10;
    private static final int DEFAULT_PRIORITY = 0;

    private final SparseArray<ConcurrentLinkedQueue<WeakReference<T>>> mListenerListArray = new
            SparseArray<>();

    public void register(T listener) {
        register(listener, DEFAULT_PRIORITY);
    }

    public void register(T listener, int priority) {
        if (listener == null || priority < MIN_PRIORITY || priority > MAX_PRIORITY) {
            return;
        }

        synchronized (mListenerListArray) {
            ConcurrentLinkedQueue<WeakReference<T>> listenerList = mListenerListArray.get(priority);
            if (listenerList == null) {
                listenerList = new ConcurrentLinkedQueue<>();
                mListenerListArray.put(priority, listenerList);
            }

            boolean contain = false;
            for (Iterator<WeakReference<T>> iterator = listenerList.iterator(); iterator.hasNext
                    (); ) {
                T listenerItem = iterator.next().get();
                if (listenerItem == null) {
                    iterator.remove();
                } else if (listenerItem == listener) {
                    contain = true;
                }
            }
            if (!contain) {
                WeakReference<T> weakListener = new WeakReference<>(listener);
                listenerList.add(weakListener);
            }
        }
    }

    public void unregister(T listener) {
        if (listener == null) {
            return;
        }

        synchronized (mListenerListArray) {
            int arraySize = mListenerListArray.size();
            for (int index = 0; index < arraySize; index++) {
                ConcurrentLinkedQueue<WeakReference<T>> listenerList = mListenerListArray.valueAt
                        (index);
                if (listenerList == null) {
                    continue;
                }
                for (Iterator<WeakReference<T>> iterator = listenerList.iterator(); iterator
                        .hasNext(); ) {
                    T listenerItem = iterator.next().get();
                    if (listenerItem == listener) {
                        iterator.remove();
                        return;
                    }
                }
            }
        }
    }

    public void startNotify(INotifyCallback<T> callback) {
        for (int priority = MAX_PRIORITY; priority >= MIN_PRIORITY; priority--) {
            ConcurrentLinkedQueue<WeakReference<T>> copyListenerList;
            synchronized (mListenerListArray) {
                ConcurrentLinkedQueue<WeakReference<T>> listenerList = mListenerListArray.get
                        (priority);
                if (listenerList == null || listenerList.isEmpty()) {
                    continue;
                }
                copyListenerList = new ConcurrentLinkedQueue<>(listenerList);
            }
            for (Iterator<WeakReference<T>> iterator = copyListenerList.iterator(); iterator
                    .hasNext(); ) {
                T listenerItem = iterator.next().get();
                if (listenerItem == null) {
                    iterator.remove();
                } else {
                    try {
                        callback.onNotify(listenerItem);
                    } catch (Throwable e) {
                        AppUtils.remindException(e);
                    }
                }
            }
            copyListenerList.clear();
        }
    }

    public void clear() {
        synchronized (mListenerListArray) {
            mListenerListArray.clear();
        }
    }

    public int size() {
        synchronized (mListenerListArray) {
            int totalSize = 0;
            int arraySize = mListenerListArray.size();
            for (int index = 0; index < arraySize; index++) {
                ConcurrentLinkedQueue<WeakReference<T>> listenerList = mListenerListArray.valueAt
                        (index);
                if (listenerList == null) {
                    continue;
                }
                totalSize += listenerList.size();
            }
            return totalSize;
        }
    }
}