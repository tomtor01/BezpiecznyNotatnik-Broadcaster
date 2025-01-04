package com.example.bezpiecznynotatnik

import com.example.bezpiecznynotatnik.utils.LocaleHelper
import com.example.bezpiecznynotatnik.utils.PreferenceHelper

import android.app.Application
import android.content.Context
import androidx.room.Room

class SecureNotesApp : Application() {

    lateinit var noteDatabase: AppDatabase

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

        val language = PreferenceHelper.getLanguage(this) ?: "default"
        LocaleHelper.setLocale(this, language)
    }
}
