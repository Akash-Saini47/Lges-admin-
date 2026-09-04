package com.example.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.util.CertificateConfig

@Entity(
    tableName = "certificates",
    indices = [
        Index(value = ["rollNo"]),
        Index(value = ["timestamp"])
    ]
)
data class Certificate(

    /**
     * Unique identity of THIS certificate.
     *
     * This must NOT be based on rollNo because a student can
     * receive multiple certificates.
     */
    @PrimaryKey
    val certificateId: String,

    /**
     * Roll number assigned to this certificate/enrollment.
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
     * Examples:
     * Course
     * Internship
     */
    val certType: String,

    val timestamp: Long = System.currentTimeMillis(),

    val isSynced: Boolean = false
) {

    companion object {

        /**
         * Creates a certificate with a unique certificate ID.
         *
         * customId can be supplied when importing an existing
         * certificate or when the server has already assigned an ID.
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
            isSynced: Boolean = false,
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
            val cleanCertType = certType.trim()

            val certId = customId
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: CertificateConfig.generateCertificateId()

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
                isSynced = isSynced
            )
        }
    }
}