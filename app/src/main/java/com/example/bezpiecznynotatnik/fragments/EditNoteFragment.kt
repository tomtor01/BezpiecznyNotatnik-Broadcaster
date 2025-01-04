package com.example.bezpiecznynotatnik.fragments

import com.example.bezpiecznynotatnik.Note
import com.example.bezpiecznynotatnik.NoteDao
import com.example.bezpiecznynotatnik.R
import com.example.bezpiecznynotatnik.SecureNotesApp
import com.example.bezpiecznynotatnik.utils.*

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources.getColorStateList
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class EditNoteFragment : Fragment() {

    private lateinit var editNoteInput: EditText
    private lateinit var saveButton: Button
    private lateinit var noteDao: NoteDao
    private var noteId: Int = -1
    private var originalNoteContent: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomNavigationView = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigationView.menu.findItem(R.id.nav_create).isChecked = true // Highlight Add Note
        bottomNavigationView.isEnabled = false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_edit_note, container, false)

        editNoteInput = view.findViewById(R.id.editNoteInput)
        saveButton = view.findViewById(R.id.saveButton)
        noteDao = (requireActivity().application as SecureNotesApp).noteDatabase.noteDao()

        // Get arguments from navigation
        noteId = arguments?.getInt("noteId") ?: -1
        originalNoteContent = arguments?.getString("noteContent") ?: ""

        // Initialize the input field with the original note content
        editNoteInput.setText(originalNoteContent)

        // Set up TextWatcher for input field
        setupTextWatcher()

        // Set up Save button
        saveButton.setOnClickListener { updateNote() }

        // Set up Delete button
        view.findViewById<View>(R.id.deleteNote).setOnClickListener { showDeleteConfirmationDialog() }

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(16, 16, 16, imeInsets.bottom + 16) // Adjust bottom padding dynamically
            insets
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        resetInputField()
    }

    private fun setupTextWatcher() {
        editNoteInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasChanged = s.toString() != originalNoteContent
                saveButton.isEnabled = hasChanged
                saveButton.isEnabled = hasChanged
                val (backgroundTintList, textColor) = if (hasChanged) {
                    ContextCompat.getColorStateList(requireContext(), R.color.md_theme_primary) to
                            ContextCompat.getColorStateList(requireContext(), R.color.md_theme_onPrimary)
                } else {
                    ContextCompat.getColorStateList(requireContext(), R.color.md_theme_surfaceVariant) to
                            ContextCompat.getColorStateList(requireContext(), R.color.md_theme_onSurfaceVariant)
                }

                saveButton.backgroundTintList = backgroundTintList
                saveButton.setTextColor(textColor)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun resetInputField() {
        editNoteInput.setText(originalNoteContent)
        saveButton.isEnabled = false
        saveButton.backgroundTintList = getColorStateList(requireContext(), R.color.md_theme_surfaceVariant)
    }

    private fun updateNote() {
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

                    Toast.makeText(requireContext(),
                        getString(R.string.note_updated), Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                } catch (e: Exception) {
                    Log.e("EditNoteFragment", "Error saving note: ${e.message}")
                    Toast.makeText(requireContext(),
                        getString(R.string.save_note_failure), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_note_dialog_tittle))
            .setMessage(getString(R.string.delete_note_dialog_text))
            .setPositiveButton(getString(R.string.yes)) { _, _ -> deleteNote() }
            .setNegativeButton(getString(R.string.no), null)
            .create()
            .show()
    }

    private fun deleteNote() {
        lifecycleScope.launch {
            try {
                noteDao.deleteById(noteId)
                Toast.makeText(requireContext(),
                    getString(R.string.note_deleted), Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } catch (e: Exception) {
                Toast.makeText(requireContext(),
                    getString(R.string.delete_note_failure), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
