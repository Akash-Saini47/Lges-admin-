package com.example.util

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Result of domain validation on certificate fields.
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
) {
    companion object {
        fun success(): ValidationResult = ValidationResult(isValid = true)
        fun error(message: String): ValidationResult = ValidationResult(isValid = false, errorMessage = message)
    }
}

/**
 * Consolidated validation error state for the certificate form.
 */
data class FormValidationErrors(
    val rollNoError: String? = null,
    val studentNameError: String? = null,
    val fatherNameError: String? = null,
    val courseNameError: String? = null,
    val sessionRangeError: String? = null,
    val durationError: String? = null,
    val gradeError: String? = null,
    val placeOfIssueError: String? = null,
    val dateOfIssueError: String? = null,
    val certTypeError: String? = null
) {
    val hasErrors: Boolean
        get() = rollNoError != null ||
                studentNameError != null ||
                fatherNameError != null ||
                courseNameError != null ||
                sessionRangeError != null ||
                durationError != null ||
                gradeError != null ||
                placeOfIssueError != null ||
                dateOfIssueError != null ||
                certTypeError != null
}

/**
 * Centralized, authoritative domain validator for LGES Certificates.
 * Single source of truth across all screens and services.
 */
object CertificateValidator {

    val SUPPORTED_GRADES = setOf("S", "A", "B", "C", "D", "F")
    val SUPPORTED_CERT_TYPES = setOf("Course", "Internship", "Diploma", "Certificate")

    // Roll number pattern: alphanumeric, hyphen, slash, underscore (e.g. 101, LGES-101, 2024/001)
    private val ROLL_NO_REGEX = Regex("""^[A-Za-z0-9/\-_]{1,30}$""")

    // Names: allow letters (including Unicode Latin / Devanagari), dots, spaces, apostrophes
    private val NAME_REGEX = Regex("""^[\p{L}\s.']{2,100}$""")

    // Date formats accepted across modern and legacy entries
    private val DATE_FORMATS = listOf(
        "dd-MM-yyyy",
        "dd/MM/yyyy",
        "yyyy-MM-dd",
        "d MMMM yyyy",
        "dd MMMM yyyy"
    )

    fun validateRollNo(rollNo: String): ValidationResult {
        val clean = rollNo.trim()
        if (clean.isBlank()) {
            return ValidationResult.error("Roll No. / Registration No. is required.")
        }
        if (clean.length > 30) {
            return ValidationResult.error("Roll No. cannot exceed 30 characters.")
        }
        if (!ROLL_NO_REGEX.matches(clean)) {
            return ValidationResult.error("Roll No. contains invalid characters. Use letters, numbers, hyphens, or slashes.")
        }
        return ValidationResult.success()
    }

    fun validateStudentName(name: String): ValidationResult {
        val clean = name.trim()
        if (clean.isBlank()) {
            return ValidationResult.error("Student Name is required.")
        }
        if (clean.length < 2) {
            return ValidationResult.error("Student Name must be at least 2 characters.")
        }
        if (clean.length > 100) {
            return ValidationResult.error("Student Name cannot exceed 100 characters.")
        }
        if (!NAME_REGEX.matches(clean)) {
            return ValidationResult.error("Student Name contains invalid characters.")
        }
        return ValidationResult.success()
    }

    fun validateFatherName(fatherName: String, certType: String): ValidationResult {
        val clean = fatherName.trim()
        val isCourseCert = certType.equals("Course", ignoreCase = true)

        if (clean.isBlank()) {
            return if (isCourseCert) {
                ValidationResult.error("Father / Guardian Name is required for Course certificates.")
            } else {
                ValidationResult.success()
            }
        }
        if (clean.length < 2) {
            return ValidationResult.error("Father / Guardian Name must be at least 2 characters.")
        }
        if (clean.length > 100) {
            return ValidationResult.error("Father / Guardian Name cannot exceed 100 characters.")
        }
        // Strip common prefix before regex check if present
        val stripped = clean
            .removePrefix("S/O ")
            .removePrefix("D/O ")
            .removePrefix("W/O ")
            .removePrefix("C/O ")
            .trim()

        if (!NAME_REGEX.matches(stripped)) {
            return ValidationResult.error("Father / Guardian Name contains invalid characters.")
        }
        return ValidationResult.success()
    }

