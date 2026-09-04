package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "certificates")
data class Certificate(
    @PrimaryKey val rollNo: String,
    val studentName: String,
    val fatherName: String,
    val courseName: String,
    val sessionRange: String,
    val duration: String,
    val grade: String,
    val placeOfIssue: String,
    val dateOfIssue: String,
    val certType: String, // "Course" or "Internship"
    val timestamp: Long = System.currentTimeMillis()
)
