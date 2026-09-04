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
     * Returns all active certificates, newest first.
     * Excludes records marked for remote deletion.
     */
    @Query("""
        SELECT * FROM certificates 
        WHERE syncStatus NOT IN ('DELETE_PENDING', 'DELETE_FAILED') 
        ORDER BY timestamp DESC
    """)
    fun getAllCertificates(): Flow<List<Certificate>>

    /**
     * Returns ALL certificates including pending deletions (for internal sync manager).
     */
    @Query("SELECT * FROM certificates ORDER BY timestamp DESC")
    suspend fun getAllCertificatesRaw(): List<Certificate>

    /**
     * Returns one certificate by its unique primary key ID.
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
     * Returns ALL certificates having the specified roll number (excluding pending deletions).
     */
    @Query("""
        SELECT * FROM certificates
        WHERE rollNo = :rollNo 
          AND syncStatus NOT IN ('DELETE_PENDING', 'DELETE_FAILED')
        ORDER BY timestamp DESC
    """)
    suspend fun getCertificatesByRollNo(
        rollNo: String
    ): List<Certificate>

    /**
     * Reactive observation of certificates by roll number.
     */
    @Query("""
        SELECT * FROM certificates
        WHERE rollNo = :rollNo
          AND syncStatus NOT IN ('DELETE_PENDING', 'DELETE_FAILED')
        ORDER BY timestamp DESC
    """)
    fun observeCertificatesByRollNo(
        rollNo: String
    ): Flow<List<Certificate>>

    /**
     * Inserts a certificate.
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
     * Updates cloud synchronization state with full metadata.
     */
    @Query("""
        UPDATE certificates
        SET syncStatus = :syncStatus,
            lastSyncTime = :lastSyncTime,
            lastSyncError = :lastSyncError,
            retryCount = :retryCount
        WHERE certificateId = :certificateId
    """)
    suspend fun updateSyncState(
        certificateId: String,
        syncStatus: SyncStatus,
        lastSyncTime: Long? = System.currentTimeMillis(),
        lastSyncError: String? = null,
        retryCount: Int = 0
    )

    /**
     * Updates cloud synchronization state (legacy boolean overload).
     */
    @Query("""
        UPDATE certificates
        SET syncStatus = CASE WHEN :isSynced = 1 THEN 'SYNCED' ELSE 'PENDING' END,
            lastSyncTime = CASE WHEN :isSynced = 1 THEN :currentTime ELSE lastSyncTime END,
            lastSyncError = NULL
        WHERE certificateId = :certificateId
    """)
    suspend fun updateSyncStatus(
        certificateId: String,
        isSynced: Boolean,
        currentTime: Long = System.currentTimeMillis()
    )

    /**
     * Marks a certificate for remote deletion in a durable offline-first queue.
     */
    @Query("""
        UPDATE certificates
        SET syncStatus = 'DELETE_PENDING'
        WHERE certificateId = :certificateId
    """)
    suspend fun markForDeletion(
        certificateId: String
    )

    /**
     * Marks a certificate as failed remote deletion so WorkManager can retry.
     */
    @Query("""
        UPDATE certificates
        SET syncStatus = 'DELETE_FAILED',
            lastSyncError = :error,
            retryCount = retryCount + 1
        WHERE certificateId = :certificateId
    """)
    suspend fun markDeletionFailed(
        certificateId: String,
        error: String?
    )

    /**
     * Returns pending uploads for background synchronization.
     */
    @Query("""
        SELECT * FROM certificates
        WHERE syncStatus IN ('PENDING', 'FAILED')
        ORDER BY timestamp ASC
    """)
    suspend fun getPendingUploads(): List<Certificate>

    /**
     * Returns pending deletions for background synchronization.
     */
    @Query("""
        SELECT * FROM certificates
        WHERE syncStatus IN ('DELETE_PENDING', 'DELETE_FAILED')
        ORDER BY timestamp ASC
    """)
    suspend fun getPendingDeletions(): List<Certificate>

    /**
     * Deletes exactly one certificate from local database by primary key.
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