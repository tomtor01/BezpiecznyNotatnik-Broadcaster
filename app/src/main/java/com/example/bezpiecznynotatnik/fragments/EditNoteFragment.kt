package com.example.bezpiecznynotatnik.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.bezpiecznynotatnik.Note
import com.example.bezpiecznynotatnik.NoteDao
import com.example.bezpiecznynotatnik.R
import com.example.bezpiecznynotatnik.SecureNotesApp
import com.example.bezpiecznynotatnik.utils.ByteArrayUtil
import com.example.bezpiecznynotatnik.utils.EncryptionUtil
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class EditNoteFragment : Fragment() {

    private lateinit var editNoteInput: EditText
    private lateinit var noteDao: NoteDao
    private var noteId: Int = -1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomNavigationView = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigationView.menu.findItem(R.id.nav_addNote).isChecked = true // Highlight Add Note
        bottomNavigationView.isEnabled = false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_edit_note, container, false)

        editNoteInput = view.findViewById(R.id.editNoteInput)
        noteDao = (requireActivity().application as SecureNotesApp).noteDatabase.noteDao()

        // Get arguments from navigation
        noteId = arguments?.getInt("noteId") ?: -1
        val noteContent = arguments?.getString("noteContent") ?: ""

        editNoteInput.setText(noteContent)

        view.findViewById<View>(R.id.saveButton).setOnClickListener { updateNote() }
        view.findViewById<View>(R.id.deleteNote).setOnClickListener { showDeleteConfirmationDialog() }

        return view
    }

    private fun updateNote() {
        val updatedContent = editNoteInput.text.toString()

        if (noteId != -1) {
            lifecycleScope.launch {
                try {
                    val (encryptedMessage, iv) = EncryptionUtil.encryptMessage(
                        updatedContent
                    )
                    val updatedNote = Note(
                        id = noteId,
                        encryptedMessage = ByteArrayUtil.toBase64(encryptedMessage),
                        iv = ByteArrayUtil.toBase64(iv)
                    )
                    noteDao.update(updatedNote)

                    Toast.makeText(requireContext(), "Note saved!", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                } catch (e: Exception) {
                    Log.e("EditNoteActivity", "Error saving note: ${e.message}")
                    Toast.makeText(requireContext(), "Failed to save note.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
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
                Toast.makeText(requireContext(), "Note deleted successfully!", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } catch (e: Exception) {
                Log.e("EditNoteActivity", "Error deleting note: ${e.message}")
                Toast.makeText(requireContext(), "Failed to delete note.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
