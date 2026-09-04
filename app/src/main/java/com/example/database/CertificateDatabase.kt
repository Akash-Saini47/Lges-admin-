package com.example.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Certificate::class], version = 2, exportSchema = false)
abstract class CertificateDatabase : RoomDatabase() {
    abstract fun certificateDao(): CertificateDao

    companion object {
        @Volatile
        private var INSTANCE: CertificateDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
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
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO certificates_new (
                        certificateId, rollNo, studentName, fatherName, courseName,
                        sessionRange, duration, grade, placeOfIssue, dateOfIssue, certType, timestamp, isSynced
                    )
                    SELECT 
                        CASE 
                            WHEN rollNo LIKE 'LGES-%' THEN rollNo 
                            ELSE 'LGES-' || rollNo 
                        END,
                        rollNo, studentName, fatherName, courseName,
                        sessionRange, duration, grade, placeOfIssue, dateOfIssue, certType, timestamp, 0
                    FROM certificates
                """.trimIndent())

                db.execSQL("DROP TABLE certificates")
                db.execSQL("ALTER TABLE certificates_new RENAME TO certificates")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_certificates_rollNo ON certificates(rollNo)")
            }
        }

        fun getDatabase(context: Context): CertificateDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CertificateDatabase::class.java,
                    "certificate_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
