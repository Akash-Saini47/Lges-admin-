package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.database.Certificate
import com.example.database.CertificateDao
import com.example.database.CertificateDatabase
import com.example.util.CertificateConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
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
    fun testCertificateIdFormatting() {
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
            baseUrl = "https://lges-computer-classes.netlify.app",
            certificateId = "LGES-4512"
        )
        assertEquals("https://lges-computer-classes.netlify.app?certNo=LGES-4512", url)
    }

    @Test
    fun testRoomDatabaseInsertAndQuery() = runBlocking {
        val testCert = Certificate.create(
            rollNo = "1001",
            studentName = "Rahul Sharma",
            fatherName = "S/O Ramesh Sharma",
            courseName = "DCA",
            sessionRange = "2024-2025",
            duration = "1 Year",
            grade = "A",
            placeOfIssue = "Sikanderpur",
            dateOfIssue = "15-08-2024",
            certType = "Course",
            isSynced = false
        )

        dao.insertCertificate(testCert)

        val retrieved = dao.getCertificateById("LGES-1001")
        assertNotNull(retrieved)
        assertEquals("Rahul Sharma", retrieved?.studentName)
        assertEquals("1001", retrieved?.rollNo)
        assertFalse(retrieved?.isSynced ?: true)

        val list = dao.getAllCertificates().first()
        assertEquals(1, list.size)
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
            placeOfIssue = "Sikanderpur",
            dateOfIssue = "20-08-2024",
            certType = "Course",
            isSynced = false
        )

        dao.insertCertificate(testCert)
        dao.updateSyncStatus("LGES-2002", true)

        val updated = dao.getCertificateById("LGES-2002")
        assertNotNull(updated)
        assertTrue(updated?.isSynced ?: false)
    }

    @Test
    fun testRoomDatabaseDelete() = runBlocking {
        val testCert = Certificate.create(
            rollNo = "3003",
            studentName = "Amit Kumar",
            fatherName = "Mahesh Kumar",
            courseName = "Web Development",
            sessionRange = "2024-2025",
            duration = "6 Months",
            grade = "A",
            placeOfIssue = "Sikanderpur",
            dateOfIssue = "25-08-2024",
            certType = "Internship",
            isSynced = true
        )

        dao.insertCertificate(testCert)
        assertEquals(1, dao.getAllCertificates().first().size)

        dao.deleteCertificateById("LGES-3003")
        assertEquals(0, dao.getAllCertificates().first().size)
    }

    @Test
    fun testCertificateDrawerTemplateRendering() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cert = Certificate.create(
            rollNo = "LGES24K001",
            studentName = "Avantika Yogi",
            fatherName = "Naval Kishor Yogi",
            courseName = "ADITCM",
            sessionRange = "2025–2026",
            duration = "450 Hours",
            grade = "S",
            placeOfIssue = "Kanta",
            dateOfIssue = "26 July 2026",
            certType = "Course",
            isSynced = false
        )

        val bitmap = com.example.ui.CertificateDrawer.drawCertificate(context, cert, null)
        assertNotNull(bitmap)
        assertEquals(2400, bitmap.width)
        assertEquals(1600, bitmap.height)
    }
}
