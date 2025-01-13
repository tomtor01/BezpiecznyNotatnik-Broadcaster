package com.example.bezpiecznynotatnik.fragments

import android.app.Activity.RESULT_OK
import com.example.bezpiecznynotatnik.R
import com.example.bezpiecznynotatnik.utils.*

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.bezpiecznynotatnik.SecureNotesApp
import com.example.bezpiecznynotatnik.data.UserState
import com.example.bezpiecznynotatnik.data.GoogleDriveBackupManager
import com.example.bezpiecznynotatnik.databinding.FragmentSettingsBinding
import com.google.firebase.auth.FirebaseAuth

class SettingsFragment : Fragment() {

    private lateinit var changePasswordButton: Button
    private lateinit var languageSpinner: Spinner
    private lateinit var applyLanguageButton: Button
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: FragmentSettingsBinding
    private lateinit var googleDriveManager: GoogleDriveBackupManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        googleDriveManager = (requireActivity().applicationContext as SecureNotesApp).googleDriveManager
        googleDriveManager.initializeGoogleSignIn(requireActivity())
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        changePasswordButton = view.findViewById(R.id.changePasswordButton)
        languageSpinner = view.findViewById(R.id.language_spinner)
        applyLanguageButton = view.findViewById(R.id.apply_language_button)
        updateAccountLayout()
        setupLanguageSpinner()
        firebaseAuth = FirebaseAuth.getInstance()

        changePasswordButton.setOnClickListener {
            showChangePasswordDialog()
        }
        applyLanguageButton.setOnClickListener {
            applySelectedLanguage()
        }
    }

    private fun updateAccountLayout() {
        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.nav_settings, true) // Pop up to settings fragment, clearing the stack from settings onward
            .build()

        val userState = googleDriveManager.isUserSignedIn()
        if (userState) {
            binding.loggedInLayout.visibility = View.VISIBLE
            binding.loggedOutLayout.visibility = View.GONE
            val userName = googleDriveManager.getSignedInUserName(requireActivity())
            val profilePictureUrl = googleDriveManager.getProfilePictureUrl(requireActivity())
            binding.accountStatusText.text = getString(R.string.welcome_user, userName)

            Glide.with(this)
                .load(profilePictureUrl)
                .placeholder(R.drawable.ic_account) // Placeholder image while loading
                .error(R.drawable.ic_account) // Fallback if the image fails to load
                .circleCrop() // Crop the image into a circle
                .into(binding.profilePicture)

            binding.loggedInLayout.setOnClickListener {
                findNavController().navigate(R.id.action_settings_to_account, null, navOptions)
            }
        } else {
            binding.loggedInLayout.visibility = View.GONE
            binding.loggedOutLayout.visibility = View.VISIBLE

            binding.loggedOutLayout.setOnClickListener {
                val signInIntent = googleDriveManager.getSignInIntent()
                signInLauncher.launch(signInIntent)
            }
        }
    }

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                googleDriveManager.handleSignInResult(requireActivity(),
                    data = result.data,
                    onSuccess = {
                        UserState.isUserSignedIn = true
                        findNavController().navigate(R.id.nav_settings)
                        Toast.makeText(requireContext(), "Sign-In successful!", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { errorMessage ->
                        UserState.isUserSignedIn = false
                        Toast.makeText(requireContext(), "Sign-In failed: $errorMessage", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                Toast.makeText(requireContext(), "Sign-In canceled or failed.", Toast.LENGTH_SHORT).show()
            }
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

        Toast.makeText(requireContext(),
            getString(R.string.language_changed, languageSpinner.selectedItem), Toast.LENGTH_SHORT).show()

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

                if (newPassword.isNotEmpty() && repeatPassword.isNotEmpty()) {
                    if (newPassword != repeatPassword) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.not_equal_passwords), Toast.LENGTH_SHORT
                        ).show()
                        newPasswordInput.text.clear()
                        repeatPasswordInput.text.clear()
                    } else {    // on success
                        changePassword(requireContext(), newPassword)
                        dialog.dismiss()
                    }
                }
            }
        }
        dialog.show()
    }
}
