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


object BiometricsUtil {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_SIZE = 256
    const val KEY_NAME = "app_key"
    private const val ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
    private const val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
    private const val ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES

    // Get a Cipher instance for AES/GCM/NoPadding
    private fun getCipher(): Cipher {
        val transformation = "$ENCRYPTION_ALGORITHM/$ENCRYPTION_BLOCK_MODE/$ENCRYPTION_PADDING"
        return Cipher.getInstance(transformation)
    }

    // Get or create a secret key in the Android Keystore
    private fun getOrCreateSecretKey(keyName: String): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        keyStore.getKey(keyName, null)?.let { return it as SecretKey }

        val keyGenParams = KeyGenParameterSpec.Builder(keyName,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(ENCRYPTION_BLOCK_MODE)
            .setEncryptionPaddings(ENCRYPTION_PADDING)
            .setKeySize(KEY_SIZE)
            .setUserAuthenticationRequired(false)
            .build()

        return KeyGenerator.getInstance(ENCRYPTION_ALGORITHM, ANDROID_KEYSTORE).apply {
            init(keyGenParams)
        }.generateKey()
    }

    // Initialize a Cipher for encryption
    fun getInitializedCipherForEncryption(): Cipher {
        val cipher = getCipher()
        val secretKey = getOrCreateSecretKey(KEY_NAME)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher
    }

    // Initialize a Cipher for decryption
    private fun getInitializedCipherForDecryption(initializationVector: ByteArray): Cipher {
        val cipher = getCipher()
        val secretKey = getOrCreateSecretKey(KEY_NAME)
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
}


//object MessageEncryptionUtil {
//    private const val KEY_ALIAS = "MessageEncryptionKey"
//    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
//    private const val TRANSFORMATION = "AES/CBC/PKCS7Padding"
//
//
//    private fun getOrCreateKey(): SecretKey {
//        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)   // ładowanie klucza symetrycznego
//        keyStore.load(null)
//
//        // sprawdzanie czy istnieje juz klucz
//        keyStore.getKey(KEY_ALIAS, null)?.let {
//            return it as SecretKey
//        }
//
//        // Generowanie nowego klucza jesli jeszcze nie istnieje
//        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
//        keyGenerator.init(
//            KeyGenParameterSpec.Builder(
//                KEY_ALIAS,
//                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
//            )
//                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
//                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
//                .build()
//        )
//        return keyGenerator.generateKey()
//    }
//}

object SaltUtil {
    fun generateSalt(): ByteArray {
        val salt = ByteArray(16) // 16 bytes = 128-bit salt
        SecureRandom().nextBytes(salt)
        return salt
    }
}
object HashUtil {
    fun hashPassword(password: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        val saltedPassword = salt + password.toByteArray()
        return digest.digest(saltedPassword)
    }
}

object ByteArrayUtil {
    fun toBase64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
    fun fromBase64(base64String: String): ByteArray = Base64.decode(base64String, Base64.NO_WRAP)
}