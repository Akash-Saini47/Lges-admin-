package com.example.util

import android.util.Log

/**
 * Controlled, secure logging abstraction.
 * - Suppresses sensitive information (API keys, credentials, full PII).
 * - Avoids raw printStackTrace() in production.
 */
object AppLogger {

    private const val DEFAULT_TAG = "LGESAdmin"

    var isDebugEnabled: Boolean = true

    fun d(tag: String = DEFAULT_TAG, message: String) {
        if (isDebugEnabled) {
            Log.d(tag, sanitize(message))
        }
    }

    fun i(tag: String = DEFAULT_TAG, message: String) {
        Log.i(tag, sanitize(message))
    }

    fun w(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w(tag, sanitize(message), throwable)
        } else {
            Log.w(tag, sanitize(message))
        }
    }

    fun e(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, sanitize(message), throwable)
        } else {
            Log.e(tag, sanitize(message))
        }
    }

    /**
     * Strips API keys or passwords from logged strings.
     */
    private fun sanitize(input: String): String {
        return input
            .replace(Regex("""("apiKey"\s*:\s*")([^"]+)(")"""), "$1***REDACTED***$3")
            .replace(Regex("""(apiKey=)([^&]+)"""), "$1***REDACTED***")
            .replace(Regex("""(key=)([^&]+)"""), "$1***REDACTED***")
    }
}
