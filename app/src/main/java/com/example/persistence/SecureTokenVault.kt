package com.example.persistence

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.core.model.ProviderId
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android Keystore-backed AES-256-GCM token vault for optional OAuth tokens.
 * SoundShelf never asks for or stores third-party account passwords.
 */
class SecureTokenVault(context: Context) {
    private val prefs = context.getSharedPreferences("soundshelf_secure_tokens", Context.MODE_PRIVATE)
    private val keyAlias = "soundshelf_token_aes_key"

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        keyStore.getKey(keyAlias, null)?.let { return it as SecretKey }

        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGen.init(spec)
        return keyGen.generateKey()
    }

    fun storeToken(providerId: ProviderId, accessToken: String, expiresAtMs: Long? = null) {
        runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            val iv = cipher.iv
            val encrypted = cipher.doFinal(accessToken.toByteArray(Charsets.UTF_8))
            prefs.edit()
                .putString("tok_${providerId.name}", Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .putString("iv_${providerId.name}", Base64.encodeToString(iv, Base64.NO_WRAP))
                .putLong("exp_${providerId.name}", expiresAtMs ?: -1L)
                .apply()
        }
    }

    fun readToken(providerId: ProviderId): String? {
        val encStr = prefs.getString("tok_${providerId.name}", null) ?: return null
        val ivStr = prefs.getString("iv_${providerId.name}", null) ?: return null
        val expiresAt = prefs.getLong("exp_${providerId.name}", -1L)
        if (expiresAt > 0 && System.currentTimeMillis() > expiresAt) {
            clearToken(providerId)
            return null
        }
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, Base64.decode(ivStr, Base64.NO_WRAP))
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)
            val decrypted = cipher.doFinal(Base64.decode(encStr, Base64.NO_WRAP))
            String(decrypted, Charsets.UTF_8)
        }.getOrNull()
    }

    fun hasActiveToken(providerId: ProviderId): Boolean = readToken(providerId) != null

    fun clearToken(providerId: ProviderId) {
        prefs.edit()
            .remove("tok_${providerId.name}")
            .remove("iv_${providerId.name}")
            .remove("exp_${providerId.name}")
            .apply()
    }
}
