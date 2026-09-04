package com.example.database

import com.example.util.AppLogger
import com.example.util.CertificateConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

sealed class SyncResult {

    data class Success(
        val action: String,
        val certificateId: String,
        val message: String
    ) : SyncResult()

    data class Error(
        val message: String,
        val cause: Throwable? = null,
        val isRetryable: Boolean = false
    ) : SyncResult()
}

object GoogleSheetsService {

    private const val TAG = "GoogleSheetsService"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val jsonMediaType =
        "application/json; charset=utf-8".toMediaType()

    /**
     * Prevents the same certificate from being uploaded concurrently.
     */
    private val uploadMutex = Mutex()
    private val inFlightUploads = mutableSetOf<String>()

    /**
     * Synchronizes one certificate with Google Sheets.
     */
    suspend fun syncCertificate(
        webAppUrl: String,
        certificate: Certificate,
        apiKey: String = "",
        action: String = "save",
        verificationBaseUrl: String = CertificateConfig.DEFAULT_BASE_VERIFICATION_URL
    ): SyncResult = withContext(Dispatchers.IO) {

        val certId = certificate.certificateId.trim()
        if (certId.isBlank()) {
            return@withContext SyncResult.Error("Certificate ID is missing.", isRetryable = false)
        }

        val cleanWebAppUrl = CertificateConfig.normalizeUrl(webAppUrl)
        if (!CertificateConfig.isValidHttpUrl(cleanWebAppUrl)) {
            return@withContext SyncResult.Error(
                "Invalid Google Sheets Web App URL. Please check Settings.",
                isRetryable = false
            )
        }

        if (certificate.studentName.isBlank()) {
            return@withContext SyncResult.Error("Student Name is required.", isRetryable = false)
        }

        if (certificate.courseName.isBlank()) {
            return@withContext SyncResult.Error("Course Name is required.", isRetryable = false)
        }

        val shouldProceed = uploadMutex.withLock {
            if (inFlightUploads.contains(certId)) {
                false
            } else {
                inFlightUploads.add(certId)
                true
            }
        }

        if (!shouldProceed) {
            return@withContext SyncResult.Error(
                "A sync operation for certificate '$certId' is already in progress.",
                isRetryable = true
            )
        }

        try {
            val timeStamp = SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
            ).format(Date(certificate.timestamp))

            val safeVerificationBaseUrl =
                CertificateConfig.getSafeVerificationBaseUrl(verificationBaseUrl)

            val verificationUrl = CertificateConfig.buildVerificationUrl(
                certificateId = certId,
                baseUrl = safeVerificationBaseUrl
            )

            val jsonPayload = JSONObject().apply {
                put("action", action.trim().ifBlank { "save" })
                put("certificateId", certId)
                put("rollNo", certificate.rollNo.trim())
                put("studentName", certificate.studentName.trim())
                put("name", certificate.studentName.trim()) // Apps Script compatibility
                put("fatherName", certificate.fatherName.trim())
                put("courseName", certificate.courseName.trim())
                put("course", certificate.courseName.trim()) // Apps Script compatibility
                put("certType", certificate.certType.trim())
                put("sessionRange", certificate.sessionRange.trim())
                put("duration", certificate.duration.trim())
                put("grade", certificate.grade.trim())
                put("placeOfIssue", certificate.placeOfIssue.trim())
                put("dateOfIssue", certificate.dateOfIssue.trim())
                put("timestamp", timeStamp)
                put("verificationUrl", verificationUrl)

                if (apiKey.isNotBlank()) {
                    put("apiKey", apiKey.trim())
                }
            }

            val requestBody = jsonPayload.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(cleanWebAppUrl)
                .post(requestBody)
                .build()

            AppLogger.d(TAG, "Executing sync request for certificateId: $certId")

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    val isRetryable = code >= 500 || code == 429
                    AppLogger.w(TAG, "Google Sheets sync HTTP Error $code for $certId")
                    return@withContext SyncResult.Error(
                        "Server HTTP Error $code: ${response.message}",
                        isRetryable = isRetryable
                    )
                }

