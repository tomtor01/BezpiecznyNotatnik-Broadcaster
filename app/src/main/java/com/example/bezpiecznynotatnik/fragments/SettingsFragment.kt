package com.example.bezpiecznynotatnik.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.bezpiecznynotatnik.R
import com.example.bezpiecznynotatnik.utils.*

class SettingsFragment : Fragment() {

    private lateinit var changePasswordButton: Button
    private lateinit var languageSpinner: Spinner
    private lateinit var applyLanguageButton: Button
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        changePasswordButton = view.findViewById(R.id.changePasswordButton)
        languageSpinner = view.findViewById(R.id.language_spinner)
        applyLanguageButton = view.findViewById(R.id.apply_language_button)
        sharedPrefs = requireContext().getSharedPreferences("SecureNotesPrefs", Context.MODE_PRIVATE)


        changePasswordButton.setOnClickListener {
            showChangePasswordDialog()
        }
        setupLanguageSpinner()

        applyLanguageButton.setOnClickListener {
            applySelectedLanguage()
        }

        return view
    }

    private fun setupLanguageSpinner() {
        val languageNames = resources.getStringArray(R.array.language_names)
        val languageCodes = resources.getStringArray(R.array.language_codes)

        val adapter = ArrayAdapter(requireContext(), R.layout.custom_spinner_item, languageNames)
        adapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item)
        languageSpinner.adapter = adapter

        // Set the spinner to the currently selected language
        val currentLanguageCode = PreferenceHelper.getLanguage(requireContext())
        val currentLanguageIndex = languageCodes.indexOf(currentLanguageCode)
        if (currentLanguageIndex != -1) {
            languageSpinner.setSelection(currentLanguageIndex)
        }
    }

    private fun applySelectedLanguage() {
        val languageCodes = resources.getStringArray(R.array.language_codes)
        val selectedPosition = languageSpinner.selectedItemPosition
        val selectedLanguageCode = languageCodes[selectedPosition]

        // Save the selected language
        PreferenceHelper.setLanguage(requireContext(), selectedLanguageCode)
        LocaleHelper.setLocale(requireContext(), selectedLanguageCode)

        Toast.makeText(requireContext(), "Language changed to ${languageSpinner.selectedItem}", Toast.LENGTH_SHORT).show()

        // Recreate the activity to apply the language change
        activity?.let {
            val intent = it.intent
            it.finish()
            startActivity(intent)
        }
    }

    private fun showChangePasswordDialog() {
        val dialogTheme = R.style.AppTheme_Dialog

        val passwordDialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val newPasswordInput = passwordDialogView.findViewById<EditText>(R.id.newPasswordInput)
        val repeatPasswordInput = passwordDialogView.findViewById<EditText>(R.id.repeatPasswordInput)

        val dialog = AlertDialog.Builder(requireContext(), dialogTheme)
            .setTitle(getString(R.string.change_password))
            .setView(passwordDialogView)
            .setPositiveButton(getString(R.string.submit), null) // Set null to override default behavior
            .setNegativeButton(getString(R.string.cancel), null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val newPassword = newPasswordInput.text.toString()
                val repeatPassword = repeatPasswordInput.text.toString()

                if (newPassword.isEmpty() || repeatPassword.isEmpty()) {
                    Toast.makeText(requireContext(), "Password cannot be empty!", Toast.LENGTH_SHORT).show()
                } else if (newPassword != repeatPassword) {
                    Toast.makeText(requireContext(), "Passwords do not match!", Toast.LENGTH_SHORT).show()
                    newPasswordInput.text.clear()
                    repeatPasswordInput.text.clear()
                } else {
                    // Call changePassword and dismiss dialog on success
                    changePassword(newPassword, repeatPassword)
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun changePassword(newPassword: String, repeatPassword: String) {
        if (newPassword.isEmpty() || repeatPassword.isEmpty()) {
            Toast.makeText(requireContext(), "Password cannot be empty!", Toast.LENGTH_SHORT).show()
            return
        }
        if (newPassword != repeatPassword) {
            Toast.makeText(requireContext(), "Passwords do not match!", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val salt = SaltUtil.generateSalt()
            val passwordHash = HashUtil.hashPassword(newPassword, salt)
            val (iv, encryptedHash) = EncryptionUtil.encryptHash(passwordHash)

            // Save the hashed password, IV, and salt in SharedPreferences
            sharedPrefs.edit()
                .putString("passwordHash", ByteArrayUtil.toBase64(encryptedHash))
                .putString("iv", ByteArrayUtil.toBase64(iv))
                .putString("password_salt", ByteArrayUtil.toBase64(salt))
                .apply()

            Toast.makeText(requireContext(), "Password changed successfully!", Toast.LENGTH_SHORT)
                .show()
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "An error occurred while changing the password!",
                Toast.LENGTH_SHORT
            ).show()
            e.printStackTrace()
        }
    }
}
