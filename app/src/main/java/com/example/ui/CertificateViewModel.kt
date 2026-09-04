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
        get() =
            rollNoError != null ||
            studentNameError != null ||
            fatherNameError != null ||
            courseNameError != null ||
            dateOfIssueError != null
}

class CertificateViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val appContext = application.applicationContext

    private val sharedPrefs =
        appContext.getSharedPreferences(
            "lges_admin_prefs",
            Context.MODE_PRIVATE
        )

    private val repository: CertificateRepository

    val allCertificates: StateFlow<List<Certificate>>

    // ============================================================
    // EDITING STATE
    // ============================================================

    /**
     * Keeps the original certificate ID while editing.
     *
     * This prevents changing a roll number during editing from
     * accidentally creating a completely new certificate ID.
     */
    private val editingCertificateId =
        MutableStateFlow<String?>(null)

    // ============================================================
    // FORM STATE
    // ============================================================

    val rollNo = MutableStateFlow("")

    val studentName = MutableStateFlow("")

    val relationPrefix =
        MutableStateFlow("S/O")

    val fatherName = MutableStateFlow("")

    val courseName = MutableStateFlow("")

    val sessionRange = MutableStateFlow("")

    val duration = MutableStateFlow("")

    val grade =
        MutableStateFlow("A")

    val placeOfIssue =
        MutableStateFlow("CHAMBA")

    val dateOfIssue =
        MutableStateFlow(
            SimpleDateFormat(
                "dd-MM-yyyy",
                Locale.getDefault()
            ).format(Date())
        )

    val certType =
        MutableStateFlow("Course")

    // ============================================================
    // VALIDATION
    // ============================================================

    private val _validationErrors =
        MutableStateFlow(FormValidationErrors())

    val validationErrors =
        _validationErrors.asStateFlow()

    private val _duplicateNote =
        MutableStateFlow<String?>(null)

    val duplicateNote =
        _duplicateNote.asStateFlow()

    // ============================================================
    // SETTINGS
    // ============================================================

    val webAppUrl =
        MutableStateFlow(
            sharedPrefs.getString(
                "web_app_url",
                CertificateConfig.DEFAULT_WEB_APP_URL
            ) ?: CertificateConfig.DEFAULT_WEB_APP_URL
        )

    val apiKey =
        MutableStateFlow(
            sharedPrefs.getString(
                "api_key",
                ""
            ) ?: ""
        )

    val verificationBaseUrl =
        MutableStateFlow(
            sharedPrefs.getString(
                "verification_base_url",
                CertificateConfig.DEFAULT_BASE_VERIFICATION_URL
            ) ?: CertificateConfig.DEFAULT_BASE_VERIFICATION_URL
        )

    // ============================================================
    // CLOUD SYNC STATE
    // ============================================================

    private val _isUploading =
        MutableStateFlow(false)

    val isUploading =
        _isUploading.asStateFlow()

    private val _uploadStatus =
        MutableStateFlow<String?>(null)

    val uploadStatus =
        _uploadStatus.asStateFlow()

    private val _uploadError =
        MutableStateFlow<String?>(null)

    val uploadError =
        _uploadError.asStateFlow()

    private val _isTestingConnection =
        MutableStateFlow(false)

    val isTestingConnection =
        _isTestingConnection.asStateFlow()

    // ============================================================
    // INIT
    // ============================================================

    init {
        val database =
            CertificateDatabase.getDatabase(appContext)

        repository =
            CertificateRepository(
                database.certificateDao()
            )

        allCertificates =
            repository.allCertificates.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    // ============================================================
    // SETTINGS
    // ============================================================

    fun updateWebAppUrl(url: String) {
        val clean = url.trim()

        webAppUrl.value = clean

        sharedPrefs.edit()
            .putString("web_app_url", clean)
            .apply()
    }

    fun updateApiKey(key: String) {
        val clean = key.trim()

        apiKey.value = clean

        sharedPrefs.edit()
            .putString("api_key", clean)
            .apply()
    }

    fun updateVerificationBaseUrl(url: String) {
        val clean = url.trim()

        verificationBaseUrl.value = clean

        sharedPrefs.edit()
            .putString(
                "verification_base_url",
                clean
            )
            .apply()
    }

    fun resetSettingsToDefault() {
        updateWebAppUrl(
            CertificateConfig.DEFAULT_WEB_APP_URL
        )

        updateApiKey("")

        updateVerificationBaseUrl(
            CertificateConfig.DEFAULT_BASE_VERIFICATION_URL
        )
    }

    // ============================================================
    // DUPLICATE CHECK
    // ============================================================

    fun checkForDuplicateRollNo(
        inputRoll: String
    ) {
        val clean =
            inputRoll.trim()

        if (clean.isBlank()) {
            _duplicateNote.value = null
            return
        }

        val certId =
            CertificateConfig.computeCertificateId(clean)

        val currentEditingId =
            editingCertificateId.value

        val existing =
            allCertificates.value.firstOrNull { certificate ->

                /*
                 * Do not treat the currently edited record as
                 * a duplicate of itself.
                 */
                if (
                    currentEditingId != null &&
                    certificate.certificateId.equals(
                        currentEditingId,
                        ignoreCase = true
                    )
                ) {
                    false
                } else {
                    certificate.rollNo.equals(
                        clean,
                        ignoreCase = true
                    ) ||
                    certificate.certificateId.equals(
                        certId,
                        ignoreCase = true
                    )
                }
            }

        if (existing != null) {

            _duplicateNote.value =
                "Notice: Record exists for '${existing.studentName}' " +
                "(${existing.certificateId}). " +
                "Saving will update the existing entry."

        } else if (currentEditingId != null) {

            _duplicateNote.value =
                "Editing existing certificate $currentEditingId"

        } else {

            _duplicateNote.value = null
        }
    }

    // ============================================================
    // VALIDATION
    // ============================================================

    fun validateForm(): Boolean {

        var rollError: String? = null
        var nameError: String? = null
        var fatherError: String? = null
        var courseError: String? = null
        var dateError: String? = null

        val roll =
            rollNo.value.trim()

        val student =
            studentName.value.trim()

        val father =
            fatherName.value.trim()

        val course =
            courseName.value.trim()

        val date =
            dateOfIssue.value.trim()

        if (roll.isEmpty()) {
            rollError =
                "Roll No. / Certificate No. is required"
        }

        if (student.isEmpty()) {
            nameError =
                "Student Name is required"
        }

        if (
            certType.value.equals(
                "Course",
                ignoreCase = true
            ) &&
            father.isEmpty()
        ) {
            fatherError =
                "Father / Guardian Name is required for Course certificates"
        }

        if (course.isEmpty()) {
            courseError =
                "Course / Internship title is required"
        }

        if (date.isEmpty()) {
            dateError =
                "Date of Issue is required"
        }

        val errors =
            FormValidationErrors(
                rollNoError = rollError,
                studentNameError = nameError,
                fatherNameError = fatherError,
                courseNameError = courseError,
                dateOfIssueError = dateError
            )

        _validationErrors.value =
            errors

        return !errors.hasErrors
    }

    // ============================================================
    // LOAD CERTIFICATE FOR EDITING
    // ============================================================

    fun loadCertificateForEditing(
        cert: Certificate
    ) {

        editingCertificateId.value =
            cert.certificateId

        rollNo.value =
            cert.rollNo

        studentName.value =
            cert.studentName

        val rawFather =
            cert.fatherName.trim()

        when {

            rawFather.startsWith(
                "S/O ",
                ignoreCase = true
            ) -> {
                relationPrefix.value = "S/O"
                fatherName.value =
                    rawFather
                        .substring(4)
                        .trim()
            }

            rawFather.startsWith(
                "D/O ",
                ignoreCase = true
            ) -> {
                relationPrefix.value = "D/O"
                fatherName.value =
                    rawFather
                        .substring(4)
                        .trim()
            }

            rawFather.startsWith(
                "W/O ",
                ignoreCase = true
            ) -> {
                relationPrefix.value = "W/O"
                fatherName.value =
                    rawFather
                        .substring(4)
                        .trim()
            }

            else -> {
                fatherName.value =
                    rawFather
            }
        }

        courseName.value =
            cert.courseName

        sessionRange.value =
            cert.sessionRange

        duration.value =
            cert.duration

        grade.value =
            cert.grade.ifBlank { "A" }

        placeOfIssue.value =
            cert.placeOfIssue.ifBlank {
                "CHAMBA"
            }

        dateOfIssue.value =
            cert.dateOfIssue

        certType.value =
            cert.certType

        _validationErrors.value =
            FormValidationErrors()

        _duplicateNote.value =
            "Editing existing certificate ${cert.certificateId}"

        clearUploadStatus()
    }

    // ============================================================
    // CLEAR FORM
    // ============================================================

    fun clearForm() {

        editingCertificateId.value =
            null

        rollNo.value = ""

        studentName.value = ""

        relationPrefix.value =
            "S/O"

        fatherName.value = ""

        courseName.value = ""

        sessionRange.value = ""

        duration.value = ""

        grade.value =
            "A"

        placeOfIssue.value =
            "CHAMBA"

        dateOfIssue.value =
            SimpleDateFormat(
                "dd-MM-yyyy",
                Locale.getDefault()
            ).format(Date())

        certType.value =
            "Course"

        _validationErrors.value =
            FormValidationErrors()

        _duplicateNote.value =
            null

        clearUploadStatus()
    }

    fun clearUploadStatus() {
        _uploadStatus.value = null
        _uploadError.value = null
    }

    // ============================================================
    // BUILD CERTIFICATE
    // ============================================================

    fun getAsCertificate(): Certificate {

        val rawFather =
            fatherName.value.trim()

        val formattedFatherName =
            when {

                rawFather.isEmpty() ->
                    ""

                rawFather.startsWith(
                    "S/O ",
                    ignoreCase = true
                ) ||
                rawFather.startsWith(
                    "D/O ",
                    ignoreCase = true
                ) ||
                rawFather.startsWith(
                    "W/O ",
                    ignoreCase = true
                ) ->
                    rawFather

                else ->
                    "${relationPrefix.value} $rawFather"
            }

        return Certificate.create(

            rollNo =
                rollNo.value.trim(),

            studentName =
                studentName.value.trim(),

            fatherName =
                formattedFatherName,

            courseName =
                courseName.value.trim(),

            sessionRange =
                sessionRange.value.trim(),

            duration =
                duration.value.trim(),

            grade =
                grade.value.trim(),

            placeOfIssue =
                placeOfIssue.value.trim(),

            dateOfIssue =
                dateOfIssue.value.trim(),

            certType =
                certType.value,

            /*
             * CRITICAL:
             *
             * Preserve the existing certificate ID during edit.
             */
            customId =
                editingCertificateId.value
        )
    }

    // ============================================================
    // SAVE LOCALLY
    // ============================================================

    fun saveCertificateLocally(
        onSuccess: (isUpdate: Boolean) -> Unit,
        onError: (String) -> Unit
    ) {

        if (!validateForm()) {

            val firstError =
                listOfNotNull(
                    validationErrors.value.rollNoError,
                    validationErrors.value.studentNameError,
                    validationErrors.value.fatherNameError,
                    validationErrors.value.courseNameError,
                    validationErrors.value.dateOfIssueError
                ).firstOrNull()
                    ?: "Please fix the required form errors."

            onError(firstError)

            return
        }

        val cert =
            getAsCertificate()

        viewModelScope.launch {

            try {

                val result =
                    withContext(Dispatchers.IO) {

                        val existing =
                            repository.getById(
                                cert.certificateId
                            )
                                ?: repository.getByRollNo(
                                    cert.rollNo
                                )

                        val isUpdate =
                            existing != null

                        repository.insert(cert)

                        isUpdate
                    }

                /*
                 * Report local save success first.
                 */
                onSuccess(result)

                /*
                 * Then perform optional cloud sync.
                 *
                 * This is intentionally not nested inside another
                 * viewModelScope.launch.
                 */
                if (
                    webAppUrl.value
                        .trim()
                        .isNotBlank()
                ) {
                    syncCertificateWithSheets(cert)
                }

            } catch (e: Exception) {

                onError(
                    "Failed to save certificate: " +
                    (e.localizedMessage
                        ?: "Database error")
                )
            }
        }
    }

    // ============================================================
    // DELETE
    // ============================================================

    fun deleteCertificate(
        certificateIdOrRollNo: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {

        val input =
            certificateIdOrRollNo.trim()

        if (input.isBlank()) {
            onError(
                "Certificate ID or Roll No. is required."
            )
            return
        }

        viewModelScope.launch {

            try {

                val certId =
                    CertificateConfig
                        .computeCertificateId(input)

                withContext(Dispatchers.IO) {

                    /*
                     * Try both forms because the UI may supply
                     * either certificate ID or roll number.
                     */
                    repository.delete(input)

                    if (
                        !input.equals(
                            certId,
                            ignoreCase = true
                        )
                    ) {
                        repository.delete(certId)
                    }
                }

                /*
                 * Remote deletion is attempted separately.
                 *
                 * Local deletion has already succeeded.
                 */
                if (
                    webAppUrl.value
                        .trim()
                        .isNotBlank()
                ) {

                    try {

                        GoogleSheetsService
                            .deleteCertificateRemotely(
                                webAppUrl =
                                    webAppUrl.value.trim(),

                                certificateId =
                                    certId,

                                apiKey =
                                    apiKey.value.trim()
                            )

                    } catch (remoteError: Exception) {

                        _uploadError.value =
                            "Deleted locally, but remote deletion failed: " +
                            (
                                remoteError.localizedMessage
                                    ?: "Unknown cloud error"
                            )
                    }
                }

                onSuccess()

            } catch (e: Exception) {

                onError(
                    "Failed to delete certificate: " +
                    (
                        e.localizedMessage
                            ?: "Database error"
                    )
                )
            }
        }
    }

    // ============================================================
    // MANUAL CLOUD UPLOAD
    // ============================================================

    fun uploadCertificateToSheets() {

        if (!validateForm()) {

            _uploadError.value =
                "Cannot upload: Please fill in all required fields first."

            return
        }

        val cert =
            getAsCertificate()

        syncCertificateWithSheets(cert)
    }

    // ============================================================
    // GOOGLE SHEETS SYNC
    // ============================================================

    private fun syncCertificateWithSheets(
        cert: Certificate
    ) {

        if (_isUploading.value) {
            return
        }

        val url =
            webAppUrl.value.trim()

        if (url.isBlank()) {

            _uploadError.value =
                "Google Sheets Web App URL is not set. " +
                "Go to Settings to configure it."

            return
        }

        _isUploading.value =
            true

        _uploadStatus.value =
            "Connecting to Google Sheets..."

        _uploadError.value =
            null

        viewModelScope.launch {

            try {

                val result =
                    withContext(Dispatchers.IO) {

                        GoogleSheetsService.syncCertificate(
                            webAppUrl = url,
                            certificate = cert,
                            apiKey = apiKey.value.trim(),
                            verificationBaseUrl =
                                verificationBaseUrl.value.trim()
                        )
                    }

                when (result) {

                    is SyncResult.Success -> {

                        _uploadStatus.value =
                            result.message

                        _uploadError.value =
                            null

                        /*
                         * Only mark local record as synced
                         * after successful remote sync.
                         */
                        withContext(Dispatchers.IO) {

                            repository.updateSyncStatus(
                                cert.certificateId,
                                true
                            )
                        }
                    }

                    is SyncResult.Error -> {

                        _uploadStatus.value =
                            null

                        _uploadError.value =
                            result.message
                    }
                }

            } catch (e: Exception) {

                _uploadStatus.value =
                    null

                _uploadError.value =
                    "Google Sheets sync failed: " +
                    (
                        e.localizedMessage
                            ?: "Unknown error"
                    )

            } finally {

                _isUploading.value =
                    false
            }
        }
    }

    // ============================================================
    // CONNECTION TEST
    // ============================================================

    fun testConnection(
        onResult: (Boolean, String) -> Unit
    ) {

        val url =
            webAppUrl.value.trim()

        if (url.isBlank()) {

            onResult(
                false,
                "Please enter a Web App URL first."
            )

            return
        }

        if (_isTestingConnection.value) {
            return
        }

        _isTestingConnection.value =
            true

        viewModelScope.launch {

            try {

                val result =
                    withContext(Dispatchers.IO) {

                        GoogleSheetsService.testConnection(
                            url,
                            apiKey.value.trim()
                        )
                    }

                when (result) {

                    is SyncResult.Success ->
                        onResult(
                            true,
                            result.message
                        )

                    is SyncResult.Error ->
                        onResult(
                            false,
                            result.message
                        )
                }

            } catch (e: Exception) {

                onResult(
                    false,
                    "Connection test failed: " +
                    (
                        e.localizedMessage
                            ?: "Unknown error"
                    )
                )

            } finally {

                _isTestingConnection.value =
                    false
            }
        }
    }

    // ============================================================
    // FACTORY
    // ============================================================

    class Factory(
        private val application: Application
    ) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(
            modelClass: Class<T>
        ): T {

            if (
                modelClass.isAssignableFrom(
                    CertificateViewModel::class.java
                )
            ) {

                @Suppress("UNCHECKED_CAST")

                return CertificateViewModel(
                    application
                ) as T
            }

            throw IllegalArgumentException(
                "Unknown ViewModel class"
            )
        }
    }
}