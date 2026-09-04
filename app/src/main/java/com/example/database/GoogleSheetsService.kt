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

    data class Success(
        val action: String,
        val certificateId: String,
        val message: String
    ) : SyncResult()

    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : SyncResult()
}

object GoogleSheetsService {

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
     * Prevents the same certificate from being uploaded
     * concurrently.
     */
    private val uploadMutex = Mutex()

    private val inFlightUploads =
        mutableSetOf<String>()

    /**
     * Synchronizes one certificate with Google Sheets.
     */
    suspend fun syncCertificate(
        webAppUrl: String,
        certificate: Certificate,
        apiKey: String = "",
        action: String = "save",
        verificationBaseUrl: String =
            CertificateConfig.DEFAULT_BASE_VERIFICATION_URL
    ): SyncResult = withContext(Dispatchers.IO) {

        val certId = certificate.certificateId
            .trim()
            .ifBlank {
                return@withContext SyncResult.Error(
                    "Certificate ID is missing."
                )
            }

        val cleanWebAppUrl =
            CertificateConfig.normalizeUrl(webAppUrl)

        if (
            cleanWebAppUrl.isBlank() ||
            !CertificateConfig.isValidHttpUrl(cleanWebAppUrl)
        ) {
            return@withContext SyncResult.Error(
                "Invalid Google Sheets Web App URL. " +
                    "Please check the URL in Settings."
            )
        }

        if (certificate.studentName.isBlank()) {
            return@withContext SyncResult.Error(
                "Student Name is required."
            )
        }

        if (certificate.courseName.isBlank()) {
            return@withContext SyncResult.Error(
                "Course Name is required."
            )
        }

        /*
         * Prevent duplicate simultaneous requests for the same
         * certificate.
         */
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
                "A sync operation for certificate '$certId' " +
                    "is already running. Please wait."
            )
        }

        try {

            val timeStamp =
                SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()
                ).format(
                    Date(certificate.timestamp)
                )

            val safeVerificationBaseUrl =
                CertificateConfig.getSafeVerificationBaseUrl(
                    verificationBaseUrl
                )

            val verificationUrl =
                CertificateConfig.buildVerificationUrl(
                    certificateId = certId,
                    baseUrl = safeVerificationBaseUrl
                )

            val jsonPayload = JSONObject().apply {

                put("action", action.trim().ifBlank { "save" })

                put("certificateId", certId)

                put(
                    "rollNo",
                    certificate.rollNo.trim()
                )

                put(
                    "studentName",
                    certificate.studentName.trim()
                )

                /*
                 * Kept for compatibility with existing Apps Script.
                 */
                put(
                    "name",
                    certificate.studentName.trim()
                )

                put(
                    "fatherName",
                    certificate.fatherName.trim()
                )

                put(
                    "courseName",
                    certificate.courseName.trim()
                )

                /*
                 * Compatibility field.
                 */
                put(
                    "course",
                    certificate.courseName.trim()
                )

                put(
                    "certType",
                    certificate.certType.trim()
                )

                put(
                    "sessionRange",
                    certificate.sessionRange.trim()
                )

                put(
                    "duration",
                    certificate.duration.trim()
                )

                put(
                    "grade",
                    certificate.grade.trim()
                )

                put(
                    "placeOfIssue",
                    certificate.placeOfIssue.trim()
                )

                put(
                    "dateOfIssue",
                    certificate.dateOfIssue.trim()
                )

                put(
                    "timestamp",
                    timeStamp
                )

                put(
                    "verificationUrl",
                    verificationUrl
                )

                if (apiKey.isNotBlank()) {
                    put(
                        "apiKey",
                        apiKey.trim()
                    )
                }
            }

            val requestBody =
                jsonPayload
                    .toString()
                    .toRequestBody(jsonMediaType)

            val request =
                Request.Builder()
                    .url(cleanWebAppUrl)
                    .post(requestBody)
                    .build()

            client.newCall(request)
                .execute()
                .use { response ->

                    if (!response.isSuccessful) {
                        return@withContext SyncResult.Error(
                            "HTTP Server Error ${response.code}: " +
                                response.message
                        )
                    }

                    val responseBody =
                        response.body?.string()?.trim().orEmpty()

                    if (responseBody.isBlank()) {
                        return@withContext SyncResult.Error(
                            "Empty response received from " +
                                "Google Sheets."
                        )
                    }

                    try {

                        val resultJson =
                            JSONObject(responseBody)

                        val status =
                            resultJson.optString("status")

                        val message =
                            resultJson.optString("message")

                        val responseAction =
                            resultJson.optString(
                                "action",
                                action
                            )

                        val returnedCertId =
                            resultJson.optString(
                                "certificateId",
                                certId
                            ).trim()

                        /*
                         * Strict success validation.
                         */
                        if (
                            status.equals(
                                "success",
                                ignoreCase = true
                            )
                        ) {

                            /*
                             * Prevent the server from accidentally
                             * confirming a different certificate ID.
                             */
                            if (
                                returnedCertId.isNotBlank() &&
                                returnedCertId != certId
                            ) {
                                return@withContext SyncResult.Error(
                                    "Server returned a different " +
                                        "certificate ID. Expected " +
                                        "'$certId' but received " +
                                        "'$returnedCertId'."
                                )
                            }

                            SyncResult.Success(
                                action =
                                    responseAction.ifBlank {
                                        action
                                    },
                                certificateId = certId,
                                message =
                                    message.ifBlank {
                                        "Cloud synchronization successful."
                                    }
                            )

                        } else {

                            SyncResult.Error(
                                message.ifBlank {
                                    "Google Sheets reported a " +
                                        "server error."
                                }
                            )
                        }

                    } catch (e: JSONException) {

                        SyncResult.Error(
                            "Invalid response from Google Sheets " +
                                "server. The endpoint did not return " +
                                "valid JSON.",
                            e
                        )
                    }
                }

        } catch (e: SocketTimeoutException) {

            SyncResult.Error(
                "Network Timeout: Google Sheets took too long " +
                    "to respond.",
                e
            )

        } catch (e: UnknownHostException) {

            SyncResult.Error(
                "No Internet: Unable to connect to Google Sheets.",
                e
            )

        } catch (e: IOException) {

            SyncResult.Error(
                "Network Error: " +
                    (e.localizedMessage
                        ?: "Connection failed."),
                e
            )

        } catch (e: Exception) {

            SyncResult.Error(
                "Unexpected Error: " +
                    (e.localizedMessage
                        ?: "Sync failed."),
                e
            )

        } finally {

            uploadMutex.withLock {
                inFlightUploads.remove(certId)
            }
        }
    }

    /**
     * Deletes one certificate from Google Sheets.
     */
    suspend fun deleteCertificateRemotely(
        webAppUrl: String,
        certificateId: String,
        apiKey: String = ""
    ): SyncResult = withContext(Dispatchers.IO) {

        val cleanWebAppUrl =
            CertificateConfig.normalizeUrl(webAppUrl)

        val cleanCertificateId =
            certificateId.trim()

        if (
            cleanWebAppUrl.isBlank() ||
            !CertificateConfig.isValidHttpUrl(
                cleanWebAppUrl
            )
        ) {
            return@withContext SyncResult.Error(
                "Invalid Google Sheets Web App URL."
            )
        }

        if (cleanCertificateId.isBlank()) {
            return@withContext SyncResult.Error(
                "Certificate ID is required for deletion."
            )
        }

        try {

            val jsonPayload =
                JSONObject().apply {

                    put("action", "delete")

                    put(
                        "certificateId",
                        cleanCertificateId
                    )

                    if (apiKey.isNotBlank()) {
                        put(
                            "apiKey",
                            apiKey.trim()
                        )
                    }
                }

            val requestBody =
                jsonPayload
                    .toString()
                    .toRequestBody(jsonMediaType)

            val request =
                Request.Builder()
                    .url(cleanWebAppUrl)
                    .post(requestBody)
                    .build()

            client.newCall(request)
                .execute()
                .use { response ->

                    if (!response.isSuccessful) {
                        return@withContext SyncResult.Error(
                            "HTTP Error ${response.code}: " +
                                response.message
                        )
                    }

                    val responseBody =
                        response.body?.string()
                            ?.trim()
                            .orEmpty()

                    if (responseBody.isBlank()) {
                        return@withContext SyncResult.Error(
                            "Empty response received from " +
                                "cloud endpoint."
                        )
                    }

                    try {

                        val resultJson =
                            JSONObject(responseBody)

                        val status =
                            resultJson.optString("status")

                        val message =
                            resultJson.optString("message")

                        val returnedCertId =
                            resultJson.optString(
                                "certificateId",
                                cleanCertificateId
                            ).trim()

                        if (
                            status.equals(
                                "success",
                                ignoreCase = true
                            )
                        ) {

                            if (
                                returnedCertId.isNotBlank() &&
                                returnedCertId != cleanCertificateId
                            ) {
                                return@withContext SyncResult.Error(
                                    "Server returned a different " +
                                        "certificate ID during deletion."
                                )
                            }

                            SyncResult.Success(
                                action = "deleted",
                                certificateId =
                                    cleanCertificateId,
                                message =
                                    message.ifBlank {
                                        "Deleted from Google Sheets."
                                    }
                            )

                        } else {

                            SyncResult.Error(
                                message.ifBlank {
                                    "Failed to delete from " +
                                        "Google Sheets."
                                }
                            )
                        }

                    } catch (e: JSONException) {

                        SyncResult.Error(
                            "Invalid JSON response received " +
                                "during cloud deletion.",
                            e
                        )
                    }
                }

        } catch (e: Exception) {

            SyncResult.Error(
                "Cloud delete failed: " +
                    (e.localizedMessage
                        ?: "Network error"),
                e
            )
        }
    }

    /**
     * Tests the Google Apps Script endpoint.
     */
    suspend fun testConnection(
        webAppUrl: String,
        apiKey: String = ""
    ): SyncResult = withContext(Dispatchers.IO) {

        val cleanWebAppUrl =
            CertificateConfig.normalizeUrl(webAppUrl)

        if (
            cleanWebAppUrl.isBlank() ||
            !CertificateConfig.isValidHttpUrl(
                cleanWebAppUrl
            )
        ) {
            return@withContext SyncResult.Error(
                "Please enter a valid Google Apps Script " +
                    "Web App URL first."
            )
        }

        try {

            val jsonPayload =
                JSONObject().apply {

                    put("action", "test")

                    if (apiKey.isNotBlank()) {
                        put(
                            "apiKey",
                            apiKey.trim()
                        )
                    }
                }

            val requestBody =
                jsonPayload
                    .toString()
                    .toRequestBody(jsonMediaType)

            val request =
                Request.Builder()
                    .url(cleanWebAppUrl)
                    .post(requestBody)
                    .build()

            client.newCall(request)
                .execute()
                .use { response ->

                    if (!response.isSuccessful) {
                        return@withContext SyncResult.Error(
                            "Connection test failed " +
                                "(HTTP ${response.code})."
                        )
                    }

                    val responseBody =
                        response.body?.string()
                            ?.trim()
                            .orEmpty()

                    if (responseBody.isBlank()) {
                        return@withContext SyncResult.Error(
                            "Endpoint returned an empty response."
                        )
                    }

                    try {

                        val resultJson =
                            JSONObject(responseBody)

                        val status =
                            resultJson.optString("status")

                        val message =
                            resultJson.optString("message")

                        if (
                            status.equals(
                                "success",
                                ignoreCase = true
                            )
                        ) {

                            SyncResult.Success(
                                action = "test",
                                certificateId = "",
                                message =
                                    message.ifBlank {
                                        "Connection verified!"
                                    }
                            )

                        } else {

                            SyncResult.Error(
                                message.ifBlank {
                                    "Endpoint returned an " +
                                        "error status."
                                }
                            )
                        }

                    } catch (e: JSONException) {

                        SyncResult.Error(
                            "Connected, but the endpoint returned " +
                                "invalid JSON. Check Apps Script " +
                                "deployment and permissions.",
                            e
                        )
                    }
                }

        } catch (e: SocketTimeoutException) {

            SyncResult.Error(
                "Connection test timed out.",
                e
            )

        } catch (e: UnknownHostException) {

            SyncResult.Error(
                "Unable to resolve the Google Sheets server.",
                e
            )

        } catch (e: IOException) {

            SyncResult.Error(
                "Network Error: " +
                    (e.localizedMessage
                        ?: "Connection failed."),
                e
            )

        } catch (e: Exception) {

            SyncResult.Error(
                "Unable to reach endpoint: " +
                    (e.localizedMessage
                        ?: "Network error"),
                e
            )
        }
    }
}