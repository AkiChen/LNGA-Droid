package com.aki.lnga.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.aki.lnga.model.NgaSession
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class CredentialStore(context: Context) {
    private val preferences = context.getSharedPreferences("nga_credentials", Context.MODE_PRIVATE)

    fun save(session: NgaSession) {
        val plainText = "${session.uid}\n${session.cid}".toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val payload = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(cipher.doFinal(plainText), Base64.NO_WRAP)
        preferences.edit().putString(KEY_PAYLOAD, payload).apply()
    }

    fun load(): NgaSession? = runCatching {
        val payload = preferences.getString(KEY_PAYLOAD, null) ?: return null
        val parts = payload.split(':', limit = 2)
        if (parts.size != 2) return null
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)),
        )
        val values = cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP))
            .toString(Charsets.UTF_8)
            .split('\n', limit = 2)
        if (values.size == 2 && values.all(String::isNotBlank)) NgaSession(values[0], values[1]) else null
    }.getOrNull()

    fun clear() {
        preferences.edit().clear().apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "lnga_android_session_v1"
        const val KEY_PAYLOAD = "encrypted_session"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
