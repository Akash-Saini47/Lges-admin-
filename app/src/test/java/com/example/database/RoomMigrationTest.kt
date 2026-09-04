package com.example.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun testMigration1To2WithAuthenticV1Schema() {
        val dbFile = File(context.cacheDir, "test_migration_1_2.db")
        if (dbFile.exists()) dbFile.delete()

        val helperConfig = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbFile.name)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Exact v1 schema: rollNo is primary key, no certificateId, no isSynced
                    db.execSQL(
                        """
                        CREATE TABLE certificates (
                            rollNo TEXT NOT NULL PRIMARY KEY,
                            studentName TEXT NOT NULL,
                            fatherName TEXT NOT NULL,
                            courseName TEXT NOT NULL,
                            sessionRange TEXT NOT NULL,
                            duration TEXT NOT NULL,
                            grade TEXT NOT NULL,
                            placeOfIssue TEXT NOT NULL,
                            dateOfIssue TEXT NOT NULL,
                            certType TEXT NOT NULL,
                            timestamp INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val openHelper = FrameworkSQLiteOpenHelperFactory().create(helperConfig)
        val v1Db = openHelper.writableDatabase

        // Insert legacy record in v1
        v1Db.execSQL(
            """
            INSERT INTO certificates (
                rollNo, studentName, fatherName, courseName, sessionRange,
                duration, grade, placeOfIssue, dateOfIssue, certType, timestamp
            ) VALUES (
                '501', 'Amit Sharma', 'S/O Raj Sharma', 'ADCA', '2023-2024',
                '1 Year', 'A', 'CHAMBA', '10-05-2024', 'Course', 1715330000000
            )
            """.trimIndent()
        )

        // Execute MIGRATION_1_2
        CertificateDatabase.MIGRATION_1_2.migrate(v1Db)

        // Verify migrated table
        val cursor = v1Db.query("SELECT certificateId, rollNo, studentName, isSynced FROM certificates WHERE rollNo = '501'")
        assertTrue("Migrated row must exist", cursor.moveToFirst())

        val certId = cursor.getString(cursor.getColumnIndexOrThrow("certificateId"))
        val rollNo = cursor.getString(cursor.getColumnIndexOrThrow("rollNo"))
        val studentName = cursor.getString(cursor.getColumnIndexOrThrow("studentName"))
        val isSynced = cursor.getInt(cursor.getColumnIndexOrThrow("isSynced"))

        assertEquals("LGES-501", certId)
        assertEquals("501", rollNo)
        assertEquals("Amit Sharma", studentName)
        assertEquals(0, isSynced)

        cursor.close()
        v1Db.close()
        dbFile.delete()
    }

    @Test
    fun testMigration3To4NonDestructiveSyncModel() {
        val dbFile = File(context.cacheDir, "test_migration_3_4.db")
        if (dbFile.exists()) dbFile.delete()

        val helperConfig = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbFile.name)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Exact v3 schema
                    db.execSQL(
                        """
                        CREATE TABLE certificates (
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
                    db.execSQL("CREATE INDEX index_certificates_rollNo ON certificates(rollNo)")
                    db.execSQL("CREATE INDEX index_certificates_timestamp ON certificates(timestamp)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val openHelper = FrameworkSQLiteOpenHelperFactory().create(helperConfig)
        val v3Db = openHelper.writableDatabase

        // Insert one synced and one unsynced certificate
        v3Db.execSQL(
            """
            INSERT INTO certificates (
                certificateId, rollNo, studentName, fatherName, courseName, sessionRange,
                duration, grade, placeOfIssue, dateOfIssue, certType, timestamp, isSynced
            ) VALUES (
                'LGES-2024-AAA1', '101', 'Student One', 'Parent One', 'DCA', '2024-2025',
                '1 Year', 'A', 'CHAMBA', '01-01-2024', 'Course', 1704067200000, 1
            )
            """.trimIndent()
        )

        v3Db.execSQL(
            """
            INSERT INTO certificates (
                certificateId, rollNo, studentName, fatherName, courseName, sessionRange,
                duration, grade, placeOfIssue, dateOfIssue, certType, timestamp, isSynced
            ) VALUES (
                'LGES-2024-BBB2', '102', 'Student Two', 'Parent Two', 'Python', '2024-2025',
                '6 Months', 'S', 'CHAMBA', '01-02-2024', 'Internship', 1706745600000, 0
            )
            """.trimIndent()
        )

        // Execute MIGRATION_3_4
        CertificateDatabase.MIGRATION_3_4.migrate(v3Db)

        // Verify synced certificate migrated to SYNCED status
        val cursorSynced = v3Db.query("SELECT syncStatus, retryCount FROM certificates WHERE certificateId = 'LGES-2024-AAA1'")
        assertTrue(cursorSynced.moveToFirst())
        assertEquals("SYNCED", cursorSynced.getString(cursorSynced.getColumnIndexOrThrow("syncStatus")))
        assertEquals(0, cursorSynced.getInt(cursorSynced.getColumnIndexOrThrow("retryCount")))
        cursorSynced.close()

        // Verify unsynced certificate migrated to PENDING status
        val cursorPending = v3Db.query("SELECT syncStatus FROM certificates WHERE certificateId = 'LGES-2024-BBB2'")
        assertTrue(cursorPending.moveToFirst())
        assertEquals("PENDING", cursorPending.getString(cursorPending.getColumnIndexOrThrow("syncStatus")))
        cursorPending.close()

        v3Db.close()
        dbFile.delete()
    }
}
