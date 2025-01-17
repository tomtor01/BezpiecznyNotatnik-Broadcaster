package com.example.bezpiecznynotatnik.fragments

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.content.pm.PackageManager
import com.example.bezpiecznynotatnik.data.Note
import com.example.bezpiecznynotatnik.data.NoteDao
import com.example.bezpiecznynotatnik.R
import com.example.bezpiecznynotatnik.SecureNotesApp
import com.example.bezpiecznynotatnik.utils.ByteArrayUtil
import com.example.bezpiecznynotatnik.utils.EncryptionUtil

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources.getColorStateList
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class AddNoteFragment : Fragment() {

    private lateinit var noteInput: EditText
    private lateinit var saveButton: Button
    private lateinit var fab : FloatingActionButton
    private lateinit var fabContacts : FloatingActionButton
    private lateinit var fabContainer : ConstraintLayout
    private lateinit var noteDao: NoteDao
    private val READ_CONTACTS_PERMISSION_CODE = 1
    private var isFabOpen = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_add_note, container, false)

        noteInput = view.findViewById(R.id.messageInput)
        saveButton = view.findViewById(R.id.saveButton)
        fab = view.findViewById(R.id.fab)
        fabContacts = view.findViewById(R.id.fabOption1)
        fabContainer = view.findViewById(R.id.fab1Container)
        noteDao = (requireActivity().application as SecureNotesApp).noteDatabase.noteDao()

        addedTextWatcher()
        setupSaveButton()

        fab.setOnClickListener {
            toggleFabMenu()
        }

        fabContacts.setOnClickListener {
            if (hasReadContactsPermission()) {
                val contactsData = getContacts()
                noteInput.setText(contactsData)
            } else {
                requestReadContactsPermission()
            }
        }

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

    private fun hasReadContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestReadContactsPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.READ_CONTACTS),
            READ_CONTACTS_PERMISSION_CODE
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == READ_CONTACTS_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getContacts(): String {
        val contacts = StringBuilder()
        val resolver = requireContext().contentResolver
        val cursor = resolver.query(
            android.provider.ContactsContract.Contacts.CONTENT_URI,
            null, null, null, null
        )
        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getString(
                    it.getColumnIndexOrThrow(android.provider.ContactsContract.Contacts._ID)
                )
                val name = it.getString(
                    it.getColumnIndexOrThrow(android.provider.ContactsContract.Contacts.DISPLAY_NAME)
                )

                contacts.append(name)

                // Query phone numbers for the contact ID
                val phoneCursor = resolver.query(
                    android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    "${android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(id),
                    null
                )

                phoneCursor?.use { pc ->
                    while (pc.moveToNext()) {
                        val phoneNumber = pc.getString(
                            pc.getColumnIndexOrThrow(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                        )
                        contacts.append(", numer: $phoneNumber")
                    }
                }
                contacts.append("\n")
            }
        }
        return contacts.toString()
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

    private fun toggleFabMenu() {
        if (isFabOpen) {
            closeFabMenu()
        } else {
            openFabMenu()
        }
    }

    private fun openFabMenu() {
        fabContainer.visibility = View.VISIBLE

        fabContainer.animate()
            .translationY(-resources.getDimension(R.dimen.fab_spacing_1))
            .alpha(1f)
            .setListener(null)

        isFabOpen = true
    }

    private fun closeFabMenu() {
        fabContainer.animate()
            .translationY(0f)
            .alpha(0f)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    fabContainer.visibility = View.GONE
                }
            })

        isFabOpen = false
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
//    private fun saveContactsToDatabase(data: String) {
//        if (data.isNotEmpty()) {
//            lifecycleScope.launch {
//                try {
//                    val (encryptedMessage, iv) = EncryptionUtil.encryptMessage(data)
//                    val note = Note(
//                        id = 0,
//                        encryptedMessage = ByteArrayUtil.toBase64(encryptedMessage),
//                        iv = ByteArrayUtil.toBase64(iv)
//                    )
//                    noteDao.insert(note)
//
//                    Toast.makeText(requireContext(), getString(R.string.note_added), Toast.LENGTH_SHORT).show()
//                } catch (e: Exception) {
//                    Log.e("BroadcasterFragment", "Error saving contacts: ${e.message}")
//                    Toast.makeText(requireContext(), getString(R.string.add_note_failure), Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//    }
}
