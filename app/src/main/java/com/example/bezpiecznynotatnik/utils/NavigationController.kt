package com.example.bezpiecznynotatnik.utils

import com.example.bezpiecznynotatnik.activities.MainActivity
import com.example.bezpiecznynotatnik.R

import com.google.android.material.bottomnavigation.BottomNavigationView

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavOptions
import androidx.navigation.findNavController

abstract class NavigationController : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupNavigationBar()
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.action_bar, menu)

        val logoutItem = menu.findItem(R.id.logoutButton)
        val accountItem = menu.findItem(R.id.accountButton)

        logoutItem.icon?.setTintList(
            ContextCompat.getColorStateList(this, R.color.md_theme_primary)
        )
        accountItem.icon?.setTintList(
            ContextCompat.getColorStateList(this, R.color.md_theme_primary)
        )

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.logoutButton -> {
                handleLogout()
                true
            }
            R.id.accountButton -> {
                openAccountSettings()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun handleLogout() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun openAccountSettings() {
        showAccountFragment()
    }

    private fun setupNavigationBar() {
        val navigationBar = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        if (navigationBar == null) {
            Log.e("NavController", "BottomNavigationView not found in the layout!")
            return
        }

        navigationBar.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_notesView -> {
                    findNavController(R.id.nav_host_fragment).navigate(
                        R.id.nav_notesView,
                        null,
                        NavOptions.Builder()
                            .setPopUpTo(R.id.nav_notesView, inclusive = true)
                            .build()
                    )
                    true
                }
                R.id.nav_create -> {
                    findNavController(R.id.nav_host_fragment).navigate(R.id.nav_create)
                    true
                }
                R.id.nav_settings -> {
                    findNavController(R.id.nav_host_fragment).navigate(R.id.nav_settings)
                    true
                }
                else -> false
            }
        }
    }
    abstract fun showAccountFragment()
}
