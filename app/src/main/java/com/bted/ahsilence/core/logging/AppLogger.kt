package com.bted.ahsilence.core.logging

import android.util.Log

object AppLogger {
    private const val TAG = "AhSilence"

    fun d(message: String) = Log.d(TAG, message)
    fun e(message: String) = Log.e(TAG, message)
    fun w(message: String) = Log.w(TAG, message)
}
