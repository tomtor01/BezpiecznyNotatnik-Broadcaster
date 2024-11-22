package com.example.bezpiecznynotatnik

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.util.Log

class MainActivity : AppCompatActivity() {
    private lateinit var passwordInput: EditText
    private lateinit var submitButton: Button
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        passwordInput = findViewById(R.id.passwordInput)
        submitButton = findViewById(R.id.submitButton)
        sharedPrefs = getSharedPreferences("SecureNotesPrefs", MODE_PRIVATE)

        // sprawdzanie czy hasło juz istnieje
        val encryptedHashBase64 = sharedPrefs.getString("passwordHash", null)
        val ivBase64 = sharedPrefs.getString("iv", null)
        val saltBase64 = sharedPrefs.getString("password_salt", null)

        if (encryptedHashBase64 == null || ivBase64 == null || saltBase64 == null) {
            // redirect to PasswordSetupActivity
            Log.d("SecureNotes", "No password found. Redirecting to setup.")
            val intent = Intent(this, PasswordSetupActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        Log.d("SecureNotes", "Encrypted Hash: $encryptedHashBase64")
        Log.d("SecureNotes", "IV: $ivBase64")
        Log.d("SecureNotes", "Salt: $saltBase64")

        submitButton.setOnClickListener {
            val enteredPassword = passwordInput.text.toString()

            if (enteredPassword.isEmpty()) {
                Toast.makeText(this, "Please enter a password.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val attemptCounter = sharedPrefs.getInt("attemptCounter", 0)
                if (attemptCounter >= 10) {
                    resetPassword()
                    return@setOnClickListener
                }

                val salt = ByteArrayUtil.fromBase64(saltBase64)
                val hashedPassword = HashUtil.hashPassword(enteredPassword, salt)

                // dekodowanie
                val storedEncryptedHash = ByteArrayUtil.fromBase64(encryptedHashBase64)
                val storedIv = ByteArrayUtil.fromBase64(ivBase64)

                // Deszyfrowanie hasha i porównanie z nowo wprowadzonym hashem hasła
                val decryptedHash = EncryptionUtil.decryptHash(storedIv, storedEncryptedHash)
                if (hashedPassword.contentEquals(decryptedHash)) {
                    Log.d("SecureNotes", "Password matched. Redirecting to second screen.")
                    sharedPrefs.edit().putInt("attemptCounter", 0)
                        .apply() // Reset the counter on success
                    val intent = Intent(this, AccessActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    handleFailedAttempt(attemptCounter)
                }
            } catch (e: Exception) {
                Log.e("SecureNotes", "Error during password validation: ${e.message}")
                Toast.makeText(
                    this,
                    "An error occurred. Please reset your password.",
                    Toast.LENGTH_LONG
                ).show()
                val intent = Intent(this, PasswordSetupActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun handleFailedAttempt(currentAttempt: Int) {
        val newAttemptCount = currentAttempt + 1
        sharedPrefs.edit().putInt("attemptCounter", newAttemptCount).apply()

        if (newAttemptCount >= 10) {
            resetPassword()
        } else {
            Toast.makeText(
                this,
                "Incorrect password! Attempt $newAttemptCount/10.",
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
            .setTitle("Password Reset")
            .setMessage("Too many failed attempts. The password has been reset, and saved data has been cleared. Please set a new password.")
            .setPositiveButton("OK") { _, _ ->
                val intent = Intent(this, PasswordSetupActivity::class.java)
                startActivity(intent)
                finish()
            }
            .setCancelable(false)
            .show()
    }
}

