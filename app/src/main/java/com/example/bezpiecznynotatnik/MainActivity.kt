package com.example.bezpiecznynotatnik

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import javax.crypto.Cipher


class MainActivity : AppCompatActivity() {
    private lateinit var passwordInput: EditText
    private lateinit var loginWithPasswordButton: Button
    private lateinit var loginWithBiometricsButton: Button
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        passwordInput = findViewById(R.id.passwordInput)
        loginWithPasswordButton = findViewById(R.id.loginWithPasswordButton)
        loginWithBiometricsButton = findViewById(R.id.loginWithBiometricsButton)
        sharedPrefs = getSharedPreferences("SecureNotesPrefs", MODE_PRIVATE)

        val saltBase64 = sharedPrefs.getString("password_salt", null)
        val encryptedHashBase64 = sharedPrefs.getString("passwordHash", null)
        val ivBase64 = sharedPrefs.getString("iv", null)

        if (saltBase64 == null || encryptedHashBase64 == null || ivBase64 == null) {
            Toast.makeText(this, "Utwórz nowe hasło", Toast.LENGTH_SHORT).show()
            redirectToPasswordSetup()
            return
        }


        loginWithPasswordButton.setOnClickListener {
            authenticateWithPassword()
        }

        loginWithBiometricsButton.setOnClickListener {
            authenticateWithBiometrics()
        }
    }

    private fun redirectToPasswordSetup() {
        val intent = Intent(this, PasswordSetupActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun authenticateWithPassword() {
        val enteredPassword = passwordInput.text.toString()

        if (enteredPassword.isEmpty()) {
            Toast.makeText(this, "Hasło nie może być puste.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val attemptCounter = sharedPrefs.getInt("attemptCounter", 0)
            if (attemptCounter >= 10) {
                resetPassword()
                return
            }
            val saltBase64 = sharedPrefs.getString("password_salt", null)
            val encryptedHashBase64 = sharedPrefs.getString("passwordHash", null)
            val ivBase64 = sharedPrefs.getString("iv", null)

            val salt = ByteArrayUtil.fromBase64(saltBase64.toString())
            val hashedPassword = HashUtil.hashPassword(enteredPassword, salt)

            val storedEncryptedHash = ByteArrayUtil.fromBase64(encryptedHashBase64.toString())
            val storedIv = ByteArrayUtil.fromBase64(ivBase64.toString())

            val decryptedHash = EncryptionUtil.decryptHash(storedIv, storedEncryptedHash)
            if (hashedPassword.contentEquals(decryptedHash)) {
                sharedPrefs.edit().putInt("attemptCounter", 0)
                    .apply() // Reset the counter on success
                navigateToAccessActivity()
            } else {
                handleFailedAttempt(attemptCounter)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error authenticating with password: ${e.message}", e)
            Toast.makeText(this, "Authentication error.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun authenticateWithBiometrics() {
        // sprawdzanie dostępności biometrii
        val biometricManager = BiometricManager.from(this)
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, "Biometric authentication is not available", Toast.LENGTH_LONG).show()
            finish()
        }
        val executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                Toast.makeText(this@MainActivity, "Uwierzytelnianie zakończone sukcesem!", Toast.LENGTH_SHORT).show()
                navigateToAccessActivity()
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

        val cipher = EncryptionUtil.getInitializedCipherForEncryption()
        val cryptoObject = BiometricPrompt.CryptoObject(cipher)
        biometricPrompt.authenticate(promptInfo, cryptoObject)
    }

    private fun navigateToAccessActivity() {
        val intent = Intent(this, AccessActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun handleFailedAttempt(currentAttempt: Int) {
        val newAttemptCount = currentAttempt + 1
        sharedPrefs.edit().putInt("attemptCounter", newAttemptCount).apply()
        val attempt = 10 - newAttemptCount
        if (newAttemptCount >= 10) {
            resetPassword()
        } else {
            Toast.makeText(
                this,
                "Błędne hasło! Próby: $attempt.",
                Toast.LENGTH_SHORT
            ).show()
            passwordInput.text.clear()
        }
    }

    private fun resetPassword() {
        sharedPrefs.edit()
            .remove("passwordHash")
            .remove("iv")
            .remove("password_salt")
            .remove("encryptedMessage")
            .remove("messageIv")
            .putInt("attemptCounter", 0)
            .apply()

        // Pop-up
        AlertDialog.Builder(this)
            .setTitle("Reset hasła")
            .setMessage("Hasło oraz notatka zostały skasowane.")
            .setPositiveButton("OK") { _, _ ->
                val intent = Intent(this, PasswordSetupActivity::class.java)
                startActivity(intent)
                finish()
            }
            .setCancelable(false)
            .show()
    }
    // troubleshooting
    private fun resetEncryptedData() {
        sharedPrefs.edit()
            .clear()
            .apply()
        Toast.makeText(this, "Data reset due to key change.", Toast.LENGTH_SHORT).show()
    }
}

