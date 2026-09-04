package com.example.util

import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Calendar
import java.util.Locale
import java.util.UUID

/**
 * Central configuration and utility functions for certificate
 * generation, verification URLs, certificate IDs, and CSV export.
 */
object CertificateConfig {

    // ============================================================
    // DEFAULT CONFIGURATION
    // ============================================================

    const val DEFAULT_BASE_VERIFICATION_URL =
        "https://lges-computer-classes.netlify.app/verify.html"

    const val DEFAULT_INSTITUTE_WEBSITE =
        "https://lges-computer-classes.netlify.app/"

    const val DEFAULT_INSTITUTE_NAME =
        "Lakshmi Group of Education Society"

    const val DEFAULT_WEB_APP_URL =
        "https://script.google.com/macros/s/AKfycbxb9VlwWNXkJwjt1927Ju1bKWzaRdbXUZVvaS6jbLBQ-l9NUudXRVfq9lNchthpcIlm0g/exec"

    // ============================================================
    // CERTIFICATE ID
    // ============================================================

    const val CERTIFICATE_PREFIX = "LGES-"

    /**
     * Generates a genuinely unique certificate ID in modern format.
     *
     * Example:
     * LGES-2026-A1B2C3D4E5F6
     *
     * Certificate ID is strictly immutable and NOT based on roll number,
     * allowing the same student to receive multiple certificates under one roll number.
     */
    fun generateCertificateId(): String {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val uniquePart = UUID.randomUUID()
            .toString()
            .replace("-", "")
            .uppercase(Locale.ROOT)
            .take(12)

        return "$CERTIFICATE_PREFIX$year-$uniquePart"
    }

    /**
     * Legacy certificate ID helper.
     *
     * KEPT STRICTLY for reading or migrating legacy data.
     * New certificates MUST NOT use this generator.
     */
    fun computeCertificateId(rollNo: String): String {
        var clean = rollNo.trim()
        if (clean.isBlank()) return ""

        while (clean.startsWith(CERTIFICATE_PREFIX, ignoreCase = true)) {
            clean = clean.substring(CERTIFICATE_PREFIX.length).trim()
        }

        if (clean.isBlank()) return ""
        return CERTIFICATE_PREFIX + clean
    }

    /**
     * Checks whether the value conforms to an LGES certificate ID pattern
     * (either modern LGES-YYYY-HEX or legacy LGES-ROLL).
     */
    fun isCertificateId(value: String): Boolean {
        val clean = value.trim()
        if (clean.isBlank()) return false

        return clean.startsWith(CERTIFICATE_PREFIX, ignoreCase = true) &&
                clean.substring(CERTIFICATE_PREFIX.length).trim().isNotBlank()
    }

    // ============================================================
    // VERIFICATION URL
    // ============================================================

    /**
     * Builds: verify.html?certNo=LGES-2026-A1B2C3D4E5F6
     */
    fun buildVerificationUrl(
        certificateId: String,
        baseUrl: String = DEFAULT_BASE_VERIFICATION_URL
    ): String {
        val safeBase = getSafeVerificationBaseUrl(baseUrl)
        val cleanCertId = certificateId.trim()
        if (cleanCertId.isBlank()) return safeBase

        val encodedId = encodeQueryParameter(cleanCertId)

        val fragmentIndex = safeBase.indexOf('#')
        val urlWithoutFragment: String
        val fragment: String

        if (fragmentIndex >= 0) {
            urlWithoutFragment = safeBase.substring(0, fragmentIndex)
            fragment = safeBase.substring(fragmentIndex)
        } else {
            urlWithoutFragment = safeBase
            fragment = ""
        }

        val cleanedQueryUrl = removeQueryParameter(urlWithoutFragment, "certNo")
        val separator = when {
            cleanedQueryUrl.contains("?") && !cleanedQueryUrl.endsWith("?") && !cleanedQueryUrl.endsWith("&") -> "&"
            cleanedQueryUrl.endsWith("?") || cleanedQueryUrl.endsWith("&") -> ""
            else -> "?"
        }

        return "$cleanedQueryUrl${separator}certNo=$encodedId$fragment"
    }

    private fun encodeQueryParameter(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.name())
    }

    private fun removeQueryParameter(url: String, parameterName: String): String {
        val qIndex = url.indexOf('?')
        if (qIndex < 0) return url

        val basePart = url.substring(0, qIndex)
        val queryPart = url.substring(qIndex + 1)
        if (queryPart.isBlank()) return basePart

        val filtered = queryPart.split('&').filter { param ->
            val eqIndex = param.indexOf('=')
            val key = if (eqIndex >= 0) param.substring(0, eqIndex) else param
            !key.equals(parameterName, ignoreCase = true)
        }

        return if (filtered.isEmpty()) {
            basePart
        } else {
            "$basePart?" + filtered.joinToString("&")
        }
    }

    // ============================================================
    // ROBUST URL VALIDATION & NORMALIZATION
    // ============================================================

    /**
     * Rigorous URI/URL validation checking scheme and host.
     */
    fun isValidHttpUrl(url: String?): Boolean {
        val clean = url?.trim().orEmpty()
        if (clean.isBlank()) return false

        return try {
            val uri = URI(clean)
            val scheme = uri.scheme?.lowercase(Locale.ROOT)
            val host = uri.host

            (scheme == "http" || scheme == "https") &&
                    !host.isNullOrBlank() &&
                    host.contains(".") &&
                    !clean.contains(" ")
        } catch (_: Exception) {
            false
        }
    }

    fun normalizeUrl(url: String): String {
        return url.trim()
    }

    fun getSafeVerificationBaseUrl(configuredUrl: String?): String {
        val clean = configuredUrl?.trim().orEmpty()
        return if (isValidHttpUrl(clean)) {
            clean
        } else {
            DEFAULT_BASE_VERIFICATION_URL
        }
    }

    fun getSafeWebAppUrl(configuredUrl: String?): String {
        val clean = configuredUrl?.trim().orEmpty()
        return if (isValidHttpUrl(clean)) {
            clean
        } else {
            DEFAULT_WEB_APP_URL
        }
    }

    // ============================================================
    // CSV
    // ============================================================

    fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    fun escapeCsvNullable(value: String?): String {
        return escapeCsv(value.orEmpty())
    }
}