package com.example.bezpiecznynotatnik.adapters

import com.example.bezpiecznynotatnik.R

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.ArrayAdapter
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsAdapter(
    private val settings: List<SettingItem>,
    private val onSignInClick: () -> Unit,
    private val onProfileClick: () -> Unit,
    private val onChangePasswordClick: () -> Unit,
    private val onFeedbackButtonClick: () -> Unit,
    private val onLanguageSelected: (Int) -> Unit,
    private val onApplyLanguageClick: (Int) -> Unit,
    private val onBiometricSwitchToggled: (Boolean) -> Unit,
    private val sharedPreferences: SharedPreferences
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int {
        return when (settings[position].type) {
            SettingType.ACCOUNT -> 0
            SettingType.AUTHENTICATION -> 1
            SettingType.LANGUAGE -> 2
            SettingType.FEEDBACK -> 3
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> AccountViewHolder(inflater.inflate(R.layout.view_account, parent, false))
            1 -> AuthViewHolder(inflater.inflate(R.layout.view_auth, parent, false))
            2 -> LanguageViewHolder(inflater.inflate(R.layout.view_language, parent, false))
            3 -> FeedbackViewHolder(inflater.inflate(R.layout.view_feedback, parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = settings[position]
        when (holder) {
            is AccountViewHolder -> holder.bind(item, onSignInClick, onProfileClick)
            is AuthViewHolder -> holder.bind(item, onChangePasswordClick, onBiometricSwitchToggled)
            is LanguageViewHolder -> holder.bind(item, onLanguageSelected, onApplyLanguageClick)
            is FeedbackViewHolder -> holder.bind(item, onFeedbackButtonClick)
        }
    }

    override fun getItemCount(): Int = settings.size

    inner class AccountViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.account_text)
        private val loggedInLayout: ConstraintLayout = itemView.findViewById(R.id.logged_in_layout)
        private val loggedOutLayout: ConstraintLayout = itemView.findViewById(R.id.logged_out_layout)
        private val profilePicture: ImageView = itemView.findViewById(R.id.profile_picture)
        private val accountStatusText: TextView = itemView.findViewById(R.id.account_status_text)

        @SuppressLint("SetTextI18n")
        fun bind(settingItem: SettingItem, onSignInClick: () -> Unit, onProfileClick: () -> Unit) {
            title.text = settingItem.title
            if (settingItem.isLoggedIn) {
                // Show logged-in layout
                loggedInLayout.visibility = View.VISIBLE
                loggedOutLayout.visibility = View.GONE

                // Set user information
                accountStatusText.text = "Welcome, ${settingItem.userName ?: "User"}"
                Glide.with(profilePicture.context)
                    .load(settingItem.profilePictureUrl)
                    .placeholder(R.drawable.ic_account)
                    .error(R.drawable.ic_account)
                    .circleCrop()
                    .into(profilePicture)

                // Handle profile click
                loggedInLayout.setOnClickListener {
                    onProfileClick()
                }
            } else {
                // Show logged-out layout
                loggedInLayout.visibility = View.GONE
                loggedOutLayout.visibility = View.VISIBLE

                // Handle sign-up click
                loggedOutLayout.setOnClickListener {
                    onSignInClick()
                }
            }
        }
    }

    inner class AuthViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val passwordTextView: TextView = itemView.findViewById(R.id.password_text_view)
        private val changePasswordButton: Button = itemView.findViewById(R.id.changePasswordButton)
        private val biometricSwitch: MaterialSwitch = itemView.findViewById(R.id.biometric_switch)

        fun bind(settingItem: SettingItem,
                 onChangePasswordClick: () -> Unit,
                 onBiometricSwitchToggled: (Boolean) -> Unit)
        {
            passwordTextView.text = settingItem.title
            changePasswordButton.text = settingItem.buttonLabel

            changePasswordButton.setOnClickListener {
                onChangePasswordClick()
            }
            val isBiometricEnabled = sharedPreferences.getBoolean("biometric_enabled", false)
            biometricSwitch.isChecked = isBiometricEnabled

            biometricSwitch.setOnCheckedChangeListener { _, isChecked ->
                onBiometricSwitchToggled(isChecked)
                sharedPreferences.edit().putBoolean("biometric_enabled", isChecked).apply()
            }
        }
    }

    inner class LanguageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val languageSpinner: Spinner = itemView.findViewById(R.id.language_spinner)
        private val applyButton: Button = itemView.findViewById(R.id.apply_language_button)

        fun bind(
            settingItem: SettingItem,
            onLanguageSelected: (Int) -> Unit,
            onApplyLanguageClick: (Int) -> Unit
        ) {
            val context = itemView.context
            val languageNames = context.resources.getStringArray(R.array.language_names)

            // Set up the spinner adapter
            val adapter = ArrayAdapter(context, R.layout.spinner_custom_items, languageNames)
            adapter.setDropDownViewResource(R.layout.spinner_custom_dropdown)
            languageSpinner.adapter = adapter

            // Preselect the current language
            val selectedIndex = context.resources.getStringArray(R.array.language_codes)
                .indexOf(settingItem.selectedLanguage)
                .takeIf { it >= 0 } ?: 0

            languageSpinner.setSelection(selectedIndex) // This ensures the correct language is shown.

            // Handle language selection
            languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    onLanguageSelected(position)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            // Handle apply button click
            applyButton.setOnClickListener {
                val selectedPosition = languageSpinner.selectedItemPosition
                onApplyLanguageClick(selectedPosition)
            }
        }
    }

    inner class FeedbackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val feedbackTextView: TextView = itemView.findViewById(R.id.feedback_text_view)
        private val feedbackButton: Button = itemView.findViewById(R.id.feedback_button)

        fun bind(settingItem: SettingItem,
                 onFeedbackButtonClick: () -> Unit) {
            feedbackTextView.text = settingItem.title
            feedbackButton.text = settingItem.buttonLabel
        }
    }
}
data class SettingItem(
    val type: SettingType,
    val title: String,
    val isLoggedIn: Boolean = false,
    val description: String? = null,
    val profilePictureUrl: String? = null,
    val userName: String? = null,
    val isToggled: Boolean? = null,
    val buttonLabel: String? = null,
    val languageOptions: List<String>? = null,
    var selectedLanguage: String? = null,
    var isBiometricEnabled: Boolean? = null
)

enum class SettingType {
    ACCOUNT, AUTHENTICATION, LANGUAGE, FEEDBACK
}