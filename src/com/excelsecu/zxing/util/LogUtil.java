package com.excelsecu.zxing.util;

import android.util.Log;

/**
 * 日志工具类，集中控制日志的打??后期考虑打印入file或??上传到服务器等策??
 *
 * @author zzz
 * @version 1.0
 * @date 2013-07-09
 */
public class LogUtil {
	/**
	 * 默认的文库日志Tag标签
	 */
	public final static String DEFAULT_TAG = Log.class.getSimpleName();

	/**
	 * 此常量用于控制是否打日志到Logcat??release版本中本变量应置为false
	 */
	public final static boolean LOGGABLE = true;

    /**
     * 判断是否使能打印log
     */
	public static boolean IsLogEnable()
	{
		return LOGGABLE;
	}

	/**
	 * 打印debug级别的log
	 *
	 * @param tag
	 *            tag标签
	 * @param str
	 *            内容
	 */
    public static void d(String tag, String str) {
		if (LOGGABLE && str != null) {
			Log.d(tag, str);
		}
	}

    public static void d(String tag, String str, Exception e) {
        if (LOGGABLE && str != null && e != null) {
            Log.d(tag, str, e);
        }
    }

	public static void d(String str) {
		if (LOGGABLE && str != null) {
			Log.d(DEFAULT_TAG, str);
		}
	}

	/**
	 * 打印warning级别的log
	 *
	 * @param tag
	 *            tag标签
	 * @param str
	 *            内容
	 */
	public static void w(String tag, String str) {
		if (LOGGABLE && str != null) {
			Log.w(tag, str);
		}
	}

	public static void w(String str) {
		if (LOGGABLE && str != null) {
			Log.w(DEFAULT_TAG, str);
		}
	}

    public static void w(String tag, String str, Exception e) {
        if (LOGGABLE && str != null && e != null) {
            Log.d(tag, str, e);
        }
    }

	/**
	 * 打印error级别的log
	 *
	 * @param tag
	 *            tag标签
	 * @param str
	 *            内容
	 */
	public static void e(String tag, String str) {
		if (LOGGABLE && str != null) {
			Log.e(tag, str);
		}
	}

	public static void e(String str) {
		if (LOGGABLE && str != null) {
			Log.e(DEFAULT_TAG, str);
		}
	}

    public static void e(String tag, String str, Exception e) {
        if (LOGGABLE && str != null && e != null) {
            Log.d(tag, str, e);
        }
    }

	/**
	 * 打印info级别的log
	 *
	 * @param tag
	 *            tag标签
	 * @param str
	 *            内容
	 */
	public static void i(String tag, String str) {
		if (LOGGABLE && str != null) {
			Log.i(tag, str);
		}
	}

	public static void i(String str) {
		if (LOGGABLE && str != null) {
			Log.i(DEFAULT_TAG, str);
		}
	}

    public static void i(String tag, String str, Exception e) {
        if (LOGGABLE && str != null && e != null) {
            Log.d(tag, str, e);
        }
    }

	/**
	 * 打印verbose级别的log
	 *
	 * @param tag
	 *            tag标签
	 * @param str
	 *            内容
	 */
	public static void v(String tag, String str) {
		if (LOGGABLE && str != null) {
			Log.v(tag, str);
		}
	}

	public static void v(String str) {
		if (LOGGABLE && str != null) {
			Log.v(DEFAULT_TAG, str);
		}
	}

    public static void v(String tag, String str, Exception e) {
        if (LOGGABLE && str != null && e != null) {
            Log.d(tag, str, e);
        }
    }
}
