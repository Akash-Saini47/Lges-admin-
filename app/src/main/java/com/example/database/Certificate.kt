package com.example.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.util.CertificateConfig

/**
 * Cloud synchronization status for offline-first durable consistency.
 */
enum class SyncStatus {
    PENDING,
    SYNCING,
    SYNCED,
    FAILED,
    DELETE_PENDING,
    DELETE_FAILED
}

@Entity(
    tableName = "certificates",
    indices = [
        Index(value = ["rollNo"]),
        Index(value = ["timestamp"]),
        Index(value = ["syncStatus"])
    ]
)
data class Certificate(

    /**
     * Immutable unique identity of THIS certificate.
     * Primary key. Not derived from roll number.
     */
    @PrimaryKey
    val certificateId: String,

    /**
     * Roll number assigned to this certificate/enrollment.
     * Multiple certificates can share the same roll number.
     */
    val rollNo: String,

    val studentName: String,

    val fatherName: String,

    val courseName: String,

    val sessionRange: String,

    val duration: String,

    val grade: String,

    val placeOfIssue: String,

    val dateOfIssue: String,

    /**
     * Examples: Course, Internship
     */
    val certType: String,

    val timestamp: Long = System.currentTimeMillis(),

    /**
     * Robust synchronization state for offline-first durability.
     */
    val syncStatus: SyncStatus = SyncStatus.PENDING,

    /**
     * Epoch timestamp in milliseconds of last successful or attempted sync.
     */
    val lastSyncTime: Long? = null,

    /**
     * Number of times synchronization has been retried.
     */
    val retryCount: Int = 0,

    /**
     * Description of last synchronization error, if any.
     */
    val lastSyncError: String? = null
) {

    /**
     * Backward-compatible property for existing callers and exporters.
     */
    val isSynced: Boolean
        get() = syncStatus == SyncStatus.SYNCED

    companion object {

        /**
         * Factory function to create or reconstruct a Certificate.
         *
         * @param customId Preserved when editing an existing certificate or importing historical data.
         *                 When null or blank, a new unique modern certificate ID is generated.
         */
        fun create(
            rollNo: String,
            studentName: String,
            fatherName: String,
            courseName: String,
            sessionRange: String,
            duration: String,
            grade: String,
            placeOfIssue: String,
            dateOfIssue: String,
            certType: String,
            timestamp: Long = System.currentTimeMillis(),
            syncStatus: SyncStatus = SyncStatus.PENDING,
            isSynced: Boolean = false,
            lastSyncTime: Long? = null,
            retryCount: Int = 0,
            lastSyncError: String? = null,
            customId: String? = null
        ): Certificate {

            val cleanRollNo = rollNo.trim()
            val cleanStudentName = studentName.trim()
            val cleanFatherName = fatherName.trim()
            val cleanCourseName = courseName.trim()
            val cleanSessionRange = sessionRange.trim()
            val cleanDuration = duration.trim()
            val cleanGrade = grade.trim()
            val cleanPlaceOfIssue = placeOfIssue.trim()
            val cleanDateOfIssue = dateOfIssue.trim()
            val cleanCertType = certType.trim().ifBlank { "Course" }

            val certId = customId
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: CertificateConfig.generateCertificateId()

            // Resolve status: if explicitly synced via legacy flag, map to SYNCED
            val effectiveStatus = if (isSynced && syncStatus == SyncStatus.PENDING) {
                SyncStatus.SYNCED
            } else {
                syncStatus
            }

            return Certificate(
                certificateId = certId,
                rollNo = cleanRollNo,
                studentName = cleanStudentName,
                fatherName = cleanFatherName,
                courseName = cleanCourseName,
                sessionRange = cleanSessionRange,
                duration = cleanDuration,
                grade = cleanGrade,
                placeOfIssue = cleanPlaceOfIssue,
                dateOfIssue = cleanDateOfIssue,
                certType = cleanCertType,
                timestamp = timestamp,
                syncStatus = effectiveStatus,
                lastSyncTime = lastSyncTime,
                retryCount = retryCount,
                lastSyncError = lastSyncError
            )
        }
    }
}