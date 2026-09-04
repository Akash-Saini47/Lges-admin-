package com.example.database

import kotlinx.coroutines.flow.Flow

class CertificateRepository(private val certificateDao: CertificateDao) {
    val allCertificates: Flow<List<Certificate>> = certificateDao.getAllCertificates()

    suspend fun getById(certificateId: String): Certificate? {
        return certificateDao.getCertificateById(certificateId)
    }

    suspend fun getByRollNo(rollNo: String): Certificate? {
        return certificateDao.getCertificateByRollNo(rollNo)
    }

    suspend fun insert(certificate: Certificate) {
        certificateDao.insertCertificate(certificate)
    }

    suspend fun update(certificate: Certificate) {
        certificateDao.updateCertificate(certificate)
    }

    suspend fun updateSyncStatus(certificateId: String, isSynced: Boolean) {
        certificateDao.updateSyncStatus(certificateId, isSynced)
    }

    suspend fun delete(certificateIdOrRollNo: String) {
        certificateDao.deleteCertificateById(certificateIdOrRollNo)
        certificateDao.deleteCertificateByRollNo(certificateIdOrRollNo)
    }

    suspend fun deleteAll() {
        certificateDao.deleteAllCertificates()
    }
}
