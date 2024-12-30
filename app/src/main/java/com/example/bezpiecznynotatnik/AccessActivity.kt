package com.example.bezpiecznynotatnik

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class AccessActivity : AppCompatActivity() {

    private lateinit var changePasswordButton: Button
    private lateinit var recyclerViewNotes: RecyclerView
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var noteDao: NoteDao

    @SuppressLint("SetTextI18n", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_access)

        changePasswordButton = findViewById(R.id.changePasswordButton)
        recyclerViewNotes = findViewById(R.id.recyclerViewNotes)
        sharedPrefs = getSharedPreferences("SecureNotesPrefs", Context.MODE_PRIVATE)

        noteDao = (application as SecureNotesApp).noteDatabase.noteDao()

        // Change Password functionality
        changePasswordButton.setOnClickListener {

            val passwordDialogView = layoutInflater.inflate(R.layout.change_password_dialog, null)

            // Find the EditText in the dialog layout
            val newPasswordInput = passwordDialogView.findViewById<EditText>(R.id.newPasswordInput)

            // Build and show the dialog
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.change_password))
                .setView(passwordDialogView) // Set the custom view
                .setPositiveButton(getString(R.string.submit)) { _, _ ->
                    val newPassword = newPasswordInput.text.toString()
                    if (newPassword.isNotEmpty()) {
                        try {
                            // Generate salt for the new password
                            val salt = SaltUtil.generateSalt()
                            val passwordHash = HashUtil.hashPassword(newPassword, salt)
                            val (iv, encryptedHash) = EncryptionUtil.encryptHash(passwordHash)

                            // Save the hashed password, IV, and salt in SharedPreferences
                            sharedPrefs.edit()
                                .putString("passwordHash", ByteArrayUtil.toBase64(encryptedHash))
                                .putString("iv", ByteArrayUtil.toBase64(iv))
                                .putString("password_salt", ByteArrayUtil.toBase64(salt))
                                .apply()

                            Toast.makeText(this, "Pomyślnie zmieniono hasło!", Toast.LENGTH_SHORT)
                                .show()
                        } catch (e: Exception) {
                            Toast.makeText(
                                this,
                                "Wystąpił błąd przy zmianie hasła!",
                                Toast.LENGTH_SHORT
                            ).show()
                            e.printStackTrace()
                        }
                    } else {
                        Toast.makeText(this, "Hasło nie może być puste!", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton(getString(R.string.cancel), null) // Dismiss dialog on cancel
                .create()
                .show()
        }


        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigation1)

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.addMessageButton -> {
                    saveNote()
                    true
                }
                R.id.logoutButton -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }
        loadNotes()
    }

    private fun saveNote() {
        val messageDialogView = layoutInflater.inflate(R.layout.add_note_dialog, null)
        val messageInput = messageDialogView.findViewById<EditText>(R.id.messageInput)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.add_note))
            .setView(messageDialogView)
            .setPositiveButton(getString(R.string.save_message)) { _, _ ->
                val newMessage = messageInput.text.toString()
                if (newMessage.isNotEmpty()) {
                    lifecycleScope.launch {
                        try {
                            // Encrypt message
                            val (encryptedMessage, iv) = EncryptionUtil.encryptMessage(newMessage)
                            val note = Note(
                                id = 0, // Auto-generated by the database
                                encryptedMessage = ByteArrayUtil.toBase64(encryptedMessage),
                                iv = ByteArrayUtil.toBase64(iv)
                            )

                            // Save note to the database
                            noteDao.insert(note)

                            Toast.makeText(this@AccessActivity, "Note added successfully.", Toast.LENGTH_SHORT).show()
                            loadNotes() // Refresh RecyclerView
                        } catch (e: Exception) {
                            Log.e("AccessActivity", "Error adding note: ${e.message}")
                            Toast.makeText(this@AccessActivity, "Error adding the note.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Cannot add an empty note.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
            .show()
    }

    private fun loadNotes() {
        lifecycleScope.launch {
            val notes = noteDao.getAllNotes()

            val decryptedNotes = notes.map { note ->
                try {
                    EncryptionUtil.decryptMessage(
                        ByteArrayUtil.fromBase64(note.encryptedMessage),
                        ByteArrayUtil.fromBase64(note.iv)
                    )
                } catch (e: Exception) {
                    "Error decrypting note"
                }
            }

            // Set up the adapter
            val adapter = NotesAdapter(
                decryptedNotes,
                notes,
                onEditNote = { note ->
                    val intent = Intent(this@AccessActivity, EditNoteActivity::class.java)
                    intent.putExtra("noteId", note.id)
                    intent.putExtra("noteContent", decryptedNotes[notes.indexOf(note)])
                    startActivity(intent)
                }
            )
            recyclerViewNotes.adapter = adapter
            recyclerViewNotes.layoutManager = LinearLayoutManager(this@AccessActivity)
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
