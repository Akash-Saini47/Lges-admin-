package com.example.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Certificate::class], version = 1, exportSchema = false)
abstract class CertificateDatabase : RoomDatabase() {
    abstract fun certificateDao(): CertificateDao

    companion object {
        @Volatile
        private var INSTANCE: CertificateDatabase? = null

        fun getDatabase(context: Context): CertificateDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CertificateDatabase::class.java,
                    "certificate_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
