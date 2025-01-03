package com.example.bezpiecznynotatnik.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bezpiecznynotatnik.MainActivity
import com.example.bezpiecznynotatnik.R
import com.example.bezpiecznynotatnik.utils.ByteArrayUtil
import com.example.bezpiecznynotatnik.utils.EncryptionUtil
import com.example.bezpiecznynotatnik.utils.HashUtil
import com.example.bezpiecznynotatnik.utils.LocaleHelper
import com.example.bezpiecznynotatnik.utils.PreferenceHelper
import com.example.bezpiecznynotatnik.utils.SaltUtil
import java.util.Locale

class PasswordSetupActivity : AppCompatActivity() {
    private lateinit var newPasswordInput: EditText
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
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_password_setup)

        newPasswordInput = findViewById(R.id.newPasswordInput)
        setPasswordButton = findViewById(R.id.setPasswordButton)
        sharedPrefs = getSharedPreferences("SecureNotesPrefs", MODE_PRIVATE)

        setPasswordButton.setOnClickListener {
            val newPassword = newPasswordInput.text.toString()

            if (newPassword.isNotEmpty()) {
                try {
                    val salt = SaltUtil.generateSalt()
                    val passwordHash = HashUtil.hashPassword(newPassword, salt)
                    val (iv, encryptedHash) = EncryptionUtil.encryptHash(passwordHash)

                    sharedPrefs.edit()
                        .putString("password_salt", ByteArrayUtil.toBase64(salt))
                        .putString("iv", ByteArrayUtil.toBase64(iv))
                        .putString("passwordHash", ByteArrayUtil.toBase64(encryptedHash))
                        .apply()

                    Toast.makeText(this, "Ustawiono nowe hasło!", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this, "Wystąpił błąd przy ustawianiu hasła!", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            } else {
                Toast.makeText(this, "Hasło nie może być puste!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}