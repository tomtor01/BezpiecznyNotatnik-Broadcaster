package com.example.bezpiecznynotatnik

import com.example.bezpiecznynotatnik.utils.LocaleHelper
import com.example.bezpiecznynotatnik.utils.PreferenceHelper
import com.example.bezpiecznynotatnik.activities.MainActivity

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.room.Room
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SecureNotesApp : Application(), Application.ActivityLifecycleCallbacks {

    lateinit var noteDatabase: AppDatabase
    private var handler: Handler? = null
    private var logoutRunnable: Runnable? = null
    private var currentActivity: AppCompatActivity? = null
    var isUserLoggedIn = false

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
    }
    override fun onCreate() {
        super.onCreate()

        registerActivityLifecycleCallbacks(this)
        setupLogoutTimer()

        // Initialize the database as a singleton
        noteDatabase = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "notes_db"
        ).build()

        val language = PreferenceHelper.getLanguage(this) ?: "default"
        LocaleHelper.setLocale(this, language)
    }

    private fun setupLogoutTimer() {
        handler = Handler(Looper.getMainLooper())
        logoutRunnable = Runnable {
            performLogout()
        }
    }

    fun resetLogoutTimer() {
        if (isUserLoggedIn) {
            handler?.removeCallbacks(logoutRunnable!!)
            handler?.postDelayed(logoutRunnable!!, 2 * 60 * 1000) // 2 minutes timeout
        }
    }

    private fun performLogout() {

        val activity = currentActivity
        isUserLoggedIn = false
        if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        if (activity is AppCompatActivity) {
            currentActivity = activity
            resetLogoutTimer()
        }
    }

    override fun onActivityPaused(activity: Activity) {
        if (currentActivity === activity) {
            handler?.removeCallbacks(logoutRunnable!!)
            currentActivity = null
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
