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
import com.example.database.SyncResult
import com.example.util.CertificateConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FormValidationErrors(
    val rollNoError: String? = null,
    val studentNameError: String? = null,
    val fatherNameError: String? = null,
    val courseNameError: String? = null,
    val dateOfIssueError: String? = null
) {
    val hasErrors: Boolean
        get() = rollNoError != null || studentNameError != null || fatherNameError != null ||
                courseNameError != null || dateOfIssueError != null
}

class CertificateViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("lges_admin_prefs", Context.MODE_PRIVATE)
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
    }

    // Form Field States
    val rollNo = MutableStateFlow("")
    val studentName = MutableStateFlow("")
    val relationPrefix = MutableStateFlow("S/O") // "S/O", "D/O", or "W/O"
    val fatherName = MutableStateFlow("")
    val courseName = MutableStateFlow("")
    val sessionRange = MutableStateFlow("")
    val duration = MutableStateFlow("")
    val grade = MutableStateFlow("A")
    val placeOfIssue = MutableStateFlow("CHAMBA")
    val dateOfIssue = MutableStateFlow(SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date()))
    val certType = MutableStateFlow("Course") // "Course" or "Internship"

    // Validation & Warning States
    private val _validationErrors = MutableStateFlow(FormValidationErrors())
    val validationErrors = _validationErrors.asStateFlow()

    private val _duplicateNote = MutableStateFlow<String?>(null)
    val duplicateNote = _duplicateNote.asStateFlow()

    // Settings States
    val webAppUrl = MutableStateFlow(sharedPrefs.getString("web_app_url", CertificateConfig.DEFAULT_WEB_APP_URL) ?: CertificateConfig.DEFAULT_WEB_APP_URL)
    val apiKey = MutableStateFlow(sharedPrefs.getString("api_key", "") ?: "")
    val verificationBaseUrl = MutableStateFlow(sharedPrefs.getString("verification_base_url", CertificateConfig.DEFAULT_BASE_VERIFICATION_URL) ?: CertificateConfig.DEFAULT_BASE_VERIFICATION_URL)

    // Network & Sync States
    private val _isUploading = MutableStateFlow(false)
    val isUploading = _isUploading.asStateFlow()

    private val _uploadStatus = MutableStateFlow<String?>(null)
    val uploadStatus = _uploadStatus.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError = _uploadError.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection = _isTestingConnection.asStateFlow()

    fun updateWebAppUrl(url: String) {
        val clean = url.trim()
        webAppUrl.value = clean
        sharedPrefs.edit().putString("web_app_url", clean).apply()
    }

    fun updateApiKey(key: String) {
        val clean = key.trim()
        apiKey.value = clean
        sharedPrefs.edit().putString("api_key", clean).apply()
    }

    fun updateVerificationBaseUrl(url: String) {
        val clean = url.trim()
        verificationBaseUrl.value = clean
        sharedPrefs.edit().putString("verification_base_url", clean).apply()
    }

    fun resetSettingsToDefault() {
        updateWebAppUrl(CertificateConfig.DEFAULT_WEB_APP_URL)
        updateApiKey("")
        updateVerificationBaseUrl(CertificateConfig.DEFAULT_BASE_VERIFICATION_URL)
    }

    fun checkForDuplicateRollNo(inputRoll: String) {
        val clean = inputRoll.trim()
        if (clean.isBlank()) {
            _duplicateNote.value = null
            return
        }
        val certId = CertificateConfig.computeCertificateId(clean)
        val existing = allCertificates.value.firstOrNull {
            it.rollNo.equals(clean, ignoreCase = true) || it.certificateId.equals(certId, ignoreCase = true)
        }
        if (existing != null) {
            _duplicateNote.value = "Notice: Record exists for '${existing.studentName}' ($certId). Saving will update the existing entry."
        } else {
            _duplicateNote.value = null
        }
    }

    fun validateForm(): Boolean {
        var rErr: String? = null
        var nErr: String? = null
        var fErr: String? = null
        var cErr: String? = null
        var dErr: String? = null

        val r = rollNo.value.trim()
        val s = studentName.value.trim()
        val f = fatherName.value.trim()
        val c = courseName.value.trim()
        val d = dateOfIssue.value.trim()

        if (r.isEmpty()) {
            rErr = "Roll No. / Certificate No. is required"
        }
        if (s.isEmpty()) {
            nErr = "Student Name is required"
        }
        if (certType.value == "Course" && f.isEmpty()) {
            fErr = "Father / Guardian Name is required for Course certificates"
        }
        if (c.isEmpty()) {
            cErr = "Course / Internship title is required"
        }
        if (d.isEmpty()) {
            dErr = "Date of Issue is required"
        }

        val errors = FormValidationErrors(rErr, nErr, fErr, cErr, dErr)
        _validationErrors.value = errors
        return !errors.hasErrors
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
        grade.value = cert.grade.ifBlank { "A" }
        placeOfIssue.value = cert.placeOfIssue.ifBlank { "CHAMBA" }
        dateOfIssue.value = cert.dateOfIssue
        certType.value = cert.certType

        _validationErrors.value = FormValidationErrors()
        _duplicateNote.value = "Editing existing certificate ${cert.certificateId}"
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
        placeOfIssue.value = "CHAMBA"
        dateOfIssue.value = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        certType.value = "Course"
        _validationErrors.value = FormValidationErrors()
        _duplicateNote.value = null
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

        return Certificate.create(
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

    fun saveCertificateLocally(onSuccess: (isUpdate: Boolean) -> Unit, onError: (String) -> Unit) {
        if (!validateForm()) {
            val firstErr = listOfNotNull(
                _validationErrors.value.rollNoError,
                _validationErrors.value.studentNameError,
                _validationErrors.value.fatherNameError,
                _validationErrors.value.courseNameError,
                _validationErrors.value.dateOfIssueError
            ).firstOrNull() ?: "Please fix the required form errors."
            onError(firstErr)
            return
        }

        val cert = getAsCertificate()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existing = repository.getById(cert.certificateId) ?: repository.getByRollNo(cert.rollNo)
                val isUpdate = existing != null

                repository.insert(cert)
                withContext(Dispatchers.Main) {
                    onSuccess(isUpdate)
                }

                // Auto-sync with cloud if configured
                if (webAppUrl.value.isNotBlank()) {
                    syncCertificateWithSheets(cert)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("Failed to save certificate: ${e.localizedMessage ?: "Database error"}")
                }
            }
        }
    }

    fun deleteCertificate(certificateIdOrRollNo: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val certId = CertificateConfig.computeCertificateId(certificateIdOrRollNo)
            repository.delete(certificateIdOrRollNo)
            repository.delete(certId)

            withContext(Dispatchers.Main) {
                onSuccess()
            }

            // Sync remote deletion if URL is configured
            if (webAppUrl.value.isNotBlank()) {
                GoogleSheetsService.deleteCertificateRemotely(
                    webAppUrl = webAppUrl.value.trim(),
                    certificateId = certId,
                    apiKey = apiKey.value.trim()
                )
            }
        }
    }

    fun uploadCertificateToSheets() {
        if (!validateForm()) {
            _uploadError.value = "Cannot upload: Please fill in all required fields first."
            return
        }
        val cert = getAsCertificate()
        syncCertificateWithSheets(cert)
    }

    private fun syncCertificateWithSheets(cert: Certificate) {
        if (_isUploading.value) {
            return
        }

        val url = webAppUrl.value.trim()
        if (url.isBlank()) {
            _uploadError.value = "Google Sheets Web App URL is not set. Go to Settings to configure it."
            return
        }

        _isUploading.value = true
        _uploadStatus.value = "Connecting to Google Sheets..."
        _uploadError.value = null

        viewModelScope.launch(Dispatchers.IO) {
            val result = GoogleSheetsService.syncCertificate(
                webAppUrl = url,
                certificate = cert,
                apiKey = apiKey.value.trim(),
                verificationBaseUrl = verificationBaseUrl.value.trim()
            )

            withContext(Dispatchers.Main) {
                _isUploading.value = false
                when (result) {
                    is SyncResult.Success -> {
                        _uploadStatus.value = result.message
                        _uploadError.value = null
                        // Mark as synced locally
                        viewModelScope.launch(Dispatchers.IO) {
                            repository.updateSyncStatus(cert.certificateId, true)
                        }
                    }
                    is SyncResult.Error -> {
                        _uploadStatus.value = null
                        _uploadError.value = result.message
                    }
                }
            }
        }
    }

    fun testConnection(onResult: (Boolean, String) -> Unit) {
        val url = webAppUrl.value.trim()
        if (url.isBlank()) {
            onResult(false, "Please enter a Web App URL first.")
            return
        }
        _isTestingConnection.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val res = GoogleSheetsService.testConnection(url, apiKey.value.trim())
            withContext(Dispatchers.Main) {
                _isTestingConnection.value = false
                when (res) {
                    is SyncResult.Success -> onResult(true, res.message)
                    is SyncResult.Error -> onResult(false, res.message)
                }
            }
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
