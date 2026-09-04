package com.example.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.database.CertificateDatabase
import com.example.database.CertificateRepository
import com.example.database.GoogleSheetsService
import com.example.database.SyncResult
import com.example.database.SyncStatus
import com.example.util.AppLogger
import com.example.util.CertificateConfig
import com.example.util.SecureSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Durable background worker that processes queued certificate synchronizations and deletions.
 * Uses certificateId as the idempotency key to prevent duplicates on retries.
 */
class SyncCertificateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME_PERIODIC = "LGES_CERT_SYNC_PERIODIC"
        const val WORK_NAME_ONETIME = "LGES_CERT_SYNC_ONETIME"
        private const val TAG = "SyncCertificateWorker"
        private const val MAX_RETRIES = 5
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val database = CertificateDatabase.getDatabase(applicationContext)
        val repository = CertificateRepository(database.certificateDao())
        val secureSettings = SecureSettings.getInstance(applicationContext)

        val webAppUrl = secureSettings.getWebAppUrl()
        val apiKey = secureSettings.getApiKey()
        val verificationBaseUrl = secureSettings.getVerificationBaseUrl()

        if (!CertificateConfig.isValidHttpUrl(webAppUrl)) {
            AppLogger.w(TAG, "Sync skipped: Invalid or unconfigured Web App URL.")
            return@withContext Result.success()
        }

        var anyTransientFailure = false

        // 1. Process Pending Deletions FIRST (Durable remote deletion queue)
        val pendingDeletions = repository.getPendingDeletions()
        AppLogger.i(TAG, "Processing ${pendingDeletions.size} pending remote deletions.")

        for (cert in pendingDeletions) {
            val certId = cert.certificateId
            val deleteResult = GoogleSheetsService.deleteCertificateRemotely(
                webAppUrl = webAppUrl,
                certificateId = certId,
                apiKey = apiKey
            )

            when (deleteResult) {
                is SyncResult.Success -> {
                    AppLogger.i(TAG, "Remote deletion succeeded for $certId. Purging local record.")
                    repository.delete(certId)
                }
                is SyncResult.Error -> {
                    AppLogger.w(TAG, "Remote deletion failed for $certId: ${deleteResult.message}")
                    repository.markDeletionFailed(certId, deleteResult.message)
                    if (deleteResult.isRetryable) {
                        anyTransientFailure = true
                    }
                }
            }
        }

        // 2. Process Pending Uploads / Updates
        val pendingUploads = repository.getPendingUploads()
        AppLogger.i(TAG, "Processing ${pendingUploads.size} pending certificate uploads.")

        for (cert in pendingUploads) {
            val certId = cert.certificateId

            // Mark as syncing in Room
            repository.updateSyncState(
                certificateId = certId,
                syncStatus = SyncStatus.SYNCING,
                retryCount = cert.retryCount
            )

            val syncResult = GoogleSheetsService.syncCertificate(
                webAppUrl = webAppUrl,
                certificate = cert,
                apiKey = apiKey,
                action = "save",
                verificationBaseUrl = verificationBaseUrl
            )

            when (syncResult) {
                is SyncResult.Success -> {
                    AppLogger.i(TAG, "Cloud sync successful for $certId")
                    repository.updateSyncState(
                        certificateId = certId,
                        syncStatus = SyncStatus.SYNCED,
                        lastSyncTime = System.currentTimeMillis(),
                        lastSyncError = null,
                        retryCount = 0
                    )
                }
                is SyncResult.Error -> {
                    AppLogger.w(TAG, "Cloud sync failed for $certId: ${syncResult.message}")
                    val newRetryCount = cert.retryCount + 1
                    repository.updateSyncState(
                        certificateId = certId,
                        syncStatus = SyncStatus.FAILED,
                        lastSyncTime = System.currentTimeMillis(),
                        lastSyncError = syncResult.message,
                        retryCount = newRetryCount
                    )

                    if (syncResult.isRetryable) {
                        anyTransientFailure = true
                    }
                }
            }
        }

        if (anyTransientFailure && runAttemptCount < MAX_RETRIES) {
            AppLogger.i(TAG, "Retrying worker due to transient network failures.")
            Result.retry()
        } else {
            Result.success()
        }
    }
}
