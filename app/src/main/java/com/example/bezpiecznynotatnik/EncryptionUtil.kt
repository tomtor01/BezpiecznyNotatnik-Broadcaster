package com.example.bezpiecznynotatnik

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import androidx.biometric.BiometricPrompt


object PasswordEncryptionUtil {
    private const val KEYSTORE_ALIAS = "SecureNotesKeyAlias"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        val existingKey = keyStore.getEntry(KEYSTORE_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existingKey != null) {
            return existingKey.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return keyGenerator.generateKey()
    }

    fun encryptHash(hash: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        return Pair(cipher.iv, cipher.doFinal(hash))
    }

    fun decryptHash(iv: ByteArray, encryptedHash: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(encryptedHash)
    }
}

object BiometricsUtil {
    private const val TRANSFORMATION = "AES/CBC/PKCS7Padding"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_NAME = "biometric_key"

    private fun generateSecretKey() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(KEY_NAME)) {
            val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            keyGen.init(
                KeyGenParameterSpec.Builder(
                    KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .setUserAuthenticationRequired(true) 
                    .setInvalidatedByBiometricEnrollment(true) // Klucz pozostaje nieważny po zmianie odcisku
                    .build()
            )
            keyGen.generateKey()
        }
    }

    private fun getCipher(): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val secretKey = keyStore.getKey(KEY_NAME, null) as SecretKey
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher
    }

    fun getCryptoObject(): BiometricPrompt.CryptoObject? {
        return try {
            generateSecretKey() // Tworzenie klucza, jeśli nie istnieje
            val cipher = getCipher()
            BiometricPrompt.CryptoObject(cipher)
        } catch (e: Exception) {
            Log.e("BiometricsUtil", "Error initializing CryptoObject: ${e.message}")
            null
        }
    }
}

object MessageEncryptionUtil {
    private const val KEY_ALIAS = "MessageEncryptionKey"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/CBC/PKCS7Padding"


    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)   // ładowanie klucza symetrycznego
        keyStore.load(null)

        // sprawdzanie czy istnieje juz klucz
        keyStore.getKey(KEY_ALIAS, null)?.let {
            return it as SecretKey
        }

        // Generowanie nowego klucza jesli jeszcze nie istnieje
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .build()
        )
        return keyGenerator.generateKey()
    }

    fun encryptMessage(message: String): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val encryptedMessage = cipher.doFinal(message.toByteArray())
        return Pair(encryptedMessage, iv)
    }

    fun decryptMessage(encryptedMessage: ByteArray, iv: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), IvParameterSpec(iv))
        return String(cipher.doFinal(encryptedMessage))
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
        val digest = MessageDigest.getInstance("SHA-256")
        val saltedPassword = salt + password.toByteArray()
        return digest.digest(saltedPassword)
    }
}

object ByteArrayUtil {
    fun toBase64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
    fun fromBase64(base64String: String): ByteArray = Base64.decode(base64String, Base64.NO_WRAP)
}
