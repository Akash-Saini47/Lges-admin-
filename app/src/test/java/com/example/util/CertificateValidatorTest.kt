package com.example.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CertificateValidatorTest {

    @Test
    fun testRollNoValidation() {
        assertTrue(CertificateValidator.validateRollNo("101").isValid)
        assertTrue(CertificateValidator.validateRollNo("LGES-2024-001").isValid)
        assertTrue(CertificateValidator.validateRollNo("2024/MCA/05").isValid)

        // Invalid
        assertFalse(CertificateValidator.validateRollNo("").isValid)
        assertFalse(CertificateValidator.validateRollNo("   ").isValid)
        assertFalse(CertificateValidator.validateRollNo("roll@123").isValid)
        assertFalse(CertificateValidator.validateRollNo("a".repeat(35)).isValid)
    }

    @Test
    fun testStudentNameValidation() {
        assertTrue(CertificateValidator.validateStudentName("Rahul Sharma").isValid)
        assertTrue(CertificateValidator.validateStudentName("A. P. J. Abdul Kalam").isValid)
        assertTrue(CertificateValidator.validateStudentName("Avantika Yogi").isValid)

        // Invalid
        assertFalse(CertificateValidator.validateStudentName("").isValid)
        assertFalse(CertificateValidator.validateStudentName("A").isValid)
        assertFalse(CertificateValidator.validateStudentName("Rahul123").isValid)
        assertFalse(CertificateValidator.validateStudentName("Name#Special").isValid)
    }

    @Test
    fun testFatherNameValidation() {
        // Course certificate requires father name
        assertFalse(CertificateValidator.validateFatherName("", "Course").isValid)
        assertTrue(CertificateValidator.validateFatherName("S/O Ramesh Sharma", "Course").isValid)
        assertTrue(CertificateValidator.validateFatherName("Ramesh Sharma", "Course").isValid)

        // Non-course certificate allows empty father name
        assertTrue(CertificateValidator.validateFatherName("", "Internship").isValid)
    }

    @Test
    fun testGradeValidation() {
        assertTrue(CertificateValidator.validateGrade("S").isValid)
        assertTrue(CertificateValidator.validateGrade("A").isValid)
        assertTrue(CertificateValidator.validateGrade("a").isValid) // Case-insensitive
        assertTrue(CertificateValidator.validateGrade("F").isValid)

        assertFalse(CertificateValidator.validateGrade("").isValid)
        assertFalse(CertificateValidator.validateGrade("Z").isValid)
        assertFalse(CertificateValidator.validateGrade("10").isValid)
    }

    @Test
    fun testDateOfIssueValidation() {
        assertTrue(CertificateValidator.validateDateOfIssue("15-08-2024").isValid)
        assertTrue(CertificateValidator.validateDateOfIssue("26 July 2026").isValid)
        assertTrue(CertificateValidator.validateDateOfIssue("2024-12-31").isValid)

        assertFalse(CertificateValidator.validateDateOfIssue("").isValid)
        assertFalse(CertificateValidator.validateDateOfIssue("not-a-date").isValid)
        assertFalse(CertificateValidator.validateDateOfIssue("32-01-2024").isValid)
        assertFalse(CertificateValidator.validateDateOfIssue("15-08-1850").isValid)
    }

    @Test
    fun testCertTypeValidation() {
        assertTrue(CertificateValidator.validateCertType("Course").isValid)
        assertTrue(CertificateValidator.validateCertType("Internship").isValid)
        assertTrue(CertificateValidator.validateCertType("course").isValid)

        assertFalse(CertificateValidator.validateCertType("").isValid)
        assertFalse(CertificateValidator.validateCertType("InvalidType").isValid)
    }

    @Test
    fun testValidateAllCombined() {
        val validErrors = CertificateValidator.validateAll(
            rollNo = "101",
            studentName = "Rahul Sharma",
            fatherName = "S/O Ramesh Sharma",
            courseName = "DCA",
            sessionRange = "2024-2025",
            duration = "1 Year",
            grade = "A",
            placeOfIssue = "CHAMBA",
            dateOfIssue = "15-08-2024",
            certType = "Course"
        )
        assertFalse(validErrors.hasErrors)
        assertNull(validErrors.rollNoError)
        assertNull(validErrors.studentNameError)

        val invalidErrors = CertificateValidator.validateAll(
            rollNo = "",
            studentName = "",
            fatherName = "",
            courseName = "",
            sessionRange = "",
            duration = "",
            grade = "INVALID",
            placeOfIssue = "",
            dateOfIssue = "invalid-date",
            certType = "InvalidType"
        )
        assertTrue(invalidErrors.hasErrors)
    }
}
