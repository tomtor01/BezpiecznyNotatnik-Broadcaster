package com.example.bezpiecznynotatnik.activities

import com.example.bezpiecznynotatnik.R
import com.example.bezpiecznynotatnik.utils.*

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

import java.util.Locale

class PasswordSetupActivity : AppCompatActivity() {
    private lateinit var newPasswordInput: EditText
    private lateinit var repeatPasswordInput: EditText
    private lateinit var setPasswordButton: Button
    private lateinit var sharedPrefs: SharedPreferences

    override fun attachBaseContext(newBase: Context?) {
        if (newBase == null) {
            super.attachBaseContext(null)
            return
        }
        val language = PreferenceHelper.getLanguage(newBase) ?: Locale.getDefault().language
        val context = LocaleHelper.setLocale(newBase, language)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_password_setup)

        newPasswordInput = findViewById(R.id.setPasswordInput)
        repeatPasswordInput = findViewById(R.id.repeatSetPasswordInput)
        setPasswordButton = findViewById(R.id.setPasswordButton)
        sharedPrefs = getSharedPreferences("SecureNotesPrefs", MODE_PRIVATE)

        setPasswordButton.setOnClickListener {
            val newPassword = newPasswordInput.text.toString()
            val repeatPassword = repeatPasswordInput.text.toString()

            if (newPassword.isEmpty() || repeatPassword.isEmpty()) {
                // do nothing
            } else if (newPassword != repeatPassword) {
                Toast.makeText(this,
                    getString(R.string.not_equal_passwords), Toast.LENGTH_SHORT).show()
                repeatPasswordInput.text.clear()
            } else {
                // Call changePassword and dismiss dialog on success
                changePassword(this, newPassword)
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
}