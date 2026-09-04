package com.example.util

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Central configuration and utility functions for certificate generation,
 * verification URLs, certificate IDs and CSV export.
 *
 * IMPORTANT:
 * Keep the public constant/function names unchanged because other parts
 * of the application depend on them.
 */
object CertificateConfig {

    // ============================================================
    // DEFAULT CONFIGURATION
    // ============================================================

    /**
     * Public certificate verification page.
     *
     * Example:
     * https://lges-computer-classes.netlify.app/verify.html?certNo=LGES-101
     */
    const val DEFAULT_BASE_VERIFICATION_URL =
        "https://lges-computer-classes.netlify.app/verify.html"

    /**
     * Main institute website.
     */
    const val DEFAULT_INSTITUTE_WEBSITE =
        "https://lges-computer-classes.netlify.app/"

    /**
     * Google Apps Script Web App endpoint.
     *
     * This should normally be configurable from Settings rather than
     * hard-coded in production. It is retained here for backward
     * compatibility with the current application.
     */
    const val DEFAULT_WEB_APP_URL =
        "https://script.google.com/macros/s/AKfycbxb9VlwWNXkJwjt1927Ju1bKWzaRdbXUZVvaS6jbLBQ-l9NUudXRVfq9lNchthpcIlm0g/exec"

    // ============================================================
    // CERTIFICATE ID
    // ============================================================

    private const val CERTIFICATE_PREFIX = "LGES-"

    /**
     * Creates the canonical certificate ID.
     *
     * Examples:
     *
     * "101"             -> "LGES-101"
     * " 101 "           -> "LGES-101"
     * "LGES-101"        -> "LGES-101"
     * "lges-101"        -> "LGES-101"
     * " LGES-101 "      -> "LGES-101"
     * "LGES-LGES-101"   -> "LGES-101"
     *
     * Blank input returns an empty string.
     */
    fun computeCertificateId(rollNo: String): String {
        var clean = rollNo.trim()

        if (clean.isBlank()) {
            return ""
        }

        /*
         * Remove repeated LGES- prefixes.
         *
         * This prevents malformed IDs such as:
         * LGES-LGES-101
         * lges-LGES-101
         * LGES-lgES-101
         */
        while (clean.startsWith(CERTIFICATE_PREFIX, ignoreCase = true)) {
            clean = clean.substring(CERTIFICATE_PREFIX.length).trim()
        }

        if (clean.isBlank()) {
            return ""
        }

        return CERTIFICATE_PREFIX + clean
    }

    /**
     * Checks whether a string already represents a valid LGES-style
     * certificate ID.
     */
    fun isCertificateId(value: String): Boolean {
        val clean = value.trim()

        if (clean.isBlank()) {
            return false
        }

        return clean.startsWith(
            CERTIFICATE_PREFIX,
            ignoreCase = true
        ) && clean.substring(CERTIFICATE_PREFIX.length).trim().isNotBlank()
    }

    // ============================================================
    // VERIFICATION URL
    // ============================================================

    /**
     * Builds the public verification URL for a certificate.
     *
     * Example:
     *
     * certificateId = "LGES-101"
     *
     * result:
     * https://lges-computer-classes.netlify.app/verify.html?certNo=LGES-101
     *
     * Existing certNo parameters are replaced instead of duplicated.
     * URL fragments are preserved correctly.
     */
    fun buildVerificationUrl(
        certificateId: String,
        baseUrl: String = DEFAULT_BASE_VERIFICATION_URL
    ): String {

        val cleanBase = baseUrl.trim()

        /*
         * If there is no certificate ID, return the cleaned base URL.
         */
        if (certificateId.isBlank()) {
            return cleanBase
        }

        /*
         * Empty base URL should fall back to the application default.
         */
        val safeBase =
            if (cleanBase.isBlank()) {
                DEFAULT_BASE_VERIFICATION_URL
            } else {
                cleanBase
            }

        val encodedId = encodeQueryParameter(
            computeCertificateId(certificateId)
        )

        /*
         * Separate fragment because query parameters must appear
         * before '#fragment'.
         *
         * Example:
         *
         * verify.html#top
         *
         * must become:
         *
         * verify.html?certNo=LGES-101#top
         */
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

        /*
         * Remove an existing certNo query parameter so that we don't
         * generate:
         *
         * ?certNo=OLD&certNo=NEW
         *
         * or:
         *
         * ?foo=1&certNo=OLD&certNo=NEW
         */
        val cleanedQueryUrl = removeQueryParameter(
            urlWithoutFragment,
            "certNo"
        )

        val separator =
            when {
                cleanedQueryUrl.contains('?') &&
                        !cleanedQueryUrl.endsWith('?') &&
                        !cleanedQueryUrl.endsWith('&') -> "&"

                cleanedQueryUrl.endsWith('?') ||
                        cleanedQueryUrl.endsWith('&') -> ""

                else -> "?"
            }

        return "$cleanedQueryUrl${separator}certNo=$encodedId$fragment"
    }

