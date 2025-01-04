package com.example.bezpiecznynotatnik.fragments

import com.example.bezpiecznynotatnik.Note
import com.example.bezpiecznynotatnik.NoteDao
import com.example.bezpiecznynotatnik.R
import com.example.bezpiecznynotatnik.SecureNotesApp
import com.example.bezpiecznynotatnik.utils.ByteArrayUtil
import com.example.bezpiecznynotatnik.utils.EncryptionUtil

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
import androidx.appcompat.content.res.AppCompatResources.getColorStateList
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch

class AddNoteFragment : Fragment() {

    private lateinit var noteInput: EditText
    private lateinit var saveButton: Button
    private lateinit var noteDao: NoteDao

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_add_note, container, false)

        noteInput = view.findViewById(R.id.messageInput)
        saveButton = view.findViewById(R.id.saveButton)
        noteDao = (requireActivity().application as SecureNotesApp).noteDatabase.noteDao()

        addedTextWatcher()
        setupSaveButton()

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

    private fun addedTextWatcher() {
        noteInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrEmpty()
                saveButton.isEnabled = hasText
                val (backgroundTintList, textColor) = if (hasText) {
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

    private fun setupSaveButton() {
        saveButton.setOnClickListener {
            saveNote()
            findNavController().navigateUp() // Navigate back after saving
        }
    }

    private fun resetInputField() {
        noteInput.text.clear()
        saveButton.isEnabled = false
        saveButton.backgroundTintList = getColorStateList(requireContext(), R.color.md_theme_surfaceVariant)
    }

    private fun saveNote() {
        val newNote = noteInput.text.toString()
        if (newNote.isNotEmpty()) {
            lifecycleScope.launch {
                try {
                    val (encryptedMessage, iv) = EncryptionUtil.encryptMessage(newNote)
                    val note = Note(
                        id = 0,
                        encryptedMessage = ByteArrayUtil.toBase64(encryptedMessage),
                        iv = ByteArrayUtil.toBase64(iv)
                    )
                    noteDao.insert(note)

                    Toast.makeText(requireContext(), getString(R.string.note_added), Toast.LENGTH_SHORT).show()
                    resetInputField()
                } catch (e: Exception) {
                    Log.e("AddNoteFragment", "Error adding note: ${e.message}")
                    Toast.makeText(requireContext(), getString(R.string.add_note_failure), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
