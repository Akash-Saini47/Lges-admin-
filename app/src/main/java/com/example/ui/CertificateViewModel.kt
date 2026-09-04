package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.database.Certificate
import com.example.database.CertificateDatabase
import com.example.database.CertificateRepository
import com.example.database.GoogleSheetsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CertificateViewModel(application: Application) : AndroidViewModel(application) {

    // SharedPreferences for Web App URL
    private val sharedPrefs = application.getSharedPreferences("lges_admin_prefs", Context.MODE_PRIVATE)
    private val defaultUrl = "https://script.google.com/macros/s/AKfycbxb9VlwWNXkJwjt1927Ju1bKWzaRdbXUZVvaS6jbLBQ-l9NUudXRVfq9lNchthpcIlm0g/exec"

    private val repository: CertificateRepository
    val allCertificates: StateFlow<List<Certificate>>

    init {
        val database = CertificateDatabase.getDatabase(application)
        repository = CertificateRepository(database.certificateDao())
        allCertificates = repository.allCertificates.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        if (!sharedPrefs.contains("web_app_url")) {
            sharedPrefs.edit().putString("web_app_url", defaultUrl).apply()
        }
    }

    // Form Fields States
    val rollNo = MutableStateFlow("")
    val studentName = MutableStateFlow("")
    val relationPrefix = MutableStateFlow("S/O") // "S/O", "D/O", or "W/O"
    val fatherName = MutableStateFlow("")
    val courseName = MutableStateFlow("")
    val sessionRange = MutableStateFlow("")
    val duration = MutableStateFlow("")
    val grade = MutableStateFlow("A")
    val placeOfIssue = MutableStateFlow("")
    val dateOfIssue = MutableStateFlow(SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date()))
    val certType = MutableStateFlow("Course") // "Course" or "Internship"

    // Settings States
    val webAppUrl = MutableStateFlow(sharedPrefs.getString("web_app_url", null) ?: defaultUrl)
    val apiKey = MutableStateFlow(sharedPrefs.getString("api_key", "") ?: "")

    // Network Upload Status
    private val _isUploading = MutableStateFlow(false)
    val isUploading = _isUploading.asStateFlow()

    private val _uploadStatus = MutableStateFlow<String?>(null)
    val uploadStatus = _uploadStatus.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError = _uploadError.asStateFlow()

    fun updateWebAppUrl(url: String) {
        webAppUrl.value = url
        sharedPrefs.edit().putString("web_app_url", url).apply()
    }

    fun updateApiKey(key: String) {
        apiKey.value = key
        sharedPrefs.edit().putString("api_key", key).apply()
    }

    fun loadCertificateForEditing(cert: Certificate) {
        rollNo.value = cert.rollNo
        studentName.value = cert.studentName
        val rawFather = cert.fatherName.trim()
        when {
            rawFather.startsWith("S/O ", ignoreCase = true) -> {
                relationPrefix.value = "S/O"
                fatherName.value = rawFather.substring(4).trim()
            }
            rawFather.startsWith("D/O ", ignoreCase = true) -> {
                relationPrefix.value = "D/O"
                fatherName.value = rawFather.substring(4).trim()
            }
            rawFather.startsWith("W/O ", ignoreCase = true) -> {
                relationPrefix.value = "W/O"
                fatherName.value = rawFather.substring(4).trim()
            }
            else -> {
                fatherName.value = rawFather
            }
        }
        courseName.value = cert.courseName
        sessionRange.value = cert.sessionRange
        duration.value = cert.duration
        grade.value = cert.grade
        placeOfIssue.value = cert.placeOfIssue
        dateOfIssue.value = cert.dateOfIssue
        certType.value = cert.certType
    }

    fun clearForm() {
        rollNo.value = ""
        studentName.value = ""
        relationPrefix.value = "S/O"
        fatherName.value = ""
        courseName.value = ""
        sessionRange.value = ""
        duration.value = ""
        grade.value = "A"
        placeOfIssue.value = ""
        dateOfIssue.value = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        certType.value = "Course"
        clearUploadStatus()
    }

    fun clearUploadStatus() {
        _uploadStatus.value = null
        _uploadError.value = null
    }

    fun getAsCertificate(): Certificate {
        val rawName = fatherName.value.trim()
        val formattedFatherName = when {
            rawName.isEmpty() -> ""
            rawName.startsWith("S/O ", ignoreCase = true) ||
            rawName.startsWith("D/O ", ignoreCase = true) ||
            rawName.startsWith("W/O ", ignoreCase = true) -> rawName
            else -> "${relationPrefix.value} $rawName"
        }

        return Certificate(
            rollNo = rollNo.value.trim(),
            studentName = studentName.value.trim(),
            fatherName = formattedFatherName,
            courseName = courseName.value.trim(),
            sessionRange = sessionRange.value.trim(),
            duration = duration.value.trim(),
            grade = grade.value.trim(),
            placeOfIssue = placeOfIssue.value.trim(),
            dateOfIssue = dateOfIssue.value.trim(),
            certType = certType.value
        )
    }

    fun saveCertificateLocally(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val cert = getAsCertificate()
        if (cert.rollNo.isEmpty()) {
            onError("Roll No. / Certificate No. is required")
            return
        }
        if (cert.studentName.isEmpty()) {
            onError("Student Name is required")
            return
        }
        if (cert.courseName.isEmpty()) {
            onError("Course Name is required")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.insert(cert)
                viewModelScope.launch(Dispatchers.Main) {
                    onSuccess()
                }
                // Automatically trigger background cloud sync
                if (webAppUrl.value.isNotBlank()) {
                    uploadCertificateToSheets()
                }
            } catch (e: Exception) {
                viewModelScope.launch(Dispatchers.Main) {
                    onError("Failed to save locally: ${e.localizedMessage}")
                }
            }
        }
    }

    fun deleteCertificate(rollNo: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(rollNo)
        }
    }

    fun uploadCertificateToSheets() {
        val cert = getAsCertificate()
        if (cert.rollNo.isEmpty() || cert.studentName.isEmpty() || cert.courseName.isEmpty()) {
            _uploadError.value = "Please fill in Certificate No, Student Name, and Course Name first."
            return
        }

        _isUploading.value = true
        _uploadStatus.value = "Uploading to Google Sheets..."
        _uploadError.value = null

        viewModelScope.launch(Dispatchers.IO) {
            GoogleSheetsService.uploadCertificate(
                webAppUrl = webAppUrl.value.trim(),
                certificate = cert,
                apiKey = apiKey.value.trim(),
                onSuccess = { message ->
                    _isUploading.value = false
                    _uploadStatus.value = message
                    _uploadError.value = null
                    // Automatically save locally too when sheets upload is successful
                    viewModelScope.launch(Dispatchers.IO) {
                        repository.insert(cert)
                    }
                },
                onError = { error ->
                    _isUploading.value = false
                    _uploadStatus.value = null
                    _uploadError.value = error
                }
            )
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CertificateViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return CertificateViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
