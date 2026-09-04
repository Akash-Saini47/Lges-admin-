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

    private val appContext =
        application.applicationContext

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
     * IMPORTANT:
     *
     * This is the ONLY identifier used to determine which
     * certificate is being edited.
     *
     * Roll number is deliberately NOT used as certificate identity.
     */
    private val editingCertificateId =
        MutableStateFlow<String?>(null)

    // ============================================================
    // FORM STATE
    // ============================================================

    val rollNo =
        MutableStateFlow("")

    val studentName =
        MutableStateFlow("")

    val relationPrefix =
        MutableStateFlow("S/O")

    val fatherName =
        MutableStateFlow("")

    val courseName =
        MutableStateFlow("")

    val sessionRange =
        MutableStateFlow("")

    val duration =
        MutableStateFlow("")

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
    // VALIDATION STATE
    // ============================================================

    private val _validationErrors =
        MutableStateFlow(
            FormValidationErrors()
        )

    val validationErrors =
        _validationErrors.asStateFlow()

    /**
     * Informational notice only.
     *
     * A duplicate roll number DOES NOT automatically mean
     * "update this certificate".
     */
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
            CertificateDatabase.getDatabase(
                appContext
            )

        repository =
            CertificateRepository(
                database.certificateDao()
            )

        allCertificates =
            repository.allCertificates.stateIn(
                scope = viewModelScope,
                started =
                    SharingStarted.WhileSubscribed(
                        5000
                    ),
                initialValue = emptyList()
            )
    }

    // ============================================================
    // SETTINGS
    // ============================================================

    fun updateWebAppUrl(
        url: String
    ) {

        val clean =
            url.trim()

        webAppUrl.value =
            clean

        sharedPrefs.edit()
            .putString(
                "web_app_url",
                clean
            )
            .apply()
    }

    fun updateApiKey(
        key: String
    ) {

        val clean =
            key.trim()

        apiKey.value =
            clean

        sharedPrefs.edit()
            .putString(
                "api_key",
                clean
            )
            .apply()
    }

    fun updateVerificationBaseUrl(
        url: String
    ) {

        val clean =
            url.trim()

        verificationBaseUrl.value =
            clean

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
    // DUPLICATE ROLL NUMBER CHECK
    // ============================================================

    /**
     * Checks whether another certificate already uses the same
     * roll number.
     *
     * IMPORTANT:
     *
     * This function NEVER changes the certificate being updated.
     *
     * A duplicate roll number is only an informational warning.
     */
    fun checkForDuplicateRollNo(
        inputRoll: String
    ) {

        val clean =
            inputRoll.trim()

        if (clean.isBlank()) {

            _duplicateNote.value =
                null

            return
        }

        val currentEditingId =
            editingCertificateId.value

        val existingCertificates =
            allCertificates.value.filter { certificate ->

                /*
                 * Exclude the certificate currently being edited.
                 *
                 * If we are editing LGES-101 and its roll number
                 * is 101, it must not warn about itself.
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
                    certificate.rollNo.trim()
                        .equals(
                            clean,
                            ignoreCase = true
                        )
                }
            }

        val existing =
            existingCertificates.firstOrNull()

        if (existing != null) {

            _duplicateNote.value =
                "Notice: Roll No. '$clean' is already used by " +
                        "${existing.studentName} " +
                        "(${existing.certificateId}). " +
                        "A new certificate will remain a separate record."

        } else if (currentEditingId != null) {

            _duplicateNote.value =
                "Editing certificate $currentEditingId"

        } else {

            _duplicateNote.value =
                null
        }
    }

    // ============================================================
    // FORM VALIDATION
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
                rollNoError =
                    rollError,

                studentNameError =
                    nameError,

                fatherNameError =
                    fatherError,

                courseNameError =
                    courseError,

                dateOfIssueError =
                    dateError
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

        /*
         * CRITICAL:
         *
         * Store the exact certificate ID.
         *
         * We do NOT store the old roll number as the identity.
         */
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

                relationPrefix.value =
                    "S/O"

                fatherName.value =
                    rawFather
                        .substring(4)
                        .trim()
            }

            rawFather.startsWith(
                "D/O ",
                ignoreCase = true
            ) -> {

                relationPrefix.value =
                    "D/O"

                fatherName.value =
                    rawFather
                        .substring(4)
                        .trim()
            }

            rawFather.startsWith(
                "W/O ",
                ignoreCase = true
            ) -> {

                relationPrefix.value =
                    "W/O"

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
            cert.grade.ifBlank {
                "A"
            }

        placeOfIssue.value =
            cert.placeOfIssue.ifBlank {
                "CHAMBA"
            }

        dateOfIssue.value =
            cert.dateOfIssue

        certType.value =
            cert.certType.ifBlank {
                "Course"
            }

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

        /*
         * Setting this to null switches the next save into
         * CREATE mode instead of EDIT mode.
         */
        editingCertificateId.value =
            null

        rollNo.value =
            ""

        studentName.value =
            ""

        relationPrefix.value =
            "S/O"

        fatherName.value =
            ""

        courseName.value =
            ""

        sessionRange.value =
            ""

        duration.value =
            ""

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

        _uploadStatus.value =
            null

        _uploadError.value =
            null
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

        /*
         * CREATE MODE:
         *
         * Certificate.create() generates a new certificate ID
         * from the roll number.
         *
         * EDIT MODE:
         *
         * customId preserves the existing certificate ID.
         */
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
                certType.value.trim()
                    .ifBlank {
                        "Course"
                    },

            /*
             * This is the critical multiple-certificate behavior.
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

                    validationErrors.value
                        .rollNoError,

                    validationErrors.value
                        .studentNameError,

                    validationErrors.value
                        .fatherNameError,

                    validationErrors.value
                        .courseNameError,

                    validationErrors.value
                        .dateOfIssueError

                ).firstOrNull()
                    ?: "Please fix the required form errors."

            onError(firstError)

            return
        }

        val cert =
            getAsCertificate()

        /*
         * Capture this BEFORE launching the coroutine.
         *
         * This tells us whether the user is editing an existing
         * certificate or creating a completely new one.
         */
        val isEditing =
            editingCertificateId.value != null

        viewModelScope.launch {

            try {

                val isUpdate =
                    withContext(Dispatchers.IO) {

                        if (isEditing) {

                            /*
                             * EDIT MODE
                             *
                             * Only certificateId determines which
                             * record is being updated.
                             *
                             * NEVER search by roll number here.
                             */
                            val existing =
                                repository.getById(
                                    cert.certificateId
                                )

                            if (existing == null) {

                                throw IllegalStateException(
                                    "The certificate being edited no longer exists."
                                )
                            }

                            repository.update(
                                cert.copy(
                                    /*
                                     * Preserve the original
                                     * synchronization state until
                                     * cloud sync completes.
                                     */
                                    isSynced = false
                                )
                            )

                            true

                        } else {

                            /*
                             * CREATE MODE
                             *
                             * This is ALWAYS a new certificate.
                             *
                             * We intentionally do NOT call
                             * getByRollNo() here.
                             *
                             * Therefore:
                             *
                             * Rahul / Roll 101 / Course
                             * Rahul / Roll 205 / Internship
                             *
                             * can coexist.
                             */
                            repository.insert(
                                cert
                            )

                            false
                        }
                    }

                /*
                 * Local database save succeeded.
                 */
                onSuccess(
                    isUpdate
                )

                /*
                 * Cloud synchronization happens AFTER local save.
                 *
                 * A cloud failure does not destroy the local record.
                 */
                if (
                    webAppUrl.value
                        .trim()
                        .isNotBlank()
                ) {

                    syncCertificateWithSheets(
                        cert
                    )
                }

            } catch (e: Exception) {

                onError(
                    "Failed to save certificate: " +
                            (
                                e.localizedMessage
                                    ?: "Database error"
                                )
                )
            }
        }
    }

    // ============================================================
    // DELETE
    // ============================================================

    /**
     * Deletes a certificate.
     *
     * Preferred input:
     * certificateId
     *
     * A roll number is still accepted for backward compatibility,
     * but the method tries to resolve it to an actual certificate
     * before deleting.
     */
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

                val certificateToDelete =
                    withContext(Dispatchers.IO) {

                        /*
                         * First treat the supplied value as a
                         * certificate ID.
                         */
                        repository.getById(
                            input
                        )
                            /*
                             * For backward compatibility, try
                             * roll number as a fallback.
                             */
                            ?: repository.getByRollNo(
                                input
                            )
                    }

                if (certificateToDelete == null) {

                    onError(
                        "Certificate not found: $input"
                    )

                    return@launch
                }

                val actualCertificateId =
                    certificateToDelete.certificateId

                withContext(Dispatchers.IO) {

                    /*
                     * Delete EXACTLY one certificate by its primary key.
                     *
                     * This is safer than deleting by roll number because
                     * a student can have multiple certificates.
                     */
                    repository.deleteById(
                        actualCertificateId
                    )
                }

                /*
                 * Remote deletion uses the actual certificate ID,
                 * never the roll number.
                 */
                if (
                    webAppUrl.value
                        .trim()
                        .isNotBlank()
                ) {

                    val remoteResult =
                        try {

                            GoogleSheetsService
                                .deleteCertificateRemotely(
                                    webAppUrl =
                                        webAppUrl.value.trim(),

                                    certificateId =
                                        actualCertificateId,

                                    apiKey =
                                        apiKey.value.trim()
                                )

                        } catch (remoteError: Exception) {

                            SyncResult.Error(
                                "Remote deletion failed: " +
                                        (
                                            remoteError.localizedMessage
                                                ?: "Unknown cloud error"
                                            ),
                                remoteError
                            )
                        }

                    if (
                        remoteResult is
                        SyncResult.Error
                    ) {

                        _uploadError.value =
                            "Deleted locally, but remote deletion failed: " +
                                    remoteResult.message
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

        syncCertificateWithSheets(
            cert
        )
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

        /*
         * Validate the endpoint before starting a network request.
         */
        if (
            !CertificateConfig.isValidHttpUrl(
                url
            )
        ) {

            _uploadError.value =
                "Invalid Google Sheets Web App URL."

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

                        GoogleSheetsService
                            .syncCertificate(

                                webAppUrl =
                                    url,

                                certificate =
                                    cert,

                                apiKey =
                                    apiKey.value.trim(),

                                verificationBaseUrl =
                                    CertificateConfig
                                        .getSafeVerificationBaseUrl(
                                            verificationBaseUrl.value
                                        )
                            )
                    }

                when (result) {

                    is SyncResult.Success -> {

                        _uploadStatus.value =
                            result.message

                        _uploadError.value =
                            null

                        /*
                         * Mark ONLY this exact certificate
                         * as synchronized.
                         */
                        withContext(Dispatchers.IO) {

                            repository.updateSyncStatus(
                                certificateId =
                                    cert.certificateId,

                                isSynced =
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
        onResult: (
            Boolean,
            String
        ) -> Unit
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

        if (
            !CertificateConfig.isValidHttpUrl(
                url
            )
        ) {

            onResult(
                false,
                "Please enter a valid HTTP/HTTPS Web App URL."
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

                        GoogleSheetsService
                            .testConnection(
                                webAppUrl =
                                    url,

                                apiKey =
                                    apiKey.value.trim()
                            )
                    }

                when (result) {

                    is SyncResult.Success -> {

                        onResult(
                            true,
                            result.message
                        )
                    }

                    is SyncResult.Error -> {

                        onResult(
                            false,
                            result.message
                        )
                    }
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
                "Unknown ViewModel class: ${modelClass.name}"
            )
        }
    }
}