package com.example.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.util.CertificateConfig

@Entity(
    tableName = "certificates",
    indices = [Index(value = ["rollNo"])]
)
data class Certificate(
    @PrimaryKey val certificateId: String,
    val rollNo: String,
    val studentName: String,
    val fatherName: String,
    val courseName: String,
    val sessionRange: String,
    val duration: String,
    val grade: String,
    val placeOfIssue: String,
    val dateOfIssue: String,
    val certType: String, // "Course" or "Internship"
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
) {
    companion object {
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
            val certId = customId?.ifBlank { null } ?: CertificateConfig.computeCertificateId(rollNo)
            return Certificate(
                certificateId = certId,
                rollNo = rollNo.trim(),
                studentName = studentName.trim(),
                fatherName = fatherName.trim(),
                courseName = courseName.trim(),
                sessionRange = sessionRange.trim(),
                duration = duration.trim(),
                grade = grade.trim(),
                placeOfIssue = placeOfIssue.trim(),
                dateOfIssue = dateOfIssue.trim(),
                certType = certType,
                timestamp = timestamp,
                isSynced = isSynced
            )
        }
    }
}
