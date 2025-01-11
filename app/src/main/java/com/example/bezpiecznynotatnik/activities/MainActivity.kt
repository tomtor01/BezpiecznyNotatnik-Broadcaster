package com.example.bezpiecznynotatnik.activities

import com.example.bezpiecznynotatnik.R
import com.example.bezpiecznynotatnik.utils.*
import com.example.bezpiecznynotatnik.data.NoteDao

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannedString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.bezpiecznynotatnik.SecureNotesApp
import com.example.bezpiecznynotatnik.data.GoogleDriveBackupManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var passwordInput: EditText
    private lateinit var loginWithPasswordButton: Button
    private lateinit var loginWithBiometricsButton: Button
    private lateinit var forgotPassword: TextView
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var noteDao: NoteDao
    private lateinit var auth: FirebaseAuth
    private lateinit var googleDriveManager: GoogleDriveBackupManager

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
        setContentView(R.layout.activity_main)

        passwordInput = findViewById(R.id.passwordInput)
        forgotPassword = findViewById(R.id.forgot_password)
        loginWithPasswordButton = findViewById(R.id.loginWithPasswordButton)
        loginWithBiometricsButton = findViewById(R.id.loginWithBiometricsButton)
        sharedPrefs = getSharedPreferences("SecureNotesPrefs", MODE_PRIVATE)
        auth = FirebaseAuth.getInstance()

        val spanString = SpannableString(getString(R.string.forgot_password))
        val forgotPasswordText = object : ClickableSpan() {
            override fun onClick(widget: View) {
                //showPasswordResetDialog()
            }
        }
        spanString.setSpan(forgotPasswordText, 0, spanString.length, SpannedString.SPAN_EXCLUSIVE_EXCLUSIVE)
        forgotPassword.text = spanString
        forgotPassword.movementMethod = LinkMovementMethod.getInstance()

        val saltBase64 = sharedPrefs.getString("password_salt", null)
        val encryptedHashBase64 = sharedPrefs.getString("passwordHash", null)
        val ivBase64 = sharedPrefs.getString("iv", null)

        if (saltBase64 == null || encryptedHashBase64 == null || ivBase64 == null) {
            Toast.makeText(this, "Utwórz nowe hasło", Toast.LENGTH_SHORT).show()
            redirectToPasswordSetup()
            return
        }
        googleDriveManager = (application as SecureNotesApp).googleDriveManager
        googleDriveManager.initializeGoogleSignIn(applicationContext)

        loginWithPasswordButton.setOnClickListener {
            authenticateWithPassword()
        }
        loginWithBiometricsButton.setOnClickListener {
            authenticateWithBiometrics()
        }
    }

    private fun anonymousAuth() {
        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.isAnonymous) {
            // Reuse existing anonymous user
            Log.d("MainActivity", "Reusing existing anonymous user: ${currentUser.uid}")
            navigateToAccessActivity()
            googleDriveManager.silentSignIn(applicationContext,
                onSuccess = {
                    Toast.makeText(this@MainActivity, "Automatically signed in to Google Drive!", Toast.LENGTH_SHORT).show()
                },
                onFailure = { errorMessage ->
                    Log.w("AccountActivity", "Silent sign-in failed: $errorMessage")
                }
            )
        } else {
            // Sign in anonymously
            auth.signInAnonymously()
                .addOnSuccessListener {
                    val user = it.user
                    Log.d("MainActivity", "Anonymous authentication succeeded for user ID: ${user?.uid}")
                    navigateToAccessActivity()
                }
                .addOnFailureListener { exception ->
                    Log.e("MainActivity", "Anonymous authentication failed: ${exception.message}")
                    Toast.makeText(this, "Authentication failed. Please try again.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun redirectToPasswordSetup() {
        val intent = Intent(this, PasswordSetupActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun authenticateWithPassword() {
        val enteredPassword = passwordInput.text.toString()
        if (enteredPassword.isEmpty()) {
            return
        }
        try {
            val attemptCounter = sharedPrefs.getInt("attemptCounter", 0)
            if (attemptCounter >= 10) {
                resetPassword()
                return
            }
            val saltBase64 = sharedPrefs.getString("password_salt", null)
            val encryptedHashBase64 = sharedPrefs.getString("passwordHash", null)
            val ivBase64 = sharedPrefs.getString("iv", null)

            val salt = ByteArrayUtil.fromBase64(saltBase64.toString())
            val hashedPassword = HashUtil.hashPassword(enteredPassword, salt)

            val storedEncryptedHash = ByteArrayUtil.fromBase64(encryptedHashBase64.toString())
            val storedIv = ByteArrayUtil.fromBase64(ivBase64.toString())

            val decryptedHash = EncryptionUtil.decryptHash(storedIv, storedEncryptedHash)
            if (hashedPassword.contentEquals(decryptedHash)) {
                anonymousAuth()
                sharedPrefs.edit().putInt("attemptCounter", 0)
                    .apply() // Reset the counter on success
            } else {
                handleFailedAttempt(attemptCounter)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error authenticating with password: ${e.message}", e)
            Toast.makeText(this, getString(R.string.authentication_error), Toast.LENGTH_SHORT).show()
        }
    }

    private fun authenticateWithBiometrics() {
        // sprawdzanie dostępności biometrii
        val biometricManager = BiometricManager.from(this)
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, "Biometric authentication is not available", Toast.LENGTH_LONG).show()
        }
        val executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                Toast.makeText(this@MainActivity, getString(R.string.logged_in), Toast.LENGTH_SHORT).show()
                anonymousAuth()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(this@MainActivity,
                    getString(R.string.error, errString), Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(this@MainActivity, getString(R.string.try_again), Toast.LENGTH_SHORT).show()
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_prompt))
            .setNegativeButtonText(getString(R.string.cancel))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        val cipher = EncryptionUtil.getInitializedCipherForEncryption()
        val cryptoObject = BiometricPrompt.CryptoObject(cipher)
        biometricPrompt.authenticate(promptInfo, cryptoObject)
    }

    fun performLogout(context: Context) {
        Firebase.auth.signOut()
    }

    private fun navigateToAccessActivity() {
        val intent = Intent(this, AccessActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun handleFailedAttempt(currentAttempt: Int) {
        val newAttemptCount = currentAttempt + 1
        sharedPrefs.edit().putInt("attemptCounter", newAttemptCount).apply()
        val attempt = 10 - newAttemptCount
        if (newAttemptCount >= 10) {
            resetPassword()
        } else {
            Toast.makeText(
                this,
                getString(R.string.wrong_password, attempt),
                Toast.LENGTH_SHORT
            ).show()
            passwordInput.text.clear()
        }
    }

    private fun resetPassword() {
        sharedPrefs.edit()
            .remove("passwordHash")
            .remove("iv")
            .remove("password_salt")
            .putInt("attemptCounter", 0)
            .apply()
        lifecycleScope.launch {
            try {
                noteDao = (application as SecureNotesApp).noteDatabase.noteDao()
                noteDao.deleteAllNotes()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error resetting password: ${e.message}")
            }
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.reset_dialog_tittle))
            .setMessage(getString(R.string.reset_dialog_text))
            .setPositiveButton("OK") { _, _ ->
                val intent = Intent(this, PasswordSetupActivity::class.java)
                startActivity(intent)
                finish()
            }
            .setCancelable(false)
            .show()
    }
}
