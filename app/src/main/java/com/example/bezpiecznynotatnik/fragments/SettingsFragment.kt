package com.example.bezpiecznynotatnik.fragments

import com.example.bezpiecznynotatnik.R
import com.example.bezpiecznynotatnik.utils.*
import com.example.bezpiecznynotatnik.SecureNotesApp
import com.example.bezpiecznynotatnik.adapters.SettingItem
import com.example.bezpiecznynotatnik.adapters.SettingType
import com.example.bezpiecznynotatnik.adapters.SettingsAdapter
import com.example.bezpiecznynotatnik.UserState
import com.example.bezpiecznynotatnik.data.GoogleDriveBackupManager
import com.example.bezpiecznynotatnik.databinding.FragmentSettingsBinding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import android.app.Activity.RESULT_OK
import android.content.Context
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import java.util.Locale

class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding
    private lateinit var googleDriveManager: GoogleDriveBackupManager
    private var selectedLanguageIndex: Int = 0

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

        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.nav_settings, true) // Pop up to settings fragment, clearing the stack from settings onward
            .build()

        val sharedPrefs = requireContext().getSharedPreferences("SecureNotesPrefs", Context.MODE_PRIVATE)

        val settingsList = listOf(
            SettingItem(
                type = SettingType.ACCOUNT,
                title = getString(R.string.account),
                isLoggedIn = googleDriveManager.isUserSignedIn(),
                userName = googleDriveManager.getSignedInUserName(requireActivity()),
                profilePictureUrl = googleDriveManager.getProfilePictureUrl(requireActivity())
            ),
            SettingItem(
                type = SettingType.AUTHENTICATION,
                title = getString(R.string.authentication_settings),
                buttonLabel = getString(R.string.change_password)
            ),
            SettingItem(
                type = SettingType.LANGUAGE,
                title = getString(R.string.choose_language),
                languageOptions = resources.getStringArray(R.array.language_names).toList()
            ),
            SettingItem(
                type = SettingType.FEEDBACK,
                title = getString(R.string.feedback),
                buttonLabel = getString(R.string.send_feedback)
            )
        )
        val adapter = SettingsAdapter(
            settings = settingsList,
            onSignInClick = { signInLauncher.launch(googleDriveManager.getSignInIntent()) },
            onProfileClick = { findNavController().navigate(R.id.action_settings_to_account, null, navOptions) },
            onChangePasswordClick = { showChangePasswordDialog() },
            onLanguageSelected = { index -> selectedLanguageIndex = index },
            onApplyLanguageClick = { index -> applySelectedLanguage(index) },
            onBiometricSwitchToggled = { isEnabled -> toggleBiometricAuthentication(isEnabled) },
            sharedPreferences = sharedPrefs
        )

        val recyclerView: RecyclerView = view.findViewById(R.id.settingsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        initializeLanguageSelection(settingsList.component3())
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
    private fun initializeLanguageSelection(languageItem: SettingItem) {
        val languageCodes = resources.getStringArray(R.array.language_codes)
        val currentLanguageCode = PreferenceHelper.getLanguage(requireContext()) ?: Locale.getDefault().language
        selectedLanguageIndex = languageCodes.indexOf(currentLanguageCode).takeIf { it >= 0 } ?: 0

        // Update the languageItem's selected language
        languageItem.selectedLanguage = languageCodes[selectedLanguageIndex]
    }

    private fun applySelectedLanguage(selectedPosition: Int) {
        val languageCodes = resources.getStringArray(R.array.language_codes)
        val selectedLanguageCode = languageCodes[selectedPosition]
        val currentLanguageCode = PreferenceHelper.getLanguage(requireContext()) ?: Locale.getDefault().language

        if (selectedLanguageCode == currentLanguageCode) {
            Toast.makeText(requireContext(), getString(R.string.language_already_set), Toast.LENGTH_SHORT).show()
            return
        }

        // Save the selected language
        PreferenceHelper.setLanguage(requireContext(), selectedLanguageCode)
        LocaleHelper.setLocale(requireContext(), selectedLanguageCode)

        Toast.makeText(
            requireContext(),
            getString(R.string.language_changed, resources.getStringArray(R.array.language_names)[selectedPosition]),
            Toast.LENGTH_SHORT
        ).show()

        // Recreate the activity to apply the language change
        activity?.let {
            val intent = it.intent
            it.finish()
            startActivity(intent)
        }
    }

    private fun toggleBiometricAuthentication(isEnabled: Boolean) {
        if (isEnabled) {
            // Enable biometric authentication logic
            Toast.makeText(requireContext(), "Biometric authentication enabled", Toast.LENGTH_SHORT).show()
        } else {
            // Disable biometric authentication logic
            Toast.makeText(requireContext(), "Biometric authentication disabled", Toast.LENGTH_SHORT).show()
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
