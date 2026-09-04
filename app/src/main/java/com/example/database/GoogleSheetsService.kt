package com.example.database

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
    data class Success(val action: String, val certificateId: String, val message: String) : SyncResult()
    data class Error(val message: String, val cause: Throwable? = null) : SyncResult()
}

object GoogleSheetsService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    // Mutex to protect simultaneous upload state and prevent race conditions
    private val uploadMutex = Mutex()
    private val inFlightUploads = mutableSetOf<String>()

    /**
     * Synchronizes a certificate to Google Sheets (Upsert / Create / Update).
     * Strictly validates server response; NEVER treats HTML, redirects, or malformed JSON as success.
     */
    suspend fun syncCertificate(
        webAppUrl: String,
        certificate: Certificate,
        apiKey: String = "",
        action: String = "save",
        verificationBaseUrl: String = CertificateConfig.DEFAULT_BASE_VERIFICATION_URL
    ): SyncResult = withContext(Dispatchers.IO) {
        val certId = certificate.certificateId.ifBlank {
            CertificateConfig.computeCertificateId(certificate.rollNo)
        }

        if (webAppUrl.isBlank()) {
            return@withContext SyncResult.Error("Google Sheets Web App URL is not configured. Please set it in Settings.")
        }

        if (certId.isBlank() || certificate.studentName.isBlank() || certificate.courseName.isBlank()) {
            return@withContext SyncResult.Error("Validation Error: Certificate ID, Student Name, and Course are required.")
        }

        // Deduplication Guard: Check if an upload for this certificate is already in-flight
        val shouldProceed = uploadMutex.withLock {
            if (inFlightUploads.contains(certId)) {
                false
            } else {
                inFlightUploads.add(certId)
                true
            }
        }

        if (!shouldProceed) {
            return@withContext SyncResult.Error("A sync operation for certificate '$certId' is already running. Please wait.")
        }

        try {
            val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(certificate.timestamp))
            val verificationUrl = CertificateConfig.buildVerificationUrl(certId, verificationBaseUrl)

            val jsonPayload = JSONObject().apply {
                put("action", action)
                put("certificateId", certId)
                put("rollNo", certificate.rollNo)
                put("studentName", certificate.studentName)
                put("name", certificate.studentName)
                put("fatherName", certificate.fatherName)
                put("courseName", certificate.courseName)
                put("course", certificate.courseName)
                put("certType", certificate.certType)
                put("sessionRange", certificate.sessionRange)
                put("duration", certificate.duration)
                put("grade", certificate.grade)
                put("placeOfIssue", certificate.placeOfIssue)
                put("dateOfIssue", certificate.dateOfIssue)
                put("timestamp", timeStamp)
                put("verificationUrl", verificationUrl)
                if (apiKey.isNotBlank()) {
                    put("apiKey", apiKey.trim())
                }
            }

            val requestBody = jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(webAppUrl.trim())
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext SyncResult.Error("HTTP Server Error ${response.code}: ${response.message}")
                }

                val responseBody = response.body?.string() ?: ""
                if (responseBody.isBlank()) {
                    return@withContext SyncResult.Error("Empty response received from Google Apps Script.")
                }

                // Parse strict JSON - do NOT treat HTML error pages as success
                try {
                    val resultJson = JSONObject(responseBody)
                    val status = resultJson.optString("status")
                    val message = resultJson.optString("message")
                    val resAction = resultJson.optString("action", action)
                    val returnedCertId = resultJson.optString("certificateId", certId)

                    if (status.equals("success", ignoreCase = true)) {
                        SyncResult.Success(
                            action = resAction,
                            certificateId = returnedCertId,
                            message = message.ifBlank { "Cloud synchronization successful." }
                        )
                    } else {
                        SyncResult.Error(message.ifBlank { "Google Sheets reported a server error." })
                    }
                } catch (e: JSONException) {
                    // Critical Protection against False Success:
                    // Google Apps Script redirects or permission errors return HTML (e.g. "<!DOCTYPE html>... Google Drive")
                    SyncResult.Error(
                        "Invalid response from Google Sheets server. Please ensure Apps Script is deployed with " +
                                "'Execute as: Me' and 'Who has access: Anyone'."
                    )
                }
            }
        } catch (e: SocketTimeoutException) {
            SyncResult.Error("Network Timeout: Google Sheets took too long to respond.", e)
        } catch (e: UnknownHostException) {
            SyncResult.Error("No Internet: Unable to connect to Google Sheets server.", e)
        } catch (e: IOException) {
            SyncResult.Error("Network Error: ${e.localizedMessage ?: "Connection failed."}", e)
        } catch (e: Exception) {
            SyncResult.Error("Unexpected Error: ${e.localizedMessage ?: "Sync failed."}", e)
        } finally {
            uploadMutex.withLock {
                inFlightUploads.remove(certId)
            }
        }
    }

    /**
     * Deletes a certificate from Google Sheets cloud registry.
     */
    suspend fun deleteCertificateRemotely(
        webAppUrl: String,
        certificateId: String,
        apiKey: String = ""
    ): SyncResult = withContext(Dispatchers.IO) {
        if (webAppUrl.isBlank()) {
            return@withContext SyncResult.Error("Google Sheets Web App URL is not configured.")
        }
        if (certificateId.isBlank()) {
            return@withContext SyncResult.Error("Certificate ID is required for deletion.")
        }

        try {
            val jsonPayload = JSONObject().apply {
                put("action", "delete")
                put("certificateId", certificateId.trim())
                if (apiKey.isNotBlank()) {
                    put("apiKey", apiKey.trim())
                }
            }

            val requestBody = jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(webAppUrl.trim())
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext SyncResult.Error("HTTP Error ${response.code}: ${response.message}")
                }
                val responseBody = response.body?.string() ?: ""
                try {
                    val resultJson = JSONObject(responseBody)
                    val status = resultJson.optString("status")
                    val message = resultJson.optString("message")
                    if (status.equals("success", ignoreCase = true)) {
                        SyncResult.Success("deleted", certificateId, message.ifBlank { "Deleted from Google Sheets." })
                    } else {
                        SyncResult.Error(message.ifBlank { "Failed to delete from Google Sheets." })
                    }
                } catch (e: JSONException) {
                    SyncResult.Error("Invalid response received from cloud endpoint during deletion.")
                }
            }
        } catch (e: Exception) {
            SyncResult.Error("Cloud delete failed: ${e.localizedMessage ?: "Network error"}", e)
        }
    }

    /**
     * Tests connectivity to the Google Apps Script Web App endpoint.
     */
    suspend fun testConnection(webAppUrl: String, apiKey: String = ""): SyncResult = withContext(Dispatchers.IO) {
        if (webAppUrl.isBlank()) {
            return@withContext SyncResult.Error("Please enter a Google Apps Script Web App URL first.")
        }
        try {
            val jsonPayload = JSONObject().apply {
                put("action", "test")
                if (apiKey.isNotBlank()) {
                    put("apiKey", apiKey.trim())
                }
            }
            val requestBody = jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(webAppUrl.trim())
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext SyncResult.Error("Connection test failed (HTTP ${response.code}).")
                }
                val responseBody = response.body?.string() ?: ""
                try {
                    val resultJson = JSONObject(responseBody)
                    val status = resultJson.optString("status")
                    val message = resultJson.optString("message")
                    if (status.equals("success", ignoreCase = true)) {
                        SyncResult.Success("test", "", message.ifBlank { "Connection verified!" })
                    } else {
                        SyncResult.Error(message.ifBlank { "Endpoint returned an error status." })
                    }
                } catch (e: JSONException) {
                    SyncResult.Error("Connected, but endpoint returned non-JSON. Check Apps Script permissions.")
                }
            }
        } catch (e: Exception) {
            SyncResult.Error("Unable to reach endpoint: ${e.localizedMessage ?: "Network error"}", e)
        }
    }
}
