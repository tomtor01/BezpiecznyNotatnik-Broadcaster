package com.example.bezpiecznynotatnik.activities

import com.example.bezpiecznynotatnik.R
import com.example.bezpiecznynotatnik.SecureNotesApp
import com.example.bezpiecznynotatnik.utils.LocaleHelper
import com.example.bezpiecznynotatnik.utils.NavigationController
import com.example.bezpiecznynotatnik.utils.PreferenceHelper

import com.google.android.material.bottomnavigation.BottomNavigationView

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.bezpiecznynotatnik.fragments.AccountFragment

import java.util.Locale

class AccessActivity : NavigationController() {

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
        setContentView(R.layout.activity_access)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Link BottomNavigationView with NavController
        bottomNavigationView.setupWithNavController(navController)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val accountFragmentContainer = findViewById<View>(R.id.account_fragment_container)
                val bottomNavView = findViewById<BottomNavigationView>(R.id.bottomNavigation)

                if (supportFragmentManager.backStackEntryCount > 0) {
                    // Handle fragment back navigation
                    supportFragmentManager.popBackStack()
                    bottomNavView.visibility = View.VISIBLE // Restore BottomNavigationView
                    findViewById<View>(R.id.nav_host_fragment).visibility = View.VISIBLE // Restore nav_host_fragment
                    accountFragmentContainer.visibility = View.GONE // Hide account_fragment_container
                } else {
                    // Default behavior
                    finish()
                }
            }
        })
    }

    override fun showAccountFragment() {
        // Hide BottomNavigationView
        findViewById<BottomNavigationView>(R.id.bottomNavigation).visibility = View.GONE
        findViewById<View>(R.id.nav_host_fragment).visibility = View.GONE

        // Show AccountFragment
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.account_fragment_container, AccountFragment())
        transaction.addToBackStack(null) // Add to backstack to enable proper back navigation
        transaction.commit()

        // Make the container visible
        findViewById<View>(R.id.account_fragment_container).visibility = View.VISIBLE
    }

    override fun onStop() {
        super.onStop()
        (application as SecureNotesApp).scheduleLogoutWork()
    }
    override fun onStart() {
        super.onStart()
        (application as SecureNotesApp).cancelLogoutWork()
    }
    override fun onRestart() {
        super.onRestart()
        (application as SecureNotesApp).cancelLogoutWork()
    }
    override fun onResume() {
        super.onResume()
        (application as SecureNotesApp).cancelLogoutWork()
    }
    override fun onDestroy() {
        super.onDestroy()
        (application as SecureNotesApp).cancelLogoutWork()
    }
    override fun onPause() {
        super.onPause()
        (application as SecureNotesApp).cancelLogoutWork()
    }
}
