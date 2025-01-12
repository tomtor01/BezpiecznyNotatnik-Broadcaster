package com.example.bezpiecznynotatnik

import com.example.bezpiecznynotatnik.utils.LocaleHelper
import com.example.bezpiecznynotatnik.utils.PreferenceHelper
import com.example.bezpiecznynotatnik.data.AppDatabase
import com.example.bezpiecznynotatnik.utils.LogoutWorker

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.bezpiecznynotatnik.data.GoogleDriveBackupManager

import java.util.concurrent.TimeUnit

class SecureNotesApp : Application() {

    lateinit var noteDatabase: AppDatabase
    lateinit var googleDriveManager: GoogleDriveBackupManager

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
    }
    override fun onCreate() {
        super.onCreate()

        // Initialize the database as a singleton
        noteDatabase = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "notes_db"
        ).build()

        googleDriveManager = GoogleDriveBackupManager().apply {
            initializeGoogleSignIn(this@SecureNotesApp)
        }

        val language = PreferenceHelper.getLanguage(this) ?: "default"
        LocaleHelper.setLocale(this, language)
    }

    fun scheduleLogoutWork() {
        Log.d("SecureNotesApp", "Scheduling logout...")
        val logoutWorkRequest = OneTimeWorkRequestBuilder<LogoutWorker>()
            .setInitialDelay(1, TimeUnit.MINUTES)
            .addTag("logout_work")
            .build()

        WorkManager.getInstance(this).enqueue(logoutWorkRequest)
    }

    fun cancelLogoutWork() {
        Log.d("SecureNotesApp", "Cancelling scheduled logout.")
        WorkManager.getInstance(this).cancelAllWorkByTag("logout_work")
    }
}
