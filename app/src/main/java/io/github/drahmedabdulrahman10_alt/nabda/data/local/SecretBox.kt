package io.github.drahmedabdulrahman10_alt.nabda.data.local

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts small secrets (the AI API keys) with an AES key that lives in the Android Keystore and
 * never leaves the device. Values written before encryption existed are read back as they are.
 * If the key is gone (for example after restoring a backup on another phone) the secret reads as
 * empty and the user simply enters it again.
 */
object SecretBox {

    private const val KEY_ALIAS = "nabda_secrets"
    private const val PREFIX = "enc1:"
    private const val TAG_BITS = 128

    fun encrypt(plain: String): String {
        if (plain.isEmpty()) return ""
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key())
            val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
            val body = Base64.encodeToString(cipher.doFinal(plain.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
            "$PREFIX$iv:$body"
        } catch (e: Exception) {
            // Keystore unavailable: keep the app working rather than losing the key.
            plain
        }
    }

    fun isEncrypted(stored: String): Boolean = stored.startsWith(PREFIX)

    fun decrypt(stored: String): String {
        if (!stored.startsWith(PREFIX)) return stored
        return try {
            val (iv, body) = stored.removePrefix(PREFIX).split(':', limit = 2)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, Base64.decode(iv, Base64.NO_WRAP)))
            String(cipher.doFinal(Base64.decode(body, Base64.NO_WRAP)), Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }
}
