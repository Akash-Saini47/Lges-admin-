package com.example.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.util.AppLogger

@Database(
    entities = [Certificate::class],
    version = 4,
    exportSchema = true
)
abstract class CertificateDatabase : RoomDatabase() {

    abstract fun certificateDao(): CertificateDao

    companion object {

        @Volatile
        private var INSTANCE: CertificateDatabase? = null

        /**
         * Migration from database version 1 to 2.
         *
         * Critical Fix:
         * In schema v1, the table had `rollNo` as the primary key and
         * contained NO `certificateId` or `isSynced` columns.
         * We derive `certificateId` from `rollNo` ('LGES-' || rollNo)
         * and initialize `isSynced` to 0.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                AppLogger.i("CertificateDatabase", "Running Room MIGRATION_1_2")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS certificates_new (
                        certificateId TEXT NOT NULL PRIMARY KEY,
                        rollNo TEXT NOT NULL,
                        studentName TEXT NOT NULL,
                        fatherName TEXT NOT NULL,
                        courseName TEXT NOT NULL,
                        sessionRange TEXT NOT NULL,
                        duration TEXT NOT NULL,
                        grade TEXT NOT NULL,
                        placeOfIssue TEXT NOT NULL,
                        dateOfIssue TEXT NOT NULL,
                        certType TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        isSynced INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO certificates_new (
                        certificateId,
                        rollNo,
                        studentName,
                        fatherName,
                        courseName,
                        sessionRange,
                        duration,
                        grade,
                        placeOfIssue,
                        dateOfIssue,
                        certType,
                        timestamp,
                        isSynced
                    )
                    SELECT
                        'LGES-' || rollNo,
                        rollNo,
                        studentName,
                        fatherName,
                        courseName,
                        sessionRange,
                        duration,
                        grade,
                        placeOfIssue,
                        dateOfIssue,
                        certType,
                        timestamp,
                        0
                    FROM certificates
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE certificates")
                db.execSQL("ALTER TABLE certificates_new RENAME TO certificates")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_certificates_rollNo ON certificates(rollNo)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_certificates_timestamp ON certificates(timestamp)")
            }
        }

        /**
         * Migration from version 2 to version 3.
         * Adds timestamp index for faster date sorting.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                AppLogger.i("CertificateDatabase", "Running Room MIGRATION_2_3")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_certificates_timestamp ON certificates(timestamp)")
            }
        }

        /**
         * Migration from version 3 to version 4.
         *
         * Hardens the cloud synchronization engine:
         * - Introduces syncStatus ('PENDING', 'SYNCING', 'SYNCED', 'FAILED', 'DELETE_PENDING', 'DELETE_FAILED')
         * - Adds lastSyncTime, retryCount, and lastSyncError
         * - Non-destructively replaces legacy isSynced column with syncStatus to match Room schema version 4
         * - Recreates the certificates table with exact Room schema and indices
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                AppLogger.i("CertificateDatabase", "Running Room MIGRATION_3_4")

                // Step 1: Create table matching Room entity schema version 4
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS certificates_new (
                        certificateId TEXT NOT NULL PRIMARY KEY,
                        rollNo TEXT NOT NULL,
                        studentName TEXT NOT NULL,
                        fatherName TEXT NOT NULL,
                        courseName TEXT NOT NULL,
                        sessionRange TEXT NOT NULL,
                        duration TEXT NOT NULL,
                        grade TEXT NOT NULL,
                        placeOfIssue TEXT NOT NULL,
                        dateOfIssue TEXT NOT NULL,
                        certType TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        syncStatus TEXT NOT NULL,
                        lastSyncTime INTEGER,
                        retryCount INTEGER NOT NULL,
                        lastSyncError TEXT
                    )
                    """.trimIndent()
                )

                // Inspect existing columns to handle any prior partial alterations gracefully
                val cursor = db.query("PRAGMA table_info(certificates)")
                val existingColumns = mutableSetOf<String>()
                cursor.use {
                    val nameIdx = it.getColumnIndex("name")
                    while (it.moveToNext()) {
                        if (nameIdx != -1) {
                            existingColumns.add(it.getString(nameIdx))
                        }
                    }
                }

                val syncStatusSql = when {
                    existingColumns.contains("syncStatus") && existingColumns.contains("isSynced") ->
                        "CASE WHEN syncStatus IS NOT NULL AND syncStatus != '' THEN syncStatus WHEN isSynced = 1 THEN 'SYNCED' ELSE 'PENDING' END"
                    existingColumns.contains("syncStatus") ->
                        "COALESCE(syncStatus, 'PENDING')"
                    existingColumns.contains("isSynced") ->
                        "CASE WHEN isSynced = 1 THEN 'SYNCED' ELSE 'PENDING' END"
                    else ->
                        "'PENDING'"
                }

                val lastSyncTimeSql = if (existingColumns.contains("lastSyncTime")) "lastSyncTime" else "NULL"
                val retryCountSql = if (existingColumns.contains("retryCount")) "COALESCE(retryCount, 0)" else "0"
                val lastSyncErrorSql = if (existingColumns.contains("lastSyncError")) "lastSyncError" else "NULL"

                // Step 2: Copy data from certificates into certificates_new
                db.execSQL(
                    """
                    INSERT INTO certificates_new (
                        certificateId, rollNo, studentName, fatherName, courseName,
                        sessionRange, duration, grade, placeOfIssue, dateOfIssue,
                        certType, timestamp, syncStatus, lastSyncTime, retryCount, lastSyncError
                    )
                    SELECT
                        certificateId, rollNo, studentName, fatherName, courseName,
                        sessionRange, duration, grade, placeOfIssue, dateOfIssue,
                        certType, timestamp,
                        $syncStatusSql,
                        $lastSyncTimeSql,
                        $retryCountSql,
                        $lastSyncErrorSql
                    FROM certificates
                    """.trimIndent()
                )

                // Step 3: Drop old table and rename new table
                db.execSQL("DROP TABLE certificates")
                db.execSQL("ALTER TABLE certificates_new RENAME TO certificates")

                // Step 4: Recreate indices expected by Room version 4
                db.execSQL("CREATE INDEX IF NOT EXISTS index_certificates_rollNo ON certificates(rollNo)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_certificates_timestamp ON certificates(timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_certificates_syncStatus ON certificates(syncStatus)")
            }
        }

        /**
         * Returns the singleton database instance with all historical migrations registered.
         */
        fun getDatabase(context: Context): CertificateDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CertificateDatabase::class.java,
                    "certificate_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4
                    )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also {
                        INSTANCE = it
                    }
            }
        }
    }
}