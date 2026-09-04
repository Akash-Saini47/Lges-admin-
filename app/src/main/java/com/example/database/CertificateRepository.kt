package com.example.database

import kotlinx.coroutines.flow.Flow

class CertificateRepository(private val certificateDao: CertificateDao) {
    val allCertificates: Flow<List<Certificate>> = certificateDao.getAllCertificates()

    suspend fun insert(certificate: Certificate) {
        certificateDao.insertCertificate(certificate)
    }

    suspend fun delete(rollNo: String) {
        certificateDao.deleteCertificateByRollNo(rollNo)
    }
}
