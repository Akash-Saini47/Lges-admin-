package com.example.database

import kotlinx.coroutines.flow.Flow

class CertificateRepository(
    private val certificateDao: CertificateDao
) {

    /**
     * All certificates, newest first.
     */
    val allCertificates: Flow<List<Certificate>>
        get() = certificateDao.getAllCertificates()

    /**
     * Get one certificate by unique certificate ID.
     */
    suspend fun getById(
        certificateId: String
    ): Certificate? {
        return certificateDao.getCertificateById(
            certificateId.trim()
        )
    }

    /**
     * Get ALL certificates associated with a roll number.
     */
    suspend fun getByRollNo(
        rollNo: String
    ): List<Certificate> {
        return certificateDao.getCertificatesByRollNo(
            rollNo.trim()
        )
    }

    /**
     * Observe all certificates associated with a roll number.
     */
    fun observeByRollNo(
        rollNo: String
    ): Flow<List<Certificate>> {
        return certificateDao.observeCertificatesByRollNo(
            rollNo.trim()
        )
    }

    /**
     * Insert a certificate.
     */
    suspend fun insert(
        certificate: Certificate
    ) {
        certificateDao.insertCertificate(certificate)
    }

    /**
     * Update a certificate.
     */
    suspend fun update(
        certificate: Certificate
    ) {
        certificateDao.updateCertificate(certificate)
    }

    /**
     * Update cloud synchronization state.
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
     * Delete exactly one certificate.
     *
     * IMPORTANT:
     * This method intentionally treats the supplied value
     * as a certificate ID, not as either ID-or-roll-number.
     *
     * This prevents accidental deletion of multiple certificates.
     */
    suspend fun delete(
        certificateId: String
    ) {
        certificateDao.deleteCertificateById(
            certificateId.trim()
        )
    }

    /**
     * Delete every certificate belonging to a roll number.
     *
     * This should be used only when that behavior is explicitly required.
     */
    suspend fun deleteByRollNo(
        rollNo: String
    ) {
        certificateDao.deleteCertificatesByRollNo(
            rollNo.trim()
        )
    }

    /**
     * Delete all certificates.
     */
    suspend fun deleteAll() {
        certificateDao.deleteAllCertificates()
    }
}