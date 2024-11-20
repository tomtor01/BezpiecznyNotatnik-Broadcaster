package com.example.bezpiecznynotatnik


import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
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

        val encryptedHashBase64 = sharedPrefs.getString("passwordHash", null)
        val ivBase64 = sharedPrefs.getString("iv", null)

        if (encryptedHashBase64 == null || ivBase64 == null) {
            // No password is set, redirect to PasswordSetupActivity
            Log.d("SecureNotes", "No password found. Redirecting to setup.")
            val intent = Intent(this, PasswordSetupActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        Log.d("SecureNotes", "Encrypted Hash: $encryptedHashBase64")
        Log.d("SecureNotes", "IV: $ivBase64")

        submitButton.setOnClickListener {
            val enteredPassword = passwordInput.text.toString()

            // Validate entered password
            if (enteredPassword.isEmpty()) {
                Toast.makeText(this, "Please enter a password.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val hashedPassword = HashUtil.hashPassword(enteredPassword)

            try {
                // Decode Base64 strings to byte arrays
                val storedEncryptedHash = ByteArrayUtil.fromBase64(encryptedHashBase64)
                val storedIv = ByteArrayUtil.fromBase64(ivBase64)

                // Decrypt stored hash and compare it with entered password hash
                val decryptedHash = EncryptionUtil.decryptHash(storedIv, storedEncryptedHash)
                if (hashedPassword.contentEquals(decryptedHash)) {
                    Log.d("SecureNotes", "Password matched. Redirecting to second screen.")
                    val intent = Intent(this, AccessActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Incorrect password!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("SecureNotes", "Error during password validation: ${e.message}")
                Toast.makeText(this, "An error occurred. Please reset your password.", Toast.LENGTH_LONG).show()
                // Optionally redirect to setup in case of corruption
                val intent = Intent(this, PasswordSetupActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
}



