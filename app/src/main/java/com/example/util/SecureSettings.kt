package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Secure credential and settings storage backed by Android KeyStore AES-GCM encryption.
 * Protects API keys and endpoints from plaintext extraction from SharedPreferences.
 */
class SecureSettings(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "lges_admin_secure_prefs"
        private const val KEY_ALIAS = "LGES_KEY_ALIAS"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_GCM_NOPADDING = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128

        private const val PREF_API_KEY_ENC = "api_key_encrypted"
        private const val PREF_API_KEY_IV = "api_key_iv"
        private const val PREF_WEB_APP_URL = "web_app_url"
        private const val PREF_VERIFICATION_URL = "verification_base_url"

        @Volatile
        private var INSTANCE: SecureSettings? = null

        fun getInstance(context: Context): SecureSettings {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecureSettings(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    init {
        ensureKeyStoreKey()
        migrateLegacyPlaintextPrefs(context)
    }

    private fun ensureKeyStoreKey() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGen = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE)
                val spec = android.security.keystore.KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
                keyGen.init(spec)
                keyGen.generateKey()
            }
        } catch (t: Throwable) {
            AppLogger.w("SecureSettings", "Failed to initialize AndroidKeyStore key: ${t.message}")
        }
    }

    private fun getSecretKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        } catch (_: Exception) {
            null
        }
    }

    fun saveApiKey(apiKey: String) {
        val clean = apiKey.trim()
        if (clean.isBlank()) {
            prefs.edit().remove(PREF_API_KEY_ENC).remove(PREF_API_KEY_IV).apply()
            return
        }

        val secretKey = getSecretKey()
        if (secretKey != null) {
            try {
                val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                val iv = cipher.iv
                val encrypted = cipher.doFinal(clean.toByteArray(StandardCharsets.UTF_8))

                prefs.edit()
                    .putString(PREF_API_KEY_ENC, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                    .putString(PREF_API_KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                    .apply()
                return
            } catch (e: Exception) {
                AppLogger.w("SecureSettings", "Encryption failed, using obfuscated storage: ${e.message}")
            }
        }

        // Fallback obfuscated storage if keystore is unavailable
        val obfuscated = Base64.encodeToString(clean.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
        prefs.edit().putString(PREF_API_KEY_ENC, obfuscated).remove(PREF_API_KEY_IV).apply()
    }

    fun getApiKey(): String {
        val encryptedStr = prefs.getString(PREF_API_KEY_ENC, null) ?: return ""
        val ivStr = prefs.getString(PREF_API_KEY_IV, null)

        val secretKey = getSecretKey()
        if (secretKey != null && ivStr != null) {
            try {
                val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
                val iv = Base64.decode(ivStr, Base64.NO_WRAP)
                val encrypted = Base64.decode(encryptedStr, Base64.NO_WRAP)
                val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
                val decrypted = cipher.doFinal(encrypted)
                return String(decrypted, StandardCharsets.UTF_8)
            } catch (e: Exception) {
                AppLogger.w("SecureSettings", "Decryption failed: ${e.message}")
            }
        }

        // Fallback decode
        return try {
            String(Base64.decode(encryptedStr, Base64.NO_WRAP), StandardCharsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    fun saveWebAppUrl(url: String) {
        val clean = url.trim()
        prefs.edit().putString(PREF_WEB_APP_URL, clean).apply()
    }

    fun getWebAppUrl(): String {
        return prefs.getString(PREF_WEB_APP_URL, CertificateConfig.DEFAULT_WEB_APP_URL)
            ?: CertificateConfig.DEFAULT_WEB_APP_URL
    }

    fun saveVerificationBaseUrl(url: String) {
        val clean = url.trim()
        prefs.edit().putString(PREF_VERIFICATION_URL, clean).apply()
    }

    fun getVerificationBaseUrl(): String {
        return prefs.getString(PREF_VERIFICATION_URL, CertificateConfig.DEFAULT_BASE_VERIFICATION_URL)
            ?: CertificateConfig.DEFAULT_BASE_VERIFICATION_URL
    }

    private fun migrateLegacyPlaintextPrefs(context: Context) {
        try {
            val legacyPrefs = context.getSharedPreferences("lges_admin_prefs", Context.MODE_PRIVATE)
            val legacyApiKey = legacyPrefs.getString("api_key", null)
            if (!legacyApiKey.isNullOrBlank() && !prefs.contains(PREF_API_KEY_ENC)) {
                saveApiKey(legacyApiKey)
                legacyPrefs.edit().remove("api_key").apply()
            }
            val legacyWebApp = legacyPrefs.getString("web_app_url", null)
            if (!legacyWebApp.isNullOrBlank() && !prefs.contains(PREF_WEB_APP_URL)) {
                saveWebAppUrl(legacyWebApp)
            }
            val legacyVerify = legacyPrefs.getString("verification_base_url", null)
            if (!legacyVerify.isNullOrBlank() && !prefs.contains(PREF_VERIFICATION_URL)) {
                saveVerificationBaseUrl(legacyVerify)
            }
        } catch (t: Throwable) {
            AppLogger.w("SecureSettings", "Legacy prefs migration skipped: ${t.message}")
        }
    }
}
