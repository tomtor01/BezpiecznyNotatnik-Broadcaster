package com.example.bezpiecznynotatnik.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.bezpiecznynotatnik.R
import com.example.bezpiecznynotatnik.data.GoogleDriveBackupManager
import com.example.bezpiecznynotatnik.utils.LocaleHelper
import com.example.bezpiecznynotatnik.utils.PreferenceHelper
import kotlinx.coroutines.launch
import java.util.Locale

class AccountActivity : AppCompatActivity() {

    private lateinit var btnSignIn: Button
    private lateinit var btnExport: Button
    private lateinit var btnImport: Button
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
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_account)

        btnSignIn = findViewById(R.id.signUpButton)
        btnExport = findViewById(R.id.btnExport)
        btnImport = findViewById(R.id.btnImport)

        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                startActivity(Intent(this@AccountActivity, AccessActivity::class.java))
            }
        }
        googleDriveManager = GoogleDriveBackupManager()
        googleDriveManager.initializeGoogleSignIn()
        setupUI()
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.action_bar, menu)

        val logoutItem = menu.findItem(R.id.logoutButton)

        logoutItem.icon?.setTintList(
            ContextCompat.getColorStateList(this, R.color.md_theme_primary)
        )
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.logoutButton -> {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupUI() {
        // Set up Sign-In button
        btnSignIn.setOnClickListener {
            val signInIntent = googleDriveManager.getSignInIntent()
            signInLauncher.launch(signInIntent)
        }

        // Set up Export button
        btnExport.setOnClickListener {
            lifecycleScope.launch {
                if (!googleDriveManager.isDriveServiceInitialized()) {
                    Toast.makeText(this@AccountActivity, "Please sign in first", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                try {
                    googleDriveManager.uploadDatabase()
                    Toast.makeText(this@AccountActivity, "Database uploaded successfully!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@AccountActivity, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Set up Import button
        btnImport.setOnClickListener {
            lifecycleScope.launch {
                if (!googleDriveManager.isDriveServiceInitialized()) {
                    Toast.makeText(this@AccountActivity, "Please sign in first", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                try {
                    googleDriveManager.downloadDatabase()
                    Toast.makeText(this@AccountActivity, "Database restored successfully!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@AccountActivity, "Restore failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Handle sign-in result
    private val signInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                googleDriveManager.handleSignInResult(
                    data = result.data,
                    onSuccess = {
                        Toast.makeText(this, "Sign-In successful!", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { errorMessage ->
                        Toast.makeText(this, "Sign-In failed: $errorMessage", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                Toast.makeText(this, "Sign-In canceled or failed.", Toast.LENGTH_SHORT).show()
            }
        }
}