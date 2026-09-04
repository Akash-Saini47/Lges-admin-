package com.example.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.database.Certificate
import com.example.database.CertificateRepository
import com.example.database.SyncStatus
import com.example.util.AppLogger
import java.util.concurrent.TimeUnit

/**
 * Coordination manager for scheduling and triggering certificate cloud synchronization.
 */
object SyncManager {

    private const val TAG = "SyncManager"

    private val networkConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /**
     * Enqueues an immediate background sync run using WorkManager.
     */
    fun scheduleImmediateSync(context: Context) {
        try {
            val workRequest = OneTimeWorkRequestBuilder<SyncCertificateWorker>()
                .setConstraints(networkConstraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15,
                    TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                SyncCertificateWorker.WORK_NAME_ONETIME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
            AppLogger.d(TAG, "Enqueued immediate sync work.")
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to enqueue WorkManager job: ${e.message}")
        }
    }

    /**
     * Schedules periodic background synchronization to ensure offline-created records are synced.
     */
    fun schedulePeriodicSync(context: Context) {
        try {
            val periodicRequest = PeriodicWorkRequestBuilder<SyncCertificateWorker>(
                6,
                TimeUnit.HOURS
            )
                .setConstraints(networkConstraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                SyncCertificateWorker.WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
            AppLogger.d(TAG, "Enqueued periodic sync work (6 hours).")
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to schedule periodic work: ${e.message}")
        }
    }

    /**
     * Queues a certificate for deletion in an offline-first durable manner.
     * Record is preserved with status DELETE_PENDING until remote deletion succeeds.
     */
    suspend fun queueForDeletion(
        context: Context,
        repository: CertificateRepository,
        certificateId: String
    ) {
        val existing = repository.getById(certificateId)
        if (existing == null) {
            AppLogger.w(TAG, "Certificate $certificateId not found for deletion.")
            return
        }

        // If certificate was never synced to cloud, delete locally immediately
        if (existing.syncStatus == SyncStatus.PENDING && existing.lastSyncTime == null) {
            repository.delete(certificateId)
            AppLogger.i(TAG, "Certificate $certificateId was purely local (never synced). Deleted directly.")
            return
        }

        // Otherwise mark DELETE_PENDING in Room
        repository.markForDeletion(certificateId)
        AppLogger.i(TAG, "Certificate $certificateId marked DELETE_PENDING.")

        // Enqueue background work to sync deletion
        scheduleImmediateSync(context)
    }

    /**
     * Saves a certificate locally with PENDING status and queues synchronization.
     */
    suspend fun saveAndQueueSync(
        context: Context,
        repository: CertificateRepository,
        certificate: Certificate,
        isUpdate: Boolean
    ) {
        val pendingCert = certificate.copy(
            syncStatus = SyncStatus.PENDING,
            lastSyncError = null
        )

        if (isUpdate) {
            repository.update(pendingCert)
        } else {
            repository.insert(pendingCert)
        }

        scheduleImmediateSync(context)
    }
}
