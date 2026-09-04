package com.example.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CertificateDao {
    @Query("SELECT * FROM certificates ORDER BY timestamp DESC")
    fun getAllCertificates(): Flow<List<Certificate>>

    @Query("SELECT * FROM certificates WHERE certificateId = :certificateId LIMIT 1")
    suspend fun getCertificateById(certificateId: String): Certificate?

    @Query("SELECT * FROM certificates WHERE rollNo = :rollNo LIMIT 1")
    suspend fun getCertificateByRollNo(rollNo: String): Certificate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCertificate(certificate: Certificate)

    @Update
    suspend fun updateCertificate(certificate: Certificate)

    @Query("UPDATE certificates SET isSynced = :isSynced WHERE certificateId = :certificateId")
    suspend fun updateSyncStatus(certificateId: String, isSynced: Boolean)

    @Query("DELETE FROM certificates WHERE certificateId = :certificateId")
    suspend fun deleteCertificateById(certificateId: String)

    @Query("DELETE FROM certificates WHERE rollNo = :rollNo")
    suspend fun deleteCertificateByRollNo(rollNo: String)

    @Query("DELETE FROM certificates")
    suspend fun deleteAllCertificates()
}
