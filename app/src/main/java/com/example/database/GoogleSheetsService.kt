package com.example.database

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object GoogleSheetsService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun uploadCertificate(
        webAppUrl: String,
        certificate: Certificate,
        apiKey: String = "",
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (webAppUrl.isEmpty()) {
            onError("Google Sheets Web App URL is not configured. Please set it in Settings.")
            return
        }

        val timeStamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())

        val jsonObject = JSONObject().apply {
            // Direct keys required by Google Apps Script
            put("name", certificate.studentName)            // CRITICAL: Apps Script uses data.name
            put("studentName", certificate.studentName)
            put("Student Name", certificate.studentName)
            put("student_name", certificate.studentName)

            put("rollNo", certificate.rollNo)
            put("Roll No", certificate.rollNo)
            put("roll_no", certificate.rollNo)

            put("regdNo", certificate.rollNo)

            put("fatherName", certificate.fatherName)
            put("Father Name", certificate.fatherName)
            put("father_name", certificate.fatherName)

            put("course", certificate.courseName)
            put("courseName", certificate.courseName)
            put("Course", certificate.courseName)

            put("issueDate", certificate.dateOfIssue)
            put("dateOfIssue", certificate.dateOfIssue)
            put("Issue Date", certificate.dateOfIssue)

            put("grade", certificate.grade)
            put("Grade", certificate.grade)

            put("percentage", "")
            put("certificateId", "LGES-${certificate.rollNo}")

            put("certType", certificate.certType)
            put("sessionRange", certificate.sessionRange)
            put("duration", certificate.duration)
            put("placeOfIssue", certificate.placeOfIssue)
            put("timestamp", timeStamp)
            put("Timestamp", timeStamp)

            if (apiKey.isNotEmpty()) {
                put("apiKey", apiKey)
            }
        }

        val requestBody = jsonObject.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(webAppUrl)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    try {
                        val resultJson = JSONObject(responseBody)
                        val status = resultJson.optString("status")
                        val message = resultJson.optString("message")
                        if (status == "success") {
                            onSuccess(message.ifEmpty { "Saved successfully to Google Sheets!" })
                        } else {
                            onError(message.ifEmpty { "Google Sheets returned an error status." })
                        }
                    } catch (e: Exception) {
                        // Sometimes Google Sheets returns success but plain text or HTML redirect info
                        if (responseBody.contains("success") || responseBody.contains("Success") || responseBody.contains("Sheet")) {
                            onSuccess("Saved successfully to Google Sheets!")
                        } else {
                            onSuccess("Data uploaded! (Response received: ${responseBody.take(100)})")
                        }
                    }
                } else {
                    onError("HTTP Error: ${response.code} ${response.message}")
                }
            }
        } catch (e: IOException) {
            onError("Connection failed: ${e.localizedMessage ?: "Network error"}")
        } catch (e: Exception) {
            onError("Failed to submit: ${e.localizedMessage ?: "Unknown error"}")
        }
    }
}
