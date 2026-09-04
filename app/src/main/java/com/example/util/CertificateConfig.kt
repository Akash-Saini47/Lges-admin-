package com.example.util

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Central configuration and utility functions for certificate
 * generation, verification URLs, certificate IDs and CSV export.
 */
object CertificateConfig {

    // ============================================================
    // DEFAULT CONFIGURATION
    // ============================================================

    const val DEFAULT_BASE_VERIFICATION_URL =
        "https://lges-computer-classes.netlify.app/verify.html"

    const val DEFAULT_INSTITUTE_WEBSITE =
        "https://lges-computer-classes.netlify.app/"

    const val DEFAULT_WEB_APP_URL =
        "https://script.google.com/macros/s/AKfycbxb9VlwWNXkJwjt1927Ju1bKWzaRdbXUZVvaS6jbLBQ-l9NUudXRVfq9lNchthpcIlm0g/exec"

    // ============================================================
    // CERTIFICATE ID
    // ============================================================

    private const val CERTIFICATE_PREFIX = "LGES-"

    /**
     * Generates a genuinely unique certificate ID.
     *
     * Certificate ID is intentionally NOT based on rollNo.
     *
     * Example:
     *
     * LGES-2026-A1B2C3D4
     *
     * This allows the same student to receive:
     *
     * Course      -> LGES-2026-...
     * Internship  -> LGES-2026-...
     * Course 2    -> LGES-2026-...
     */
    fun generateCertificateId(): String {

        val year =
            java.util.Calendar
                .getInstance()
                .get(java.util.Calendar.YEAR)

        val uniquePart =
            UUID.randomUUID()
                .toString()
                .replace("-", "")
                .uppercase(LocaleHolder.ROOT)
                .take(12)

        return "$CERTIFICATE_PREFIX$year-$uniquePart"
    }

    /**
     * Legacy certificate ID generator.
     *
     * KEPT for backward compatibility with existing code/data.
     *
     * New certificates SHOULD use generateCertificateId().
     *
     * Example:
     *
     * 101       -> LGES-101
     * LGES-101  -> LGES-101
     */
    fun computeCertificateId(
        rollNo: String
    ): String {

        var clean = rollNo.trim()

        if (clean.isBlank()) {
            return ""
        }

        while (
            clean.startsWith(
                CERTIFICATE_PREFIX,
                ignoreCase = true
            )
        ) {
            clean =
                clean
                    .substring(
                        CERTIFICATE_PREFIX.length
                    )
                    .trim()
        }

        if (clean.isBlank()) {
            return ""
        }

        return CERTIFICATE_PREFIX + clean
    }

    /**
     * Checks whether the value looks like an LGES certificate ID.
     *
     * Supports both:
     *
     * Old:
     * LGES-101
     *
     * New:
     * LGES-2026-A1B2C3D4
     */
    fun isCertificateId(
        value: String
    ): Boolean {

        val clean = value.trim()

        if (clean.isBlank()) {
            return false
        }

        return clean.startsWith(
            CERTIFICATE_PREFIX,
            ignoreCase = true
        ) &&
            clean.substring(
                CERTIFICATE_PREFIX.length
            ).trim().isNotBlank()
    }

    // ============================================================
    // VERIFICATION URL
    // ============================================================

    /**
     * Builds:
     *
     * verify.html?certNo=LGES-2026-A1B2C3D4
     */
    fun buildVerificationUrl(
        certificateId: String,
        baseUrl: String =
            DEFAULT_BASE_VERIFICATION_URL
    ): String {

        val cleanBase = baseUrl.trim()

        val safeBase =
            if (cleanBase.isBlank()) {
                DEFAULT_BASE_VERIFICATION_URL
            } else {
                cleanBase
            }

        if (certificateId.isBlank()) {
            return safeBase
        }

        /*
         * If certificateId is already a certificate ID,
         * preserve it instead of treating it as a roll number.
         *
         * This is important now that IDs are independent
         * from roll numbers.
         */
        val cleanCertificateId =
            certificateId.trim()

        val encodedId =
            encodeQueryParameter(
                cleanCertificateId
            )

        /*
         * Separate URL fragment.
         */
        val fragmentIndex =
            safeBase.indexOf('#')

        val urlWithoutFragment: String

        val fragment: String

        if (fragmentIndex >= 0) {

            urlWithoutFragment =
                safeBase.substring(
                    0,
                    fragmentIndex
                )

            fragment =
                safeBase.substring(
                    fragmentIndex
                )

        } else {

            urlWithoutFragment = safeBase
            fragment = ""
        }

        val cleanedQueryUrl =
            removeQueryParameter(
                urlWithoutFragment,
                "certNo"
            )

        val separator =
            when {

                cleanedQueryUrl.contains("?") &&
                    !cleanedQueryUrl.endsWith("?") &&
                    !cleanedQueryUrl.endsWith("&") -> {
                    "&"
                }

                cleanedQueryUrl.endsWith("?") ||
                    cleanedQueryUrl.endsWith("&") -> {
                    ""
                }

                else -> {
                    "?"
                }
            }

        return "$cleanedQueryUrl" +
            "${separator}certNo=$encodedId$fragment"
    }

    /**
     * UTF-8 query parameter encoding.
     */
    private fun encodeQueryParameter(
        value: String
    ): String {

        return URLEncoder.encode(
            value,
            StandardCharsets.UTF_8.name()
        )
    }

    /**
     * Removes a query parameter from a URL.
     */
    private fun removeQueryParameter(
        url: String,
        parameterName: String
    ): String {

        val questionMarkIndex =
            url.indexOf('?')

        if (questionMarkIndex < 0) {
            return url
        }

        val basePart =
            url.substring(
                0,
                questionMarkIndex
            )

        val queryPart =
            url.substring(
                questionMarkIndex + 1
            )

        if (queryPart.isBlank()) {
            return basePart
        }

        val filteredParameters =
            queryPart
                .split('&')
                .filter { parameter ->

                    val equalsIndex =
                        parameter.indexOf('=')

                    val key =
                        if (equalsIndex >= 0) {
                            parameter.substring(
                                0,
                                equalsIndex
                            )
                        } else {
                            parameter
                        }

                    !key.equals(
                        parameterName,
                        ignoreCase = true
                    )
                }

        return if (
            filteredParameters.isEmpty()
        ) {
            basePart
        } else {
            "$basePart?" +
                filteredParameters.joinToString("&")
        }
    }

    // ============================================================
    // URL VALIDATION / NORMALIZATION
    // ============================================================

    fun isValidHttpUrl(
        url: String
    ): Boolean {

        val clean = url.trim()

        return clean.startsWith(
            "https://",
            ignoreCase = true
        ) ||
            clean.startsWith(
                "http://",
                ignoreCase = true
            )
    }

    fun normalizeUrl(
        url: String
    ): String {
        return url.trim()
    }

    fun getSafeVerificationBaseUrl(
        configuredUrl: String?
    ): String {

        val clean =
            configuredUrl
                ?.trim()
                .orEmpty()

        return if (
            clean.isNotBlank() &&
            isValidHttpUrl(clean)
        ) {
            clean
        } else {
            DEFAULT_BASE_VERIFICATION_URL
        }
    }

    fun getSafeWebAppUrl(
        configuredUrl: String?
    ): String {

        val clean =
            configuredUrl
                ?.trim()
                .orEmpty()

        return if (
            clean.isNotBlank() &&
            isValidHttpUrl(clean)
        ) {
            clean
        } else {
            DEFAULT_WEB_APP_URL
        }
    }

    // ============================================================
    // CSV
    // ============================================================

    /**
     * RFC 4180-compatible CSV escaping.
     */
    fun escapeCsv(
        value: String
    ): String {

        val escaped =
            value.replace(
                "\"",
                "\"\""
            )

        return "\"$escaped\""
    }

    fun escapeCsvNullable(
        value: String?
    ): String {
        return escapeCsv(
            value.orEmpty()
        )
    }

    /**
     * Small locale holder so ID generation doesn't depend on
     * the device's language/region.
     */
    private object LocaleHolder {
        val ROOT = java.util.Locale.ROOT
    }
}