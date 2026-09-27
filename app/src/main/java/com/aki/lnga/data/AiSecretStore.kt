package com.aki.lnga.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Stores only encrypted AI API keys. Configuration metadata lives in [AiConfigStore]. */
internal class AiSecretStore(context: Context) {
    private val preferences = context.getSharedPreferences("ai_secrets", Context.MODE_PRIVATE)

    fun save(configId: String, apiKey: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val payload = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(cipher.doFinal(apiKey.trim().toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
        preferences.edit().putString(key(configId), payload).apply()
    }

    fun load(configId: String): String? = runCatching {
        val parts = preferences.getString(key(configId), null)?.split(':', limit = 2) ?: return null
        if (parts.size != 2) return null
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)),
        )
        cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)).toString(Charsets.UTF_8).takeIf(String::isNotBlank)
    }.getOrNull()

    fun has(configId: String): Boolean = load(configId) != null

    fun delete(configId: String) {
        preferences.edit().remove(key(configId)).apply()
    }

    private fun key(configId: String) = "secret_$configId"

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
        }.generateKey()
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "lnga_android_ai_keys_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