    fun validateCourseName(course: String): ValidationResult {
        val clean = course.trim()
        if (clean.isBlank()) {
            return ValidationResult.error("Course / Internship title is required.")
        }
        if (clean.length < 2) {
            return ValidationResult.error("Course title must be at least 2 characters.")
        }
        if (clean.length > 120) {
            return ValidationResult.error("Course title cannot exceed 120 characters.")
        }
        return ValidationResult.success()
    }

    fun validateSessionRange(session: String): ValidationResult {
        val clean = session.trim()
        if (clean.isBlank()) {
            return ValidationResult.success() // Optional in some legacy formats
        }
        if (clean.length > 50) {
            return ValidationResult.error("Session range cannot exceed 50 characters.")
        }
        return ValidationResult.success()
    }

    fun validateDuration(duration: String): ValidationResult {
        val clean = duration.trim()
        if (clean.isBlank()) {
            return ValidationResult.success() // Optional in some certificates
        }
        if (clean.length > 50) {
            return ValidationResult.error("Duration cannot exceed 50 characters.")
        }
        return ValidationResult.success()
    }

    fun validateGrade(grade: String): ValidationResult {
        val clean = grade.trim().uppercase(Locale.ROOT)
        if (clean.isBlank()) {
            return ValidationResult.error("Grade is required.")
        }
        if (!SUPPORTED_GRADES.contains(clean)) {
            return ValidationResult.error("Grade '$clean' is not supported. Choose from: ${SUPPORTED_GRADES.joinToString(", ")}.")
        }
        return ValidationResult.success()
    }

    fun validatePlaceOfIssue(place: String): ValidationResult {
        val clean = place.trim()
        if (clean.isBlank()) {
            return ValidationResult.error("Place of Issue is required.")
        }
        if (clean.length < 2 || clean.length > 50) {
            return ValidationResult.error("Place of Issue must be between 2 and 50 characters.")
        }
        return ValidationResult.success()
    }

    fun validateDateOfIssue(dateStr: String): ValidationResult {
        val clean = dateStr.trim()
        if (clean.isBlank()) {
            return ValidationResult.error("Date of Issue is required.")
        }

        var parsedCalendar: Calendar? = null
        for (pattern in DATE_FORMATS) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.ENGLISH).apply {
                    isLenient = false
                }
                val date = sdf.parse(clean)
                if (date != null) {
                    parsedCalendar = Calendar.getInstance().apply { time = date }
                    break
                }
            } catch (_: ParseException) {
                // Try next pattern
            }
        }

        if (parsedCalendar == null) {
            return ValidationResult.error("Date of Issue is not in a valid format (e.g. DD-MM-YYYY).")
        }

        val year = parsedCalendar.get(Calendar.YEAR)
        if (year < 1990 || year > 2100) {
            return ValidationResult.error("Date of Issue year must be between 1990 and 2100.")
        }

        return ValidationResult.success()
    }

    fun validateCertType(type: String): ValidationResult {
        val clean = type.trim()
        if (clean.isBlank()) {
            return ValidationResult.error("Certificate type is required.")
        }
        val match = SUPPORTED_CERT_TYPES.firstOrNull { it.equals(clean, ignoreCase = true) }
        if (match == null) {
            return ValidationResult.error("Invalid certificate type: '$clean'. Choose from: ${SUPPORTED_CERT_TYPES.joinToString(", ")}.")
        }
        return ValidationResult.success()
    }

    /**
     * Validates all fields together and returns comprehensive errors.
     */
    fun validateAll(
        rollNo: String,
        studentName: String,
        fatherName: String,
        courseName: String,
        sessionRange: String,
        duration: String,
        grade: String,
        placeOfIssue: String,
        dateOfIssue: String,
        certType: String
    ): FormValidationErrors {
        return FormValidationErrors(
            rollNoError = validateRollNo(rollNo).errorMessage,
            studentNameError = validateStudentName(studentName).errorMessage,
            fatherNameError = validateFatherName(fatherName, certType).errorMessage,
            courseNameError = validateCourseName(courseName).errorMessage,
            sessionRangeError = validateSessionRange(sessionRange).errorMessage,
            durationError = validateDuration(duration).errorMessage,
            gradeError = validateGrade(grade).errorMessage,
            placeOfIssueError = validatePlaceOfIssue(placeOfIssue).errorMessage,
            dateOfIssueError = validateDateOfIssue(dateOfIssue).errorMessage,
            certTypeError = validateCertType(certType).errorMessage
        )
    }
}
