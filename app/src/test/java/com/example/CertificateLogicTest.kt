package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.database.Certificate
import com.example.database.CertificateDao
import com.example.database.CertificateDatabase
import com.example.database.SyncStatus
import com.example.util.CertificateConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CertificateLogicTest {

    private lateinit var db: CertificateDatabase
    private lateinit var dao: CertificateDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, CertificateDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.certificateDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun testModernCertificateIdGeneration() {
        val id1 = CertificateConfig.generateCertificateId()
        val id2 = CertificateConfig.generateCertificateId()

        assertTrue("ID should start with LGES-", id1.startsWith("LGES-"))
        assertTrue("ID should be valid certificate ID", CertificateConfig.isCertificateId(id1))
        assertFalse("Two generated IDs must be unique", id1 == id2)
    }

    @Test
    fun testLegacyCertificateIdFormatting() {
        val certId = CertificateConfig.computeCertificateId("4512")
        assertEquals("LGES-4512", certId)

        val alreadyPrefixed = CertificateConfig.computeCertificateId("LGES-4512")
        assertEquals("LGES-4512", alreadyPrefixed)

        val trimmed = CertificateConfig.computeCertificateId("  9876  ")
        assertEquals("LGES-9876", trimmed)
    }

    @Test
    fun testQrVerificationUrl() {
        val url = CertificateConfig.buildVerificationUrl(
            baseUrl = "https://lges-computer-classes.netlify.app/verify.html",
            certificateId = "LGES-2026-A1B2C3D4E5F6"
        )
        assertEquals("https://lges-computer-classes.netlify.app/verify.html?certNo=LGES-2026-A1B2C3D4E5F6", url)
    }

    @Test
    fun testRoomDatabaseInsertAndQueryWithModernId() = runBlocking {
        val testCert = Certificate.create(
            rollNo = "1001",
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

        val assignedId = testCert.certificateId
        dao.insertCertificate(testCert)

        val retrieved = dao.getCertificateById(assignedId)
        assertNotNull(retrieved)
        assertEquals("Rahul Sharma", retrieved?.studentName)
        assertEquals("1001", retrieved?.rollNo)
        assertEquals(SyncStatus.PENDING, retrieved?.syncStatus)
        assertFalse(retrieved?.isSynced ?: true)

        val list = dao.getAllCertificates().first()
        assertEquals(1, list.size)
    }

    @Test
    fun testMultipleCertificatesPerRollNumber() = runBlocking {
        val cert1 = Certificate.create(
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

        val cert2 = Certificate.create(
            rollNo = "101",
            studentName = "Rahul Sharma",
            fatherName = "S/O Ramesh Sharma",
            courseName = "Python Internship",
            sessionRange = "2025-2026",
            duration = "6 Months",
            grade = "S",
            placeOfIssue = "CHAMBA",
            dateOfIssue = "20-01-2026",
            certType = "Internship"
        )

        dao.insertCertificate(cert1)
        dao.insertCertificate(cert2)

        val studentCertificates = dao.getCertificatesByRollNo("101")
        assertEquals(2, studentCertificates.size)
        assertTrue(studentCertificates.any { it.certType == "Course" })
        assertTrue(studentCertificates.any { it.certType == "Internship" })
        assertFalse(cert1.certificateId == cert2.certificateId)
    }

    @Test
    fun testRoomDatabaseUpdateAndMarkSynced() = runBlocking {
        val testCert = Certificate.create(
            rollNo = "2002",
            studentName = "Priya Verma",
            fatherName = "D/O Suresh Verma",
            courseName = "ADCA",
            sessionRange = "2024-2025",
            duration = "1 Year",
            grade = "S",
            placeOfIssue = "CHAMBA",
            dateOfIssue = "20-08-2024",
            certType = "Course"
        )

        dao.insertCertificate(testCert)
        val certId = testCert.certificateId

        // Update sync state to SYNCED
        dao.updateSyncState(
            certificateId = certId,
            syncStatus = SyncStatus.SYNCED,
            lastSyncTime = System.currentTimeMillis()
        )

        val updated = dao.getCertificateById(certId)
        assertNotNull(updated)
        assertEquals(SyncStatus.SYNCED, updated?.syncStatus)
        assertTrue(updated?.isSynced ?: false)
    }

    @Test
    fun testRoomDatabaseDeleteStrictlyById() = runBlocking {
        val testCert = Certificate.create(
            rollNo = "3003",
            studentName = "Amit Kumar",
            fatherName = "Mahesh Kumar",
            courseName = "Web Development",
            sessionRange = "2024-2025",
            duration = "6 Months",
            grade = "A",
            placeOfIssue = "CHAMBA",
            dateOfIssue = "25-08-2024",
            certType = "Internship"
        )

        val certId = testCert.certificateId
        dao.insertCertificate(testCert)
        assertEquals(1, dao.getAllCertificates().first().size)

        dao.deleteCertificateById(certId)
        assertNull(dao.getCertificateById(certId))
        assertEquals(0, dao.getAllCertificates().first().size)
    }

    @Test
    fun testCustomIdPreservationOnEdit() {
        val legacyId = "LGES-999"
        val cert = Certificate.create(
            rollNo = "999",
            studentName = "Historical Student",
            fatherName = "Parent",
            courseName = "Legacy Course",
            sessionRange = "2020-2021",
            duration = "1 Year",
            grade = "A",
            placeOfIssue = "CHAMBA",
            dateOfIssue = "01-01-2021",
            certType = "Course",
            customId = legacyId
        )

        assertEquals("Custom/legacy ID must be preserved exactly", legacyId, cert.certificateId)
    }
}
