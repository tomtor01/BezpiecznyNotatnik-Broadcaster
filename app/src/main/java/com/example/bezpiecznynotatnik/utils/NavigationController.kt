package com.example.bezpiecznynotatnik.utils

import com.example.bezpiecznynotatnik.activities.MainActivity
import com.example.bezpiecznynotatnik.R

import com.google.android.material.bottomnavigation.BottomNavigationView
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavOptions
import androidx.navigation.findNavController

abstract class NavigationController : AppCompatActivity() {

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
                handleLogout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun handleLogout() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    fun setupNavigationView() {
        val navigationView = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        navigationView.setOnItemSelectedListener { menuItem ->
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
}


//        if (navigationBar == null) {
//            Log.e("NavController", "BottomNavigationView not found in the layout!")
//            return
//        }
