package com.example.database

import kotlinx.coroutines.flow.Flow

class CertificateRepository(
    private val certificateDao: CertificateDao
) {

    /**
     * All active certificates, newest first.
     */
    val allCertificates: Flow<List<Certificate>>
        get() = certificateDao.getAllCertificates()

    /**
     * Get one certificate by unique primary key certificate ID.
     */
    suspend fun getById(certificateId: String): Certificate? {
        return certificateDao.getCertificateById(certificateId.trim())
    }

    /**
     * Get ALL certificates associated with a roll number.
     */
    suspend fun getByRollNo(rollNo: String): List<Certificate> {
        return certificateDao.getCertificatesByRollNo(rollNo.trim())
    }

    /**
     * Observe all certificates associated with a roll number.
     */
    fun observeByRollNo(rollNo: String): Flow<List<Certificate>> {
        return certificateDao.observeCertificatesByRollNo(rollNo.trim())
    }

    /**
     * Insert a certificate.
     */
    suspend fun insert(certificate: Certificate) {
        certificateDao.insertCertificate(certificate)
    }

    /**
     * Update a certificate.
     */
    suspend fun update(certificate: Certificate) {
        certificateDao.updateCertificate(certificate)
    }

    /**
     * Update cloud synchronization state with full metadata.
     */
    suspend fun updateSyncState(
        certificateId: String,
        syncStatus: SyncStatus,
        lastSyncTime: Long? = System.currentTimeMillis(),
        lastSyncError: String? = null,
        retryCount: Int = 0
    ) {
        certificateDao.updateSyncState(
            certificateId = certificateId.trim(),
            syncStatus = syncStatus,
            lastSyncTime = lastSyncTime,
            lastSyncError = lastSyncError,
            retryCount = retryCount
        )
    }

    /**
     * Update cloud synchronization state (legacy overload).
     */
    suspend fun updateSyncStatus(
        certificateId: String,
        isSynced: Boolean
    ) {
        certificateDao.updateSyncStatus(
            certificateId = certificateId.trim(),
            isSynced = isSynced
        )
    }

    /**
     * Marks a certificate for remote deletion in a durable offline-first queue.
     */
    suspend fun markForDeletion(certificateId: String) {
        certificateDao.markForDeletion(certificateId.trim())
    }

    /**
     * Marks a certificate as failed remote deletion so it can be retried.
     */
    suspend fun markDeletionFailed(certificateId: String, error: String?) {
        certificateDao.markDeletionFailed(certificateId.trim(), error)
    }

    /**
     * Fetches pending uploads for synchronization.
     */
    suspend fun getPendingUploads(): List<Certificate> {
        return certificateDao.getPendingUploads()
    }

    /**
     * Fetches pending deletions for synchronization.
     */
    suspend fun getPendingDeletions(): List<Certificate> {
        return certificateDao.getPendingDeletions()
    }

    /**
     * Permanently deletes exactly one certificate by its primary key.
     */
    suspend fun delete(certificateId: String) {
        certificateDao.deleteCertificateById(certificateId.trim())
    }

    /**
     * Alias for delete() to provide consistent API across ViewModel and callers.
     */
    suspend fun deleteById(certificateId: String) {
        delete(certificateId)
    }

    /**
     * Delete every certificate belonging to a roll number.
     */
    suspend fun deleteByRollNo(rollNo: String) {
        certificateDao.deleteCertificatesByRollNo(rollNo.trim())
    }

    /**
     * Delete all certificates.
     */
    suspend fun deleteAll() {
        certificateDao.deleteAllCertificates()
    }
}