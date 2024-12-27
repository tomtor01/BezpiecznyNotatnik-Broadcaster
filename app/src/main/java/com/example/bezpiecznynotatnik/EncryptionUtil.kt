package com.example.bezpiecznynotatnik

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.Charset
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec


object EncryptionUtil {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_SIZE = 256
    private const val KEY_NAME = "APP_KEY2"
    private const val ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
    private const val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
    private const val ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES

    // Get a Cipher instance for AES/GCM/NoPadding
    private fun getCipher(): Cipher {
        val transformation = "$ENCRYPTION_ALGORITHM/$ENCRYPTION_BLOCK_MODE/$ENCRYPTION_PADDING"
        return Cipher.getInstance(transformation)
    }

    // Get or create a secret key in the Android Keystore
    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        keyStore.getKey(KEY_NAME, null)?.let { return it as SecretKey }

        val keyGenParams = KeyGenParameterSpec.Builder(
            KEY_NAME,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(ENCRYPTION_BLOCK_MODE)
            .setEncryptionPaddings(ENCRYPTION_PADDING)
            .setKeySize(KEY_SIZE)
            .setRandomizedEncryptionRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
            .build()

        return KeyGenerator.getInstance(ENCRYPTION_ALGORITHM, ANDROID_KEYSTORE).apply {
            init(keyGenParams)
        }.generateKey()
    }

    // Initialize a Cipher for encryption
    fun getInitializedCipherForEncryption(): Cipher {
        val cipher = getCipher()
        val secretKey = getOrCreateSecretKey()
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher
    }

    // Initialize a Cipher for decryption
    private fun getInitializedCipherForDecryption(initializationVector: ByteArray): Cipher {
        val cipher = getCipher()
        val secretKey = getOrCreateSecretKey()
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, initializationVector))
        return cipher
    }

    // Encrypt a message and return the encrypted data and IV
    fun encryptMessage(message: String): Pair<ByteArray, ByteArray> {
        val cipher = getInitializedCipherForEncryption()
        val encryptedMessage = cipher.doFinal(message.toByteArray(Charset.forName("UTF-8")))
        return Pair(encryptedMessage, cipher.iv)
    }

    // Decrypt a message using the provided encrypted data and IV
    fun decryptMessage(encryptedMessage: ByteArray, iv: ByteArray): String {
        val cipher = getInitializedCipherForDecryption(iv)
        val plaintext = cipher.doFinal(encryptedMessage)
        return String(plaintext, Charset.forName("UTF-8"))
    }

    fun encryptHash(hash: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = getInitializedCipherForEncryption()
        return Pair(cipher.iv, cipher.doFinal(hash))
    }

    fun decryptHash(iv: ByteArray, encryptedHash: ByteArray): ByteArray {
        val cipher = getInitializedCipherForDecryption(iv)
        return cipher.doFinal(encryptedHash)
    }
}

object SaltUtil {
    fun generateSalt(): ByteArray {
        val salt = ByteArray(16) // 16 bytes = 128-bit salt
        SecureRandom().nextBytes(salt)
        return salt
    }
}
object HashUtil {
    fun hashPassword(password: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-512")
        val saltedPassword = salt + password.toByteArray()
        return digest.digest(saltedPassword)
    }
}

object ByteArrayUtil {
    fun toBase64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
    fun fromBase64(base64String: String): ByteArray = Base64.decode(base64String, Base64.NO_WRAP)
}
