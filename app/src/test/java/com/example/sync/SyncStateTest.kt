package com.example.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.database.Certificate
import com.example.database.CertificateDao
import com.example.database.CertificateDatabase
import com.example.database.CertificateRepository
import com.example.database.SyncStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
class SyncStateTest {

    private lateinit var db: CertificateDatabase
    private lateinit var dao: CertificateDao
    private lateinit var repository: CertificateRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, CertificateDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.certificateDao()
        repository = CertificateRepository(dao)
    }

    @After
    @Throws(IOException::class)
    fun teardown() {
        db.close()
    }

    @Test
    fun testPendingUploadQueue() = runBlocking {
        val cert1 = Certificate.create(
            rollNo = "101",
            studentName = "Student 1",
            fatherName = "Parent 1",
            courseName = "Course 1",
            sessionRange = "2024",
            duration = "1 Year",
            grade = "A",
            placeOfIssue = "CHAMBA",
            dateOfIssue = "01-01-2024",
            certType = "Course",
            syncStatus = SyncStatus.PENDING
        )

        val cert2 = Certificate.create(
            rollNo = "102",
            studentName = "Student 2",
            fatherName = "Parent 2",
            courseName = "Course 2",
            sessionRange = "2024",
            duration = "1 Year",
            grade = "S",
            placeOfIssue = "CHAMBA",
            dateOfIssue = "02-01-2024",
            certType = "Course",
            syncStatus = SyncStatus.SYNCED
        )

        repository.insert(cert1)
        repository.insert(cert2)

        val pending = repository.getPendingUploads()
        assertEquals(1, pending.size)
        assertEquals(cert1.certificateId, pending.first().certificateId)
    }

    @Test
    fun testDurableDeletionQueue() = runBlocking {
        val cert = Certificate.create(
            rollNo = "201",
            studentName = "To Be Deleted",
            fatherName = "Parent",
            courseName = "Course",
            sessionRange = "2024",
            duration = "6 Months",
            grade = "B",
            placeOfIssue = "CHAMBA",
            dateOfIssue = "01-01-2024",
            certType = "Course",
            syncStatus = SyncStatus.SYNCED
        )

        repository.insert(cert)
        val certId = cert.certificateId

        // Mark for deletion in offline queue
        repository.markForDeletion(certId)

        val pendingDeletes = repository.getPendingDeletions()
        assertEquals(1, pendingDeletes.size)
        assertEquals(SyncStatus.DELETE_PENDING, pendingDeletes.first().syncStatus)

        // Active certificates query must exclude records marked for deletion
        val activeCertificates = dao.getCertificatesByRollNo("201")
        assertTrue("Active query must filter out DELETE_PENDING", activeCertificates.isEmpty())

        // Simulate remote delete failure -> mark DELETE_FAILED for WorkManager retry
        repository.markDeletionFailed(certId, "HTTP 503 Service Unavailable")
        val failedDeletes = repository.getPendingDeletions()
        assertEquals(1, failedDeletes.size)
        assertEquals(SyncStatus.DELETE_FAILED, failedDeletes.first().syncStatus)
        assertEquals(1, failedDeletes.first().retryCount)

        // Simulate successful remote delete -> purge from local database
        repository.delete(certId)
        assertNull(repository.getById(certId))
        assertTrue(repository.getPendingDeletions().isEmpty())
    }

    @Test
    fun testSyncStateTransitions() = runBlocking {
        val cert = Certificate.create(
            rollNo = "301",
            studentName = "State Tester",
            fatherName = "Parent",
            courseName = "DCA",
            sessionRange = "2024",
            duration = "1 Year",
            grade = "A",
            placeOfIssue = "CHAMBA",
            dateOfIssue = "01-01-2024",
            certType = "Course",
            syncStatus = SyncStatus.PENDING
        )

        val certId = cert.certificateId
        repository.insert(cert)

        // Transition: PENDING -> SYNCING
        repository.updateSyncState(certId, SyncStatus.SYNCING)
        assertEquals(SyncStatus.SYNCING, repository.getById(certId)?.syncStatus)

        // Transition: SYNCING -> FAILED
        repository.updateSyncState(certId, SyncStatus.FAILED, lastSyncError = "Timeout", retryCount = 1)
        val failed = repository.getById(certId)
        assertEquals(SyncStatus.FAILED, failed?.syncStatus)
        assertEquals("Timeout", failed?.lastSyncError)
        assertEquals(1, failed?.retryCount)

        // Transition: FAILED -> SYNCED
        repository.updateSyncState(certId, SyncStatus.SYNCED, lastSyncError = null, retryCount = 0)
        val synced = repository.getById(certId)
        assertEquals(SyncStatus.SYNCED, synced?.syncStatus)
        assertNull(synced?.lastSyncError)
        assertEquals(0, synced?.retryCount)
    }
}
