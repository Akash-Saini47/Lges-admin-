# LGES Admin ProGuard / R8 Optimization Rules

# ============================================================
# ROOM DATABASE
# ============================================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# ============================================================
# DOMAIN & DATABASE MODELS
# ============================================================
-keep class com.example.database.Certificate { *; }
-keep class com.example.database.SyncStatus { *; }
-keep class com.example.database.SyncResult** { *; }
-keep class com.example.util.CertificateValidator** { *; }
-keep class com.example.util.ValidationResult** { *; }
-keep class com.example.util.FormValidationErrors** { *; }

# ============================================================
# WORKMANAGER
# ============================================================
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class com.example.sync.SyncCertificateWorker { *; }

# ============================================================
# MOSHI & JSON SERIALIZATION
# ============================================================
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <fields>;
}
-keep class com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**

# ============================================================
# RETROFIT & OKHTTP
# ============================================================
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# ============================================================
# ZXING QR CODE
# ============================================================
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# ============================================================
# COROUTINES
# ============================================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Preserve line numbers for release stack traces
-keepattributes SourceFile,LineNumberTable