                val responseBody = response.body?.string()?.trim().orEmpty()
                if (responseBody.isBlank()) {
                    return@withContext SyncResult.Error("Empty response received from Google Sheets.", isRetryable = true)
                }

                try {
                    val resultJson = JSONObject(responseBody)
                    val status = resultJson.optString("status")
                    val message = resultJson.optString("message")
                    val responseAction = resultJson.optString("action", action)
                    val returnedCertId = resultJson.optString("certificateId", certId).trim()

                    if (status.equals("success", ignoreCase = true)) {
                        if (returnedCertId.isNotBlank() && returnedCertId != certId) {
                            return@withContext SyncResult.Error(
                                "Server confirmed a different certificate ID. Expected '$certId' but got '$returnedCertId'.",
                                isRetryable = false
                            )
                        }

                        SyncResult.Success(
                            action = responseAction.ifBlank { action },
                            certificateId = certId,
                            message = message.ifBlank { "Cloud synchronization successful." }
                        )
                    } else {
                        AppLogger.w(TAG, "Google Sheets error status for $certId: $message")
                        SyncResult.Error(
                            message.ifBlank { "Google Sheets reported an error." },
                            isRetryable = false
                        )
                    }
                } catch (e: JSONException) {
                    AppLogger.e(TAG, "Invalid JSON from Sheets endpoint: ${e.message}")
                    SyncResult.Error("Invalid JSON received from server. Check Apps Script deployment.", e, isRetryable = false)
                }
            }
        } catch (e: SocketTimeoutException) {
            AppLogger.w(TAG, "Timeout syncing $certId: ${e.message}")
            SyncResult.Error("Network Timeout: Google Sheets took too long to respond.", e, isRetryable = true)
        } catch (e: UnknownHostException) {
            AppLogger.w(TAG, "DNS resolution failed for $certId: ${e.message}")
            SyncResult.Error("No Internet: Unable to reach Google Sheets.", e, isRetryable = true)
        } catch (e: IOException) {
            AppLogger.w(TAG, "IO error syncing $certId: ${e.message}")
            SyncResult.Error("Network connection error: ${e.localizedMessage ?: "Connection failed"}", e, isRetryable = true)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Unexpected error syncing $certId: ${e.message}", e)
            SyncResult.Error("Unexpected error: ${e.localizedMessage ?: "Sync failed"}", e, isRetryable = false)
        } finally {
            uploadMutex.withLock {
                inFlightUploads.remove(certId)
            }
        }
    }

    /**
     * Deletes one certificate from Google Sheets using certificateId as idempotency key.
     */
    suspend fun deleteCertificateRemotely(
        webAppUrl: String,
        certificateId: String,
        apiKey: String = ""
    ): SyncResult = withContext(Dispatchers.IO) {

        val cleanWebAppUrl = CertificateConfig.normalizeUrl(webAppUrl)
        val cleanCertId = certificateId.trim()

        if (!CertificateConfig.isValidHttpUrl(cleanWebAppUrl)) {
            return@withContext SyncResult.Error("Invalid Google Sheets Web App URL.", isRetryable = false)
        }

        if (cleanCertId.isBlank()) {
            return@withContext SyncResult.Error("Certificate ID is required for deletion.", isRetryable = false)
        }

        try {
            val jsonPayload = JSONObject().apply {
                put("action", "delete")
                put("certificateId", cleanCertId)
                if (apiKey.isNotBlank()) {
                    put("apiKey", apiKey.trim())
                }
            }

            val requestBody = jsonPayload.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(cleanWebAppUrl)
                .post(requestBody)
                .build()

            AppLogger.d(TAG, "Executing remote deletion request for: $cleanCertId")

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    return@withContext SyncResult.Error(
                        "HTTP Error $code: ${response.message}",
                        isRetryable = code >= 500 || code == 429
                    )
                }

                val responseBody = response.body?.string()?.trim().orEmpty()
                if (responseBody.isBlank()) {
                    return@withContext SyncResult.Error("Empty response received from cloud endpoint.", isRetryable = true)
                }

                try {
                    val resultJson = JSONObject(responseBody)
                    val status = resultJson.optString("status")
                    val message = resultJson.optString("message")
                    val returnedCertId = resultJson.optString("certificateId", cleanCertId).trim()

                    if (status.equals("success", ignoreCase = true)) {
                        if (returnedCertId.isNotBlank() && returnedCertId != cleanCertId) {
                            return@withContext SyncResult.Error(
                                "Server confirmed a different certificate ID during deletion.",
                                isRetryable = false
                            )
                        }

                        SyncResult.Success(
                            action = "deleted",
                            certificateId = cleanCertId,
                            message = message.ifBlank { "Deleted from Google Sheets." }
                        )
                    } else {
                        // If record was not found remotely, treat as successfully deleted
                        if (message.contains("not found", ignoreCase = true)) {
                            SyncResult.Success(
                                action = "deleted",
                                certificateId = cleanCertId,
                                message = "Record was not present in cloud (already deleted)."
                            )
                        } else {
                            SyncResult.Error(
                                message.ifBlank { "Failed to delete from Google Sheets." },
                                isRetryable = false
                            )
                        }
                    }
                } catch (e: JSONException) {
                    SyncResult.Error("Invalid JSON response received during cloud deletion.", e, isRetryable = false)
                }
            }
        } catch (e: SocketTimeoutException) {
            SyncResult.Error("Network Timeout: Deletion request timed out.", e, isRetryable = true)
        } catch (e: UnknownHostException) {
            SyncResult.Error("No Internet: Unable to connect for remote deletion.", e, isRetryable = true)
        } catch (e: IOException) {
            SyncResult.Error("Network error during cloud deletion: ${e.localizedMessage}", e, isRetryable = true)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Unexpected error deleting $cleanCertId: ${e.message}", e)
            SyncResult.Error("Cloud delete failed: ${e.localizedMessage ?: "Unexpected error"}", e, isRetryable = false)
        }
    }

    /**
     * Tests the Google Apps Script endpoint.
     */
    suspend fun testConnection(
        webAppUrl: String,
        apiKey: String = ""
    ): SyncResult = withContext(Dispatchers.IO) {

        val cleanWebAppUrl = CertificateConfig.normalizeUrl(webAppUrl)
        if (!CertificateConfig.isValidHttpUrl(cleanWebAppUrl)) {
            return@withContext SyncResult.Error(
                "Please enter a valid Google Apps Script Web App URL first.",
                isRetryable = false
            )
        }

        try {
            val jsonPayload = JSONObject().apply {
                put("action", "test")
                if (apiKey.isNotBlank()) {
                    put("apiKey", apiKey.trim())
                }
            }

            val requestBody = jsonPayload.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(cleanWebAppUrl)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext SyncResult.Error("Connection test failed (HTTP ${response.code}).")
                }

                val responseBody = response.body?.string()?.trim().orEmpty()
                if (responseBody.isBlank()) {
                    return@withContext SyncResult.Error("Endpoint returned an empty response.")
                }

                try {
                    val resultJson = JSONObject(responseBody)
                    val status = resultJson.optString("status")
                    val message = resultJson.optString("message")

                    if (status.equals("success", ignoreCase = true)) {
                        SyncResult.Success(
                            action = "test",
                            certificateId = "",
                            message = message.ifBlank { "Connection verified!" }
                        )
                    } else {
                        SyncResult.Error(message.ifBlank { "Endpoint returned an error status." })
                    }
                } catch (e: JSONException) {
                    SyncResult.Error("Connected, but endpoint returned invalid JSON. Check Apps Script deployment.", e)
                }
            }
        } catch (e: SocketTimeoutException) {
            SyncResult.Error("Connection test timed out.", e)
        } catch (e: UnknownHostException) {
            SyncResult.Error("Unable to resolve Google Sheets server address.", e)
        } catch (e: IOException) {
            SyncResult.Error("Network error: ${e.localizedMessage ?: "Connection failed."}", e)
        } catch (e: Exception) {
            SyncResult.Error("Unable to reach endpoint: ${e.localizedMessage ?: "Unknown error"}", e)
        }
    }
}