package com.example.bezpiecznynotatnik

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AccessActivity : AppCompatActivity() {
    private lateinit var newPasswordInput: EditText
    private lateinit var changePasswordButton: Button
    private lateinit var messageInput: EditText
    private lateinit var saveMessageButton: Button
    private lateinit var messageDisplay: TextView
    private lateinit var logoutButton: Button
    private lateinit var sharedPrefs: SharedPreferences

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_access)

        newPasswordInput = findViewById(R.id.newPasswordInput)
        changePasswordButton = findViewById(R.id.changePasswordButton)
        messageInput = findViewById(R.id.messageInput)
        saveMessageButton = findViewById(R.id.saveMessageButton)
        messageDisplay = findViewById(R.id.messageDisplay)
        logoutButton = findViewById(R.id.logoutButton)
        sharedPrefs = getSharedPreferences("SecureNotesPrefs", MODE_PRIVATE)

        // Change Password Functionality
        changePasswordButton.setOnClickListener {
            val newPassword = newPasswordInput.text.toString()
            if (newPassword.isNotEmpty()) {
                try {
                    // generowanie salt dla nowego hasła
                    val salt = SaltUtil.generateSalt()
                    val passwordHash = HashUtil.hashPassword(newPassword, salt)
                    val (iv, encryptedHash) = EncryptionUtil.encryptHash(passwordHash)

                    sharedPrefs.edit()
                        .putString("passwordHash", ByteArrayUtil.toBase64(encryptedHash))
                        .putString("iv", ByteArrayUtil.toBase64(iv))
                        .putString("password_salt", ByteArrayUtil.toBase64(salt))
                        .apply()

                    newPasswordInput.text.clear()
                    Toast.makeText(this, "Password changed successfully!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error changing password!", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            } else {
                Toast.makeText(this, "Password cannot be empty!", Toast.LENGTH_SHORT).show()
            }
        }

        // Ładowanie i deszyfrowanie wiadomości
        val encryptedMessageBase64 = sharedPrefs.getString("encryptedMessage", null)
        val ivBase64 = sharedPrefs.getString("messageIv", null)
        if (encryptedMessageBase64 != null && ivBase64 != null) {
            try {
                val encryptedMessage = Base64.decode(encryptedMessageBase64, Base64.DEFAULT)
                val iv = Base64.decode(ivBase64, Base64.DEFAULT)
                val decryptedMessage = MessageEncryptionUtil.decryptMessage(encryptedMessage, iv)
                messageDisplay.text = decryptedMessage
            } catch (e: Exception) {
                messageDisplay.text = "Error decrypting message!"
                e.printStackTrace()
            }
        } else {
            messageDisplay.text = "No message saved yet."
        }

        saveMessageButton.setOnClickListener {
            val newMessage = messageInput.text.toString()
            if (newMessage.isNotEmpty()) {
                try {
                    val (encryptedMessage, iv) = MessageEncryptionUtil.encryptMessage(newMessage)
                    val encryptedMessageBase64 = Base64.encodeToString(encryptedMessage, Base64.DEFAULT)
                    val ivBase64 = Base64.encodeToString(iv, Base64.DEFAULT)

                    sharedPrefs.edit()
                        .putString("encryptedMessage", encryptedMessageBase64)
                        .putString("messageIv", ivBase64)
                        .apply()

                    messageDisplay.text = newMessage
                    messageInput.text.clear()
                    Toast.makeText(this, "Message saved securely!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error saving message!", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            } else {
                Toast.makeText(this, "Message cannot be empty!", Toast.LENGTH_SHORT).show()
            }
        }

        logoutButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}

class PasswordSetupActivity : AppCompatActivity() {
    private lateinit var newPasswordInput: EditText
    private lateinit var setPasswordButton: Button
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_setup)

        newPasswordInput = findViewById(R.id.newPasswordInput)
        setPasswordButton = findViewById(R.id.setPasswordButton)
        sharedPrefs = getSharedPreferences("SecureNotesPrefs", MODE_PRIVATE)

        setPasswordButton.setOnClickListener {
            val newPassword = newPasswordInput.text.toString()

            if (newPassword.isNotEmpty()) {
                try {
                    val salt = SaltUtil.generateSalt()

                    val passwordHash = HashUtil.hashPassword(newPassword, salt)

                    // szyfrowanie hasła
                    val (iv, encryptedHash) = EncryptionUtil.encryptHash(passwordHash)

                    sharedPrefs.edit()
                        .putString("passwordHash", ByteArrayUtil.toBase64(encryptedHash))
                        .putString("iv", ByteArrayUtil.toBase64(iv))
                        .putString("password_salt", ByteArrayUtil.toBase64(salt))
                        .apply()

                    Toast.makeText(this, "Password set successfully!", Toast.LENGTH_SHORT).show()

                    // powrót do MainActivity
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error setting up password!", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            } else {
                Toast.makeText(this, "Password cannot be empty!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}