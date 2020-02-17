package com.idevel.waterbottle.utils

import android.util.Log
import com.idevel.waterbottle.BuildConfig

/**
 * The MLogger
 * @company : medialog
 * @author  : jjbae
 */
object DLog {
    private const val TAG = "PrePayment_AOS"

    private fun getLogMsg(msg: Any): String {
        val stack = Throwable().fillInStackTrace()
        val trace = stack.stackTrace

        return "[${trace[2].fileName}>${trace[2].methodName}():${trace[2].lineNumber}]: $msg"
    }

    @JvmStatic
    fun i() {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, getLogMsg(""))
        }
    }

    @JvmStatic
    fun i(msg: Any) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, getLogMsg(msg))
        }
    }

    @JvmStatic
    fun d() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, getLogMsg(""))
        }
    }

    @JvmStatic
    fun d(msg: Any) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, getLogMsg(msg))
        }
    }

    @JvmStatic
    fun w() {
        if (BuildConfig.DEBUG) {
            Log.w(TAG, getLogMsg(""))
        }
    }

    @JvmStatic
    fun w(msg: Any) {
        if (BuildConfig.DEBUG) {
            Log.w(TAG, getLogMsg(msg))
        }
    }

    @JvmStatic
    fun e() {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, getLogMsg(""))
        }
    }

    @JvmStatic
    fun e(msg: Any) {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, getLogMsg(msg))
        }
    }

    @JvmStatic
    fun d(tag: String, msg: Any) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, getLogMsg(msg))
        }
    }

    @JvmStatic
    fun i(tag: String, msg: Any) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, getLogMsg(msg))
        }
    }

    @JvmStatic
    fun w(tag: String, msg: Any) {
        if (BuildConfig.DEBUG) {
            Log.w(tag, getLogMsg(msg))
        }
    }

    @JvmStatic
    fun e(tag: String, msg: Any) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, getLogMsg(msg))
        }
    }

    @JvmStatic
    fun v(msg: String) {
        if (BuildConfig.DEBUG) {
            Log.v(TAG, getLogMsg(msg))

        }
    }

    @JvmStatic
    fun v(TAG: String, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.v(TAG, getLogMsg(msg))
        }
    }
}
