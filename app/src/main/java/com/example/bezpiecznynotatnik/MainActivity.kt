package com.example.bezpiecznynotatnik

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec


class MainActivity : AppCompatActivity() {
    private lateinit var passwordInput: EditText
    private lateinit var submitButton: Button
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views and SharedPreferences
        passwordInput = findViewById(R.id.passwordInput)
        submitButton = findViewById(R.id.submitButton)
        sharedPrefs = getSharedPreferences("SecureNotesPrefs", MODE_PRIVATE)

        // Check if a password exists
        val savedPassword = sharedPrefs.getString("password", null)

        if (savedPassword == null) {
            // Redirect to password setup if no password is set
            Toast.makeText(this, "No password found. Please set one first.", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, PasswordSetupActivity::class.java)
            startActivity(intent)
            finish() // Close this activity so the user can't return without setting a password
        }

        // Login Button Listener
        submitButton.setOnClickListener {
            val enteredPassword = passwordInput.text.toString()
            if (enteredPassword == savedPassword) {
                // Navigate to SecondActivity if the password is correct
                val intent = Intent(this, SecondActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Incorrect Password!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

class SecondActivity : AppCompatActivity() {
    private lateinit var newPasswordInput: EditText
    private lateinit var changePasswordButton: Button
    private lateinit var messageInput: EditText
    private lateinit var saveMessageButton: Button
    private lateinit var messageView: TextView
    private lateinit var logoutButton: Button
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_access)

        // Initialize views and SharedPreferences
        newPasswordInput = findViewById(R.id.newPasswordInput)
        messageView = findViewById(R.id.messageView)
        changePasswordButton = findViewById(R.id.changePasswordButton)
        messageInput = findViewById(R.id.messageInput)
        saveMessageButton = findViewById(R.id.saveMessageButton)
        logoutButton = findViewById(R.id.logoutButton)
        sharedPrefs = getSharedPreferences("SecureNotesPrefs", MODE_PRIVATE)

        showMessage()

        // Change Password Button Listener
        changePasswordButton.setOnClickListener {
            val newPassword = newPasswordInput.text.toString()
            if (newPassword.isNotEmpty()) {
                sharedPrefs.edit().putString("password", newPassword).apply()
                Toast.makeText(this, "Password changed successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Password cannot be empty!", Toast.LENGTH_SHORT).show()
            }
        }

        // Save Message Button Listener
        saveMessageButton.setOnClickListener {
            val message = messageInput.text.toString()
            sharedPrefs.edit().putString("message", message).apply()
            Toast.makeText(this, "Message saved successfully!", Toast.LENGTH_SHORT).show()
            showMessage()
        }


        // Logout Button Listener
        logoutButton.setOnClickListener {
            // Clear sensitive session data if needed (optional)
            // Navigate back to MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Close this activity
        }
    }

    private fun showMessage() {
        val message = sharedPrefs.getString("message", "No message saved.")
        messageView.text = message
    }
}


class PasswordSetupActivity : AppCompatActivity() {
    private lateinit var newPasswordInput: EditText
    private lateinit var setPasswordButton: Button
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_setup)

        // Initialize views and SharedPreferences
        newPasswordInput = findViewById(R.id.newPasswordInput)
        setPasswordButton = findViewById(R.id.setPasswordButton)
        sharedPrefs = getSharedPreferences("SecureNotesPrefs", MODE_PRIVATE)

        setPasswordButton.setOnClickListener {
            val newPassword = newPasswordInput.text.toString()
            if (newPassword.isNotEmpty()) {
                // Save the new password
                sharedPrefs.edit().putString("password", newPassword).apply()
                Toast.makeText(this, "Password set successfully!", Toast.LENGTH_SHORT).show()

                // Redirect to MainActivity
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Password cannot be empty!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

