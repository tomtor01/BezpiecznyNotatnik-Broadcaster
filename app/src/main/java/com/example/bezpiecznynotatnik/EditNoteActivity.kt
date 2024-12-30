package com.example.bezpiecznynotatnik

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class EditNoteActivity : AppCompatActivity() {

    private lateinit var editNoteInput: EditText
    private lateinit var noteDao: NoteDao
    private var noteId: Int = -1

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_note)

        editNoteInput = findViewById(R.id.editNoteInput)

        // Get note data from intent
        val noteContent = intent.getStringExtra("noteContent") ?: ""

        noteId = intent.getIntExtra("noteId", -1)
        noteDao = (application as SecureNotesApp).noteDatabase.noteDao()

        // Display the note content
        editNoteInput.setText(noteContent)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigation2)

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.saveButton -> {
                    val updatedContent = editNoteInput.text.toString()
                    if (noteId != -1) {
                        lifecycleScope.launch {
                            try {
                                val (encryptedMessage, iv) = EncryptionUtil.encryptMessage(updatedContent)

                                val updatedNote = Note(
                                    id = noteId,
                                    encryptedMessage = ByteArrayUtil.toBase64(encryptedMessage),
                                    iv = ByteArrayUtil.toBase64(iv)
                                )
                                noteDao.update(updatedNote)

                                Toast.makeText(this@EditNoteActivity, "Note saved!", Toast.LENGTH_SHORT).show()
                                val backIntent = Intent(this@EditNoteActivity, AccessActivity::class.java)
                                startActivity(backIntent)
                                finish()
                            } catch (e: Exception) {
                                Log.e("EditNoteActivity", "Error saving note: ${e.message}")
                                Toast.makeText(this@EditNoteActivity, "Failed to save note.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    true
                }
                R.id.deleteNote -> {
                    showDeleteConfirmationDialog()
                    true
                }
                R.id.goBackButton -> {
                    val backIntent = Intent(this@EditNoteActivity, AccessActivity::class.java)
                    startActivity(backIntent)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Note")
            .setMessage("Are you sure you want to delete this note?")
            .setPositiveButton("Yes") { _, _ -> deleteNote() }
            .setNegativeButton("No", null)
            .create()
            .show()
    }

    private fun deleteNote() {
        lifecycleScope.launch {
            try {
                noteDao.deleteById(noteId)
                Toast.makeText(this@EditNoteActivity, "Note deleted successfully!", Toast.LENGTH_SHORT).show()
                val backIntent = Intent(this@EditNoteActivity, AccessActivity::class.java)
                startActivity(backIntent)
                finish()
            } catch (e: Exception) {
                Log.e("EditNoteActivity", "Error deleting note: ${e.message}")
                Toast.makeText(this@EditNoteActivity, "Failed to delete note.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}