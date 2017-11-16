package net.gotev.uploadservice;

import java.lang.ref.WeakReference;

/**
 * Android Upload Service library logger.
 * You can provide your own logger delegate implementation, to be able to log in a different way.
 * By default the log level is set to DEBUG when the build type is debug, and OFF in release.
 * The default logger implementation logs in Android's LogCat.
 * @author gotev (Aleksandar Gotev)
 */
public class Logger {

    public enum LogLevel {
        DEBUG,
        INFO,
        ERROR,
        OFF
    }

    public interface LoggerDelegate {
        void error(String tag, String message);
        void error(String tag, String message, Throwable exception);
        void debug(String tag, String message);
        void info(String tag, String message);
    }

    private LogLevel mLogLevel = BuildConfig.DEBUG ? LogLevel.DEBUG : LogLevel.OFF;
    private static LoggerDelegate mDefaultLogger = new DefaultLoggerDelegate();
    private WeakReference<LoggerDelegate> mDelegate = new WeakReference<>(mDefaultLogger);

    private Logger() { }

    private static class SingletonHolder {
        private static final Logger instance = new Logger();
    }

    public static void resetLoggerDelegate() {
        synchronized (Logger.class) {
            SingletonHolder.instance.mDelegate = new WeakReference<>(mDefaultLogger);
        }
    }

    public static void setLoggerDelegate(LoggerDelegate delegate) {
        if (delegate == null)
            throw new IllegalArgumentException("delegate MUST not be null!");

        synchronized (Logger.class) {
            SingletonHolder.instance.mDelegate = new WeakReference<>(delegate);
        }
    }

    public static void setLogLevel(LogLevel level) {
        synchronized (Logger.class) {
            SingletonHolder.instance.mLogLevel = level;
        }
    }

    private static boolean delegateIsDefinedAndLogLevelIsAtLeast(LogLevel level) {
        return SingletonHolder.instance.mDelegate.get() != null
                && SingletonHolder.instance.mLogLevel.compareTo(level) <= 0;
    }

    public static void error(String tag, String message) {
        if (delegateIsDefinedAndLogLevelIsAtLeast(LogLevel.ERROR)) {
            SingletonHolder.instance.mDelegate.get().error(tag, message);
        }
    }

    public static void error(String tag, String message, Throwable exception) {
        if (delegateIsDefinedAndLogLevelIsAtLeast(LogLevel.ERROR)) {
            SingletonHolder.instance.mDelegate.get().error(tag, message, exception);
        }
    }

    public static void info(String tag, String message) {
        if (delegateIsDefinedAndLogLevelIsAtLeast(LogLevel.INFO)) {
            SingletonHolder.instance.mDelegate.get().info(tag, message);
        }
    }

    public static void debug(String tag, String message) {
        if (delegateIsDefinedAndLogLevelIsAtLeast(LogLevel.DEBUG)) {
            SingletonHolder.instance.mDelegate.get().debug(tag, message);
        }
    }
}