    /**
     * URL-encodes a query parameter using UTF-8.
     */
    private fun encodeQueryParameter(value: String): String {
        return URLEncoder.encode(
            value,
            StandardCharsets.UTF_8.name()
        )
    }

    /**
     * Removes one or more occurrences of a query parameter.
     *
     * This deliberately works without Android Uri so CertificateConfig
     * remains easy to unit-test as a normal Kotlin/JVM utility.
     */
    private fun removeQueryParameter(
        url: String,
        parameterName: String
    ): String {

        val questionMarkIndex = url.indexOf('?')

        if (questionMarkIndex < 0) {
            return url
        }

        val basePart = url.substring(0, questionMarkIndex)
        val queryPart = url.substring(questionMarkIndex + 1)

        if (queryPart.isBlank()) {
            return basePart
        }

        val filteredParameters = queryPart
            .split('&')
            .filter { parameter ->
                val equalsIndex = parameter.indexOf('=')

                val key =
                    if (equalsIndex >= 0) {
                        parameter.substring(0, equalsIndex)
                    } else {
                        parameter
                    }

                !key.equals(
                    parameterName,
                    ignoreCase = true
                )
            }

        return if (filteredParameters.isEmpty()) {
            basePart
        } else {
            "$basePart?${filteredParameters.joinToString("&")}"
        }
    }

    // ============================================================
    // URL VALIDATION / NORMALIZATION
    // ============================================================

    /**
     * Basic validation for URLs used by the application.
     *
     * This intentionally performs lightweight validation instead of
     * attempting a network request.
     */
    fun isValidHttpUrl(url: String): Boolean {
        val clean = url.trim()

        return clean.startsWith(
            "https://",
            ignoreCase = true
        ) || clean.startsWith(
            "http://",
            ignoreCase = true
        )
    }

    /**
     * Normalizes a configured URL.
     *
     * - trims whitespace
     * - removes unnecessary trailing whitespace
     * - preserves query strings/fragments
     *
     * No aggressive slash manipulation is performed because changing
     * slashes can break paths or deployed endpoints.
     */
    fun normalizeUrl(url: String): String {
        return url.trim()
    }

    /**
     * Returns the configured verification URL or the application default
     * when the supplied URL is blank.
     */
    fun getSafeVerificationBaseUrl(
        configuredUrl: String?
    ): String {
        val clean = configuredUrl?.trim().orEmpty()

        return if (
            clean.isNotBlank() &&
            isValidHttpUrl(clean)
        ) {
            clean
        } else {
            DEFAULT_BASE_VERIFICATION_URL
        }
    }

    /**
     * Returns the configured Google Apps Script URL or the default endpoint.
     */
    fun getSafeWebAppUrl(
        configuredUrl: String?
    ): String {
        val clean = configuredUrl?.trim().orEmpty()

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
     *
     * Example:
     *
     * John Smith
     * ->
     * "John Smith"
     *
     * John "Rocky" Smith
     * ->
     * "John ""Rocky"" Smith"
     */
    fun escapeCsv(value: String): String {
        val escaped = value.replace(
            "\"",
            "\"\""
        )

        return "\"$escaped\""
    }

    /**
     * Null-safe CSV escaping helper.
     */
    fun escapeCsvNullable(value: String?): String {
        return escapeCsv(value.orEmpty())
    }
}