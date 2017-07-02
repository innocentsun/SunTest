package com.sun.logger;

import com.sun.base.SunApplication;

/**
 * Created by sunhzchen on 2017/1/5.
 * 日志相关常量
 */

class LogConstants {

    static final String LOG_DIRECTORY = SunApplication.getAppContext().getExternalFilesDir
            (null) + "/log/"; //路径：data/包名/files/log/
    static final String LOG_FILE_NAME = "SunLog"; //日志文件名

    static final long CHECK_CLOSE_LOOP_INTERVAL = 10 * 1000L; //检查文件是否需要关闭写入流的轮训间隔
    static final int MAX_FILE_COUNT = 3; //保留最近日志文件数量
    static final int MAX_FILE_COUNT_NUMBER = 10000; //日志文件编号上限
    static final int MAX_FILE_SIZE = 2 * 1024 * 1024; //单个日志文件最大字节数
    static final int LOG_INFO_CACHE_COUNT = 48; //缓存log对象个数

    static final int INVALID = -1;
    static final int LEVEL_V = 0;
    static final int LEVEL_D = 1;
    static final int LEVEL_I = 2;
    static final int LEVEL_W = 3;
    static final int LEVEL_E = 4;
    static final int LEVEL_A = 5;

    static final String[] LEVEL_MESSAGE = new String[]{"V", "D", "I", "W", "E", "A"};
}
