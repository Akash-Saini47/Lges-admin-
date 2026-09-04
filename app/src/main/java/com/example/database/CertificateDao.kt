package com.example.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CertificateDao {

    /**
     * Returns all certificates, newest first.
     */
    @Query("SELECT * FROM certificates ORDER BY timestamp DESC")
    fun getAllCertificates(): Flow<List<Certificate>>

    /**
     * Returns one certificate by its unique certificate ID.
     */
    @Query("""
        SELECT * FROM certificates
        WHERE certificateId = :certificateId
        LIMIT 1
    """)
    suspend fun getCertificateById(
        certificateId: String
    ): Certificate?

    /**
     * Returns ALL certificates having the specified roll number.
     *
     * Do NOT use LIMIT 1 here because one roll number may
     * eventually be associated with multiple certificates.
     */
    @Query("""
        SELECT * FROM certificates
        WHERE rollNo = :rollNo
        ORDER BY timestamp DESC
    """)
    suspend fun getCertificatesByRollNo(
        rollNo: String
    ): List<Certificate>

    /**
     * Reactive version for observing certificates by roll number.
     */
    @Query("""
        SELECT * FROM certificates
        WHERE rollNo = :rollNo
        ORDER BY timestamp DESC
    """)
    fun observeCertificatesByRollNo(
        rollNo: String
    ): Flow<List<Certificate>>

    /**
     * Inserts a certificate.
     *
     * REPLACE is retained for compatibility with the existing
     * application. Since certificateId is the primary key,
     * replacing only affects the same certificate ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCertificate(
        certificate: Certificate
    )

    /**
     * Updates an existing certificate.
     */
    @Update
    suspend fun updateCertificate(
        certificate: Certificate
    )

    /**
     * Updates cloud synchronization state.
     */
    @Query("""
        UPDATE certificates
        SET isSynced = :isSynced
        WHERE certificateId = :certificateId
    """)
    suspend fun updateSyncStatus(
        certificateId: String,
        isSynced: Boolean
    )

    /**
     * Deletes exactly one certificate.
     */
    @Query("""
        DELETE FROM certificates
        WHERE certificateId = :certificateId
    """)
    suspend fun deleteCertificateById(
        certificateId: String
    )

    /**
     * Deletes all certificates associated with a roll number.
     *
     * Use carefully: if multiple certificates share a roll number,
     * all of them will be deleted.
     */
    @Query("""
        DELETE FROM certificates
        WHERE rollNo = :rollNo
    """)
    suspend fun deleteCertificatesByRollNo(
        rollNo: String
    )

    /**
     * Deletes every certificate.
     */
    @Query("DELETE FROM certificates")
    suspend fun deleteAllCertificates()
}