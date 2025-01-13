package com.example.bezpiecznynotatnik.activities

import com.example.bezpiecznynotatnik.SecureNotesApp
import com.example.bezpiecznynotatnik.R
import com.example.bezpiecznynotatnik.utils.*
import com.example.bezpiecznynotatnik.utils.ByteArrayUtil.fromBase64
import com.example.bezpiecznynotatnik.data.NoteDao
import com.example.bezpiecznynotatnik.data.GoogleDriveBackupManager
import com.example.bezpiecznynotatnik.data.UserState

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.text.SpannableString
import android.text.SpannedString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var passwordInput: EditText
    private lateinit var loginWithPasswordButton: Button
    private lateinit var loginWithBiometricsButton: ImageButton
    private lateinit var forgotPassword: TextView
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var noteDao: NoteDao
    private lateinit var auth: FirebaseAuth
    private lateinit var googleDriveManager: GoogleDriveBackupManager
    private var isAuthenticatedWithBiometric: Boolean = false
    private var isAuthenticatedWithPassword: Boolean = false

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
                //TODO("Not yet implemented")
            }
        }
        spanString.setSpan(forgotPasswordText, 0, spanString.length, SpannedString.SPAN_EXCLUSIVE_EXCLUSIVE)
        forgotPassword.text = spanString
        forgotPassword.movementMethod = LinkMovementMethod.getInstance()

        val saltBase64 = sharedPrefs.getString("password_salt", null)
        val passwordHashBase64 = sharedPrefs.getString("passwordHash", null)

        if (saltBase64 == null || passwordHashBase64 == null) {
            Toast.makeText(this, getString(R.string.set_password), Toast.LENGTH_SHORT).show()
            resetPassword()
            clearNotes()
            redirectToPasswordSetup()
            return
        }
        googleDriveManager = (application as SecureNotesApp).googleDriveManager
        googleDriveManager.initializeGoogleSignIn(applicationContext)

        loginWithPasswordButton.setOnClickListener {
            authenticateWithPassword(object : PasswordAuthenticationListener {
                override fun onPasswordAuthenticationSuccess() {
                    isAuthenticatedWithPassword = true
                    anonymousAuth()
                }
                override fun onPasswordAuthenticationFailure() {}
                override fun onPasswordAuthenticationException() {
                    Toast.makeText(this@MainActivity,
                        getString(R.string.authentication_error),
                        Toast.LENGTH_SHORT
                    ).show()
                    redirectToPasswordSetup()
                    return
                }
            })
        }
        loginWithBiometricsButton.setOnClickListener {
            authenticateWithBiometrics()
        }
    }

    private fun anonymousAuth() {
        if (!(isAuthenticatedWithBiometric || isAuthenticatedWithPassword)) {
            Toast.makeText(this, "Authentication required", Toast.LENGTH_SHORT).show()
            return
        }
        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.isAnonymous) {
            // Reuse existing anonymous user
            Log.d("MainActivity", "Reusing existing anonymous user: ${currentUser.uid}")
            navigateToAccessActivity()
            googleDriveManager.silentSignIn(applicationContext,
                onSuccess = {
                    UserState.isUserSignedIn = true
                    val userName = googleDriveManager.getSignedInUserName(applicationContext)
                    Toast.makeText(this@MainActivity,
                        getString(R.string.welcome_user, userName), Toast.LENGTH_SHORT).show()
                },
                onFailure = { errorMessage ->
                    UserState.isUserSignedIn = false
                    Log.w("MainActivity", "Silent sign-in failed: $errorMessage")
                })
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
                    Toast.makeText(this, getString(R.string.authentication_error), Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun authenticateWithPassword(listener: PasswordAuthenticationListener) {
        val enteredPassword = passwordInput.text.toString()
        if (enteredPassword.isEmpty()) {
            return
        }
        try {
            val attemptCounter = sharedPrefs.getInt("attemptCounter", 0)
            val saltBase64 = sharedPrefs.getString("password_salt", null)
            val passwordHashBase64 = sharedPrefs.getString("passwordHash", null)

            val salt = fromBase64(saltBase64.toString())
            val hashedPassword = HashUtil.hashPassword(enteredPassword, salt)
            val storedPasswordHash = fromBase64(passwordHashBase64.toString())

            if (hashedPassword.contentEquals(storedPasswordHash)) {
                listener.onPasswordAuthenticationSuccess()
                Log.d("MainActivity", "Password matches stored hash.")
                sharedPrefs.edit().putInt("attemptCounter", 0)
                    .apply() // Reset the counter on success
            } else {
                listener.onPasswordAuthenticationFailure()
                handleFailedAttempt(attemptCounter)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error authenticating with password: ${e.message}", e)
            listener.onPasswordAuthenticationException()
        }
    }

    private fun authenticateWithBiometrics() {
        val biometricManager = BiometricManager.from(this)
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, "Biometric authentication is not available", Toast.LENGTH_LONG).show()
            return
        }
        val executor = ContextCompat.getMainExecutor(this)
        val cipher = EncryptionUtil.getInitializedCipherForEncryption()
        val cryptoObject = BiometricPrompt.CryptoObject(cipher)

        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                try {
                    val biometricCipher = result.cryptoObject?.cipher
                    if (biometricCipher == null) {
                        Log.e("MainActivity", "Cipher is null.")
                        return
                    }
                    Log.d("MainActivity", "Biometric authentication succeeded!")
                    isAuthenticatedWithBiometric = true
                    anonymousAuth()
                } catch (e: Exception) {
                    if (e is KeyPermanentlyInvalidatedException) {
                        Log.e("MainActivity", "Biometric enrollment changed. Prompting for password.")
                        promptPasswordAuthenticationToRegenerateKey()
                    } else {
                        Log.e("MainActivity", "Authentication error: ${e.message}")
                    }
                }
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Toast.makeText(this@MainActivity, "$errString", Toast.LENGTH_SHORT).show()
            }
            override fun onAuthenticationFailed() {
                Toast.makeText(this@MainActivity, getString(R.string.try_again), Toast.LENGTH_SHORT).show()
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_prompt))
            .setNegativeButtonText(getString(R.string.cancel))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo, cryptoObject)
    }

    private fun promptPasswordAuthenticationToRegenerateKey() {
        authenticateWithPassword(object : PasswordAuthenticationListener {
            override fun onPasswordAuthenticationSuccess() {
                Log.d("MainActivity", "Password authentication succeeded. Regenerating key.")
                EncryptionUtil.getOrCreateSecretKey()
                Toast.makeText(this@MainActivity, "Key regenerated. Biometric authentication restored.", Toast.LENGTH_SHORT).show()
            }

            override fun onPasswordAuthenticationFailure() {
                Log.d("MainActivity", "Password authentication failed.")
            }

            override fun onPasswordAuthenticationException() {
                Log.d("MainActivity", "An error occurred during password authentication.")
            }
        })
    }

    private fun navigateToAccessActivity() {
        startActivity(Intent(this, AccessActivity::class.java))
        finish()
    }

    private fun redirectToPasswordSetup() {
        startActivity(Intent(this, PasswordSetupActivity::class.java))
        finish()
    }

    private fun handleFailedAttempt(currentAttempt: Int) {
        val newAttemptCount = currentAttempt + 1
        sharedPrefs.edit().putInt("attemptCounter", newAttemptCount).apply()
        val attempt = 10 - newAttemptCount
        if (newAttemptCount >= 10) {
            showResetPasswordDialog()
            return
        } else {
            Toast.makeText(
                this,
                getString(R.string.wrong_password, attempt),
                Toast.LENGTH_SHORT
            ).show()
            passwordInput.text.clear()
        }
    }
    private fun clearNotes() {
        lifecycleScope.launch {
            try {
                noteDao = (application as SecureNotesApp).noteDatabase.noteDao()
                noteDao.deleteAllNotes()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error resetting notes: ${e.message}")
            }
        }
    }

    private fun resetPassword() {
        sharedPrefs.edit()
            .remove("passwordHash")
            .remove("fingerprint_hash")
            .remove("password_salt")
            .putInt("attemptCounter", 0)
            .apply()
        clearNotes()
    }

    private fun showResetPasswordDialog() {
        resetPassword()
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.reset_dialog_tittle))
            .setMessage(getString(R.string.reset_dialog_text))
            .setPositiveButton("OK") { _, _ ->
                startActivity(Intent(this, PasswordSetupActivity::class.java))
                finish()
            }
            .setCancelable(false)
            .show()
    }

    interface PasswordAuthenticationListener {
        fun onPasswordAuthenticationSuccess()
        fun onPasswordAuthenticationFailure()
        fun onPasswordAuthenticationException()
    }
}
