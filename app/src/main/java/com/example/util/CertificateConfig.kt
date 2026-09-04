package com.example.util

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object CertificateConfig {

    // Default institutional verification endpoints
    const val DEFAULT_BASE_VERIFICATION_URL = "https://lges-computer-classes.netlify.app/verify.html"
    const val DEFAULT_INSTITUTE_WEBSITE = "https://lges-computer-classes.netlify.app/"
    const val DEFAULT_WEB_APP_URL = "https://script.google.com/macros/s/AKfycbxb9VlwWNXkJwjt1927Ju1bKWzaRdbXUZVvaS6jbLBQ-l9NUudXRVfq9lNchthpcIlm0g/exec"

    /**
     * Deterministically generates a standardized unique Certificate ID from a roll number.
     * E.g. "101" -> "LGES-101", "LGES-101" -> "LGES-101"
     */
    fun computeCertificateId(rollNo: String): String {
        val clean = rollNo.trim()
        if (clean.isEmpty()) return ""
        return if (clean.startsWith("LGES-", ignoreCase = true)) {
            "LGES-${clean.substring(5).trim()}"
        } else {
            "LGES-$clean"
        }
    }

    /**
     * Constructs the official verification link encoded with the unique certificate ID.
     */
    fun buildVerificationUrl(certificateId: String, baseUrl: String = DEFAULT_BASE_VERIFICATION_URL): String {
        if (certificateId.isBlank()) return baseUrl
        val cleanBase = baseUrl.trim()
        val separator = if (cleanBase.contains("?")) "&" else "?"
        val encodedId = URLEncoder.encode(certificateId.trim(), StandardCharsets.UTF_8.toString())
        return "$cleanBase${separator}certNo=$encodedId"
    }

    /**
     * Safely escapes a string for RFC 4180 compliant CSV output.
     * Wraps in quotes and escapes internal quotes by doubling them ("" -> """").
     */
    fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}
