package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.database.Certificate
import com.example.database.CertificateDatabase
import com.example.database.CertificateRepository
import com.example.database.GoogleSheetsService
import com.example.database.SyncResult
import com.example.database.SyncStatus
import com.example.sync.SyncManager
import com.example.util.AppLogger
import com.example.util.CertificateConfig
import com.example.util.CertificateValidator
import com.example.util.FormValidationErrors
import com.example.util.SecureSettings
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

class CertificateViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val secureSettings = SecureSettings.getInstance(appContext)

    private val repository: CertificateRepository
    val allCertificates: StateFlow<List<Certificate>>

    // ============================================================
    // EDITING STATE
    // ============================================================

    /**
     * The primary key identifier of the certificate currently being edited.
     * Null means creating a new certificate.
     */
    private val editingCertificateId = MutableStateFlow<String?>(null)
    val currentEditingId: StateFlow<String?> = editingCertificateId.asStateFlow()

    // ============================================================
    // FORM STATE
    // ============================================================

    val rollNo = MutableStateFlow("")
    val studentName = MutableStateFlow("")
    val relationPrefix = MutableStateFlow("S/O")
    val fatherName = MutableStateFlow("")
    val courseName = MutableStateFlow("")
    val sessionRange = MutableStateFlow("")
    val duration = MutableStateFlow("")
    val grade = MutableStateFlow("A")
    val placeOfIssue = MutableStateFlow("CHAMBA")
    val dateOfIssue = MutableStateFlow(
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
    )
    val certType = MutableStateFlow("Course")

    // ============================================================
    // VALIDATION STATE
    // ============================================================

    private val _validationErrors = MutableStateFlow(FormValidationErrors())
    val validationErrors: StateFlow<FormValidationErrors> = _validationErrors.asStateFlow()

    private val _duplicateNote = MutableStateFlow<String?>(null)
    val duplicateNote: StateFlow<String?> = _duplicateNote.asStateFlow()

    // ============================================================
    // SETTINGS STATE (Secure)
    // ============================================================

    val webAppUrl = MutableStateFlow(secureSettings.getWebAppUrl())
    val apiKey = MutableStateFlow(secureSettings.getApiKey())
    val verificationBaseUrl = MutableStateFlow(secureSettings.getVerificationBaseUrl())

    // ============================================================
    // CLOUD SYNC STATE
    // ============================================================

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _uploadStatus = MutableStateFlow<String?>(null)
    val uploadStatus: StateFlow<String?> = _uploadStatus.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    // ============================================================
    // INITIALIZATION
    // ============================================================

    init {
        val database = CertificateDatabase.getDatabase(appContext)
        repository = CertificateRepository(database.certificateDao())

        allCertificates = repository.allCertificates.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Schedule periodic background sync for offline robustness
        SyncManager.schedulePeriodicSync(appContext)
    }

    // ============================================================
    // SETTINGS METHODS
    // ============================================================

    fun updateWebAppUrl(url: String) {
        val clean = url.trim()
        webAppUrl.value = clean
        secureSettings.saveWebAppUrl(clean)
    }

    fun updateApiKey(key: String) {
        val clean = key.trim()
        apiKey.value = clean
        secureSettings.saveApiKey(clean)
    }

    fun updateVerificationBaseUrl(url: String) {
        val clean = url.trim()
        verificationBaseUrl.value = clean
        secureSettings.saveVerificationBaseUrl(clean)
    }

    fun resetSettingsToDefault() {
        updateWebAppUrl(CertificateConfig.DEFAULT_WEB_APP_URL)
        updateApiKey("")
        updateVerificationBaseUrl(CertificateConfig.DEFAULT_BASE_VERIFICATION_URL)
    }

    // ============================================================
    // DUPLICATE ROLL NUMBER CHECK (Informational only)
    // ============================================================

    fun checkForDuplicateRollNo(inputRoll: String) {
        val clean = inputRoll.trim()
        if (clean.isBlank()) {
            _duplicateNote.value = null
            return
        }

        val currentId = editingCertificateId.value
        val existing = allCertificates.value.firstOrNull { cert ->
            if (currentId != null && cert.certificateId.equals(currentId, ignoreCase = true)) {
                false
            } else {
                cert.rollNo.trim().equals(clean, ignoreCase = true)
            }
        }

        _duplicateNote.value = if (existing != null) {
            "Notice: Roll No. '$clean' is also used by ${existing.studentName} (${existing.certificateId}). A separate certificate will be created."
        } else if (currentId != null) {
            "Editing certificate $currentId"
        } else {
            null
        }
    }

    // ============================================================
    // FORM VALIDATION (Single Source of Truth)
    // ============================================================

    fun validateForm(): Boolean {
        val rawFather = fatherName.value.trim()
        val formattedFather = formatFatherName(rawFather)

        val errors = CertificateValidator.validateAll(
            rollNo = rollNo.value,
            studentName = studentName.value,
            fatherName = formattedFather,
            courseName = courseName.value,
            sessionRange = sessionRange.value,
            duration = duration.value,
            grade = grade.value,
            placeOfIssue = placeOfIssue.value,
            dateOfIssue = dateOfIssue.value,
            certType = certType.value
        )

        _validationErrors.value = errors
        return !errors.hasErrors
    }

    private fun formatFatherName(raw: String): String {
        if (raw.isBlank()) return ""
        val clean = raw.trim()
        return if (clean.startsWith("S/O ", ignoreCase = true) ||
            clean.startsWith("D/O ", ignoreCase = true) ||
            clean.startsWith("W/O ", ignoreCase = true) ||
            clean.startsWith("C/O ", ignoreCase = true)
        ) {
            clean
        } else {
            "${relationPrefix.value} $clean"
        }
    }

    // ============================================================
    // LOAD & CLEAR FORM
    // ============================================================

    fun loadCertificateForEditing(cert: Certificate) {
        editingCertificateId.value = cert.certificateId
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
            rawFather.startsWith("C/O ", ignoreCase = true) -> {
                relationPrefix.value = "C/O"
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
        certType.value = cert.certType.ifBlank { "Course" }

        _validationErrors.value = FormValidationErrors()
        _duplicateNote.value = "Editing existing certificate ${cert.certificateId}"
        clearUploadStatus()
    }

    fun clearForm() {
        editingCertificateId.value = null
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

    // ============================================================
    // CONSTRUCT CERTIFICATE
    // ============================================================

    fun getAsCertificate(): Certificate {
        val formattedFather = formatFatherName(fatherName.value)

        return Certificate.create(
            rollNo = rollNo.value,
            studentName = studentName.value,
            fatherName = formattedFather,
            courseName = courseName.value,
            sessionRange = sessionRange.value,
            duration = duration.value,
            grade = grade.value,
            placeOfIssue = placeOfIssue.value,
            dateOfIssue = dateOfIssue.value,
            certType = certType.value,
            customId = editingCertificateId.value
        )
    }

    // ============================================================
    // SAVE LOCALLY & SYNC
    // ============================================================

    fun saveCertificateLocally(
        onSuccess: (isUpdate: Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!validateForm()) {
            val firstError = listOfNotNull(
                validationErrors.value.rollNoError,
                validationErrors.value.studentNameError,
                validationErrors.value.fatherNameError,
                validationErrors.value.courseNameError,
                validationErrors.value.dateOfIssueError
            ).firstOrNull() ?: "Please fix validation errors before saving."

            onError(firstError)
            return
        }

        val cert = getAsCertificate()
        val isEditing = editingCertificateId.value != null

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    if (isEditing) {
                        val existing = repository.getById(cert.certificateId)
                        if (existing == null) {
                            throw IllegalStateException("The certificate being edited (${cert.certificateId}) no longer exists.")
                        }
                        SyncManager.saveAndQueueSync(appContext, repository, cert, isUpdate = true)
                    } else {
                        SyncManager.saveAndQueueSync(appContext, repository, cert, isUpdate = false)
                    }
                }

                onSuccess(isEditing)

                // Trigger immediate direct cloud sync attempt if URL is configured
                if (CertificateConfig.isValidHttpUrl(webAppUrl.value)) {
                    syncCertificateWithSheets(cert)
                }

            } catch (e: Exception) {
                AppLogger.e("CertificateViewModel", "Save failed: ${e.message}", e)
                onError("Failed to save certificate: ${e.localizedMessage ?: "Database error"}")
            }
        }
    }

    // ============================================================
    // DELETE (Durable Offline-First)
    // ============================================================

    /**
     * Deletes a certificate using its unique certificateId.
     * Safely resolves legacy roll numbers if provided.
     */
    fun deleteCertificate(
        certificateIdOrRollNo: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val input = certificateIdOrRollNo.trim()
        if (input.isBlank()) {
            onError("Certificate ID is required for deletion.")
            return
        }

        viewModelScope.launch {
            try {
                val certToDelete = withContext(Dispatchers.IO) {
                    // 1. Look up directly by Certificate ID (preferred)
                    repository.getById(input)
                        // 2. Fallback to roll number lookup (taking first match)
                        ?: repository.getByRollNo(input).firstOrNull()
                }

                if (certToDelete == null) {
                    onError("Certificate not found: $input")
                    return@launch
                }

                val actualId = certToDelete.certificateId

                // Queue for durable offline deletion
                withContext(Dispatchers.IO) {
                    SyncManager.queueForDeletion(appContext, repository, actualId)
                }

                // If currently editing this certificate, reset form
                if (editingCertificateId.value == actualId) {
                    clearForm()
                }

                onSuccess()

            } catch (e: Exception) {
                AppLogger.e("CertificateViewModel", "Delete failed: ${e.message}", e)
                onError("Failed to delete certificate: ${e.localizedMessage ?: "Error"}")
            }
        }
    }

    // ============================================================
    // MANUAL CLOUD UPLOAD & SYNC
    // ============================================================

    fun uploadCertificateToSheets() {
        if (!validateForm()) {
            _uploadError.value = "Cannot upload: Please fill in all required fields."
            return
        }
        val cert = getAsCertificate()
        syncCertificateWithSheets(cert)
    }

    fun syncAllPending() {
        SyncManager.scheduleImmediateSync(appContext)
        _uploadStatus.value = "Synchronization queued in background."
    }

    private fun syncCertificateWithSheets(cert: Certificate) {
        if (_isUploading.value) return

        val url = webAppUrl.value.trim()
        if (!CertificateConfig.isValidHttpUrl(url)) {
            _uploadError.value = "Invalid Google Sheets Web App URL. Configure it in Settings."
            return
        }

        _isUploading.value = true
        _uploadStatus.value = "Connecting to Google Sheets..."
        _uploadError.value = null

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    GoogleSheetsService.syncCertificate(
                        webAppUrl = url,
                        certificate = cert,
                        apiKey = apiKey.value.trim(),
                        verificationBaseUrl = CertificateConfig.getSafeVerificationBaseUrl(verificationBaseUrl.value)
                    )
                }

                when (result) {
                    is SyncResult.Success -> {
                        _uploadStatus.value = result.message
                        _uploadError.value = null

                        withContext(Dispatchers.IO) {
                            repository.updateSyncState(
                                certificateId = cert.certificateId,
                                syncStatus = SyncStatus.SYNCED,
                                lastSyncTime = System.currentTimeMillis(),
                                lastSyncError = null,
                                retryCount = 0
                            )
                        }
                    }
                    is SyncResult.Error -> {
                        _uploadStatus.value = null
                        _uploadError.value = result.message

                        withContext(Dispatchers.IO) {
                            repository.updateSyncState(
                                certificateId = cert.certificateId,
                                syncStatus = SyncStatus.FAILED,
                                lastSyncTime = System.currentTimeMillis(),
                                lastSyncError = result.message
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("CertificateViewModel", "Sync exception: ${e.message}", e)
                _uploadStatus.value = null
                _uploadError.value = "Google Sheets sync failed: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isUploading.value = false
            }
        }
    }

    // ============================================================
    // CONNECTION TEST
    // ============================================================

    fun testConnection(onResult: (Boolean, String) -> Unit) {
        val url = webAppUrl.value.trim()
        if (!CertificateConfig.isValidHttpUrl(url)) {
            onResult(false, "Please enter a valid HTTP/HTTPS Web App URL.")
            return
        }

        if (_isTestingConnection.value) return
        _isTestingConnection.value = true

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    GoogleSheetsService.testConnection(
                        webAppUrl = url,
                        apiKey = apiKey.value.trim()
                    )
                }

                when (result) {
                    is SyncResult.Success -> onResult(true, result.message)
                    is SyncResult.Error -> onResult(false, result.message)
                }
            } catch (e: Exception) {
                onResult(false, "Connection test failed: ${e.localizedMessage ?: "Unknown error"}")
            } finally {
                _isTestingConnection.value = false
            }
        }
    }

    // ============================================================
    // FACTORY
    // ============================================================

    class Factory(
        private val application: Application
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CertificateViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return CertificateViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}