package com.example.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CertificateDao {
    @Query("SELECT * FROM certificates ORDER BY timestamp DESC")
    fun getAllCertificates(): Flow<List<Certificate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCertificate(certificate: Certificate)

    @Query("DELETE FROM certificates WHERE rollNo = :rollNo")
    suspend fun deleteCertificateByRollNo(rollNo: String)
}
