package net.ypresto.androidtranscoder.utils;

import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class Logger {

    public final static int LEVEL_VERBOSE = 0;
    public final static int LEVEL_INFO = 1;
    public final static int LEVEL_WARNING = 2;
    public final static int LEVEL_ERROR = 3;

    private static int sLevel;

    /**
     * Interface of integers representing log levels.
     * @see #LEVEL_VERBOSE
     * @see #LEVEL_INFO
     * @see #LEVEL_WARNING
     * @see #LEVEL_ERROR
     */
    @IntDef({LEVEL_VERBOSE, LEVEL_INFO, LEVEL_WARNING, LEVEL_ERROR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface LogLevel {}

    private String mTag;

    Logger(String tag) {
        mTag = tag;
    }

    /**
     * Sets the log sLevel for logcat events.
     *
     * @see #LEVEL_VERBOSE
     * @see #LEVEL_INFO
     * @see #LEVEL_WARNING
     * @see #LEVEL_ERROR
     * @param logLevel the desired log sLevel
     */
    public static void setLogLevel(@LogLevel int logLevel) {
        sLevel = logLevel;
    }

    private boolean should(int messageLevel) {
        return sLevel <= messageLevel;
    }

    void v(String message, @Nullable Throwable error) {
        log(LEVEL_VERBOSE, message, error);
    }

    void i(String message, @Nullable Throwable error) {
        log(LEVEL_INFO, message, error);
    }

    void w(String message, @Nullable Throwable error) {
        log(LEVEL_WARNING, message, error);
    }

    void e(String message, @Nullable Throwable error) {
        log(LEVEL_ERROR, message, error);
    }

    private void log(int level, String message, @Nullable Throwable throwable) {
        if (!should(level)) return;
        switch (level) {
            case LEVEL_VERBOSE: Log.v(mTag, message, throwable); break;
            case LEVEL_INFO: Log.i(mTag, message, throwable); break;
            case LEVEL_WARNING: Log.w(mTag, message, throwable); break;
            case LEVEL_ERROR: Log.e(mTag, message, throwable); break;
        }
    }
}
