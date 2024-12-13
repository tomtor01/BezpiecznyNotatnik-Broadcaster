package com.example.bezpiecznynotatnik

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import javax.crypto.Cipher


class MainActivity : AppCompatActivity() {
    private lateinit var submitButton: Button
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        submitButton = findViewById(R.id.submitButton)
        sharedPrefs = getSharedPreferences("SecureNotesPrefs", MODE_PRIVATE)

        // Sprawdzenie dostępności biometrii
        val biometricManager = BiometricManager.from(this)
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, "Biometric authentication is not available", Toast.LENGTH_LONG).show()
            finish()
        }

        submitButton.setOnClickListener {
            authenticateWithBiometrics()
        }
    }

    private fun authenticateWithBiometrics() {
        val executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                Toast.makeText(this@MainActivity, "Uwierzytelnianie zakończone sukcesem!", Toast.LENGTH_SHORT).show()
                navigateToAccessActivity(result.cryptoObject)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(this@MainActivity, "Błąd: $errString", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(this@MainActivity, "Spróbuj ponownie", Toast.LENGTH_SHORT).show()
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Aby przejrzeć notatki, musisz potwierdzić tożsamość")
            .setNegativeButtonText("Zamknij")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        val cipher = BiometricsUtil.getInitializedCipherForEncryption(BiometricsUtil.KEY_NAME)
        val cryptoObject = BiometricPrompt.CryptoObject(cipher)
        biometricPrompt.authenticate(promptInfo, cryptoObject)
    }

    private fun navigateToAccessActivity(cryptoObject: BiometricPrompt.CryptoObject?) {
        val intent = Intent(this, AccessActivity::class.java)
        intent.putExtra(CRYPTO_IV, cryptoObject?.cipher?.iv) // Pass initialization vector
        startActivity(intent)
        finish()
    }

    companion object {
        const val CRYPTO_IV = "crypto_iv"
    }
}

