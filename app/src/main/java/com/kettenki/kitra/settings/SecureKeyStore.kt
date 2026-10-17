package com.kettenki.kitra.settings

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Guarda la clave de Gemini cifrada con AES-GCM usando una clave no exportable de Android Keystore.
 * En disco solo queda el texto cifrado. La clave nunca se escribe en logs.
 */
class SecureKeyStore(private val prefs: SharedPreferences) {

    fun save(apiKey: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(apiKey.toByteArray(Charsets.UTF_8))
        prefs.edit {
            putString(PREF_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            putString(PREF_CIPHERTEXT, Base64.encodeToString(encrypted, Base64.NO_WRAP))
        }
    }

    fun load(): String? {
        val iv = prefs.getString(PREF_IV, null) ?: return null
        val data = prefs.getString(PREF_CIPHERTEXT, null) ?: return null
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
            String(cipher.doFinal(Base64.decode(data, Base64.NO_WRAP)), Charsets.UTF_8)
        } catch (_: Exception) {
            // Clave de Keystore invalidada (p. ej. restauración del dispositivo): se pedirá de nuevo.
            null
        }
    }

    fun hasKey(): Boolean = prefs.contains(PREF_CIPHERTEXT)

    fun clear() = prefs.edit { remove(PREF_IV); remove(PREF_CIPHERTEXT) }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "kitra_gemini_api_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val PREF_IV = "api_key_iv"
        const val PREF_CIPHERTEXT = "api_key_ciphertext"
    }
}
