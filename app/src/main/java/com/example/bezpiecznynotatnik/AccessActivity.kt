package com.example.bezpiecznynotatnik

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Base64
import android.util.Log
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
    private lateinit var deleteMessageButton: Button
    private lateinit var messageDisplay: TextView
    private lateinit var logoutButton: Button
    private lateinit var sharedPrefs: SharedPreferences

    @SuppressLint("SetTextI18n", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_access)

        newPasswordInput = findViewById(R.id.newPasswordInput)
        changePasswordButton = findViewById(R.id.changePasswordButton)
        messageInput = findViewById(R.id.messageInput)
        saveMessageButton = findViewById(R.id.saveMessageButton)
        deleteMessageButton = findViewById(R.id.deleteMessageButton)
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
                    Toast.makeText(this, "Pomyślnie zmieniono hasło!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Wystąpił błąd przy zmianie hasła!", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            } else {
                Toast.makeText(this, "Hasło nie może być puste!", Toast.LENGTH_SHORT).show()
            }
        }

        // Ładowanie i deszyfrowanie wiadomości
        loadMessage()

        saveMessageButton.setOnClickListener {
            val newMessage = messageInput.text.toString()
            if (newMessage.isNotEmpty()) {
                saveMessage(newMessage)
            } else {
                Toast.makeText(this, "Nie można dodać pustej notatki.", Toast.LENGTH_SHORT).show()
            }
        }

        deleteMessageButton.setOnClickListener {
            deleteMessage()
        }

        logoutButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadMessage() {
        val encryptedMessageBase64 = sharedPrefs.getString("encryptedMessage", null)
        val ivBase64 = sharedPrefs.getString("messageIv", null)

        if (encryptedMessageBase64 != null && ivBase64 != null) {
            try {
                val encryptedMessage = ByteArrayUtil.fromBase64(encryptedMessageBase64)
                val iv = ByteArrayUtil.fromBase64(ivBase64)
                val decryptedMessage = EncryptionUtil.decryptMessage(encryptedMessage, iv)
                messageDisplay.text = decryptedMessage
            } catch (e: Exception) {
                messageDisplay.text = "Wystąpił błąd przy deszyfrowaniu wiadomości."
                Log.e("AccessActivity", "Error decrypting message: ${e.message}")
            }
        } else {
            messageDisplay.text = "Brak notatek."
        }
    }

    private fun saveMessage(message: String) {
        try {
            val (encryptedMessage, iv) = EncryptionUtil.encryptMessage(message)
            val encryptedMessageBase64 = ByteArrayUtil.toBase64(encryptedMessage)
            val ivBase64 = ByteArrayUtil.toBase64(iv)

            sharedPrefs.edit()
                .putString("encryptedMessage", encryptedMessageBase64)
                .putString("messageIv", ivBase64)
                .apply()

            messageDisplay.text = message
            messageInput.text.clear()
            Toast.makeText(this, "Dodano notatkę.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("AccessActivity", "err: ${e.message}")
            Toast.makeText(this, "Wystąpił błąd przy dodawaniu notatki.", Toast.LENGTH_SHORT).show()
        }


        deleteMessageButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        logoutButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun deleteMessage() {
        val editor = sharedPrefs.edit()

        if (sharedPrefs.contains("encryptedMessage")) {
            editor.remove("encryptedMessage")
                .remove("messageIv")
                .apply()

            messageDisplay.text = "Notatka została usunięta."
            Toast.makeText(this, "Notatka usunięta!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Nie ma zapisanej notatki do usunięcia.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmationDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Usuń notatkę")
            .setMessage("Czy na pewno chcesz usunąć tę notatkę?")
            .setPositiveButton("Tak") { _, _ -> deleteMessage() }
            .setNegativeButton("Nie", null)
            .show()
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
                        .putString("password_salt", ByteArrayUtil.toBase64(salt))
                        .putString("iv", ByteArrayUtil.toBase64(iv))
                        .putString("passwordHash", ByteArrayUtil.toBase64(encryptedHash))
                        .apply()

                    Toast.makeText(this, "Ustawiono nowe hasło!", Toast.LENGTH_SHORT).show()

                    // powrót do MainActivity
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this, "Wystąpił błąd przy ustawianiu hasła!", Toast.LENGTH_SHORT).show()
                    Log.e("PasswordSetup", "Error initializing Cipher: ${e.message}", e)
                    e.printStackTrace()
                }
            } else {
                Toast.makeText(this, "Hasło nie może być puste!", Toast.LENGTH_SHORT).show()
            }
        }
    }
 }