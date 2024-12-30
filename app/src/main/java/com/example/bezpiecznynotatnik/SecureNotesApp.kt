package com.example.bezpiecznynotatnik

import android.app.Application
import androidx.room.Room

class SecureNotesApp : Application() {

    lateinit var noteDatabase: AppDatabase

    override fun onCreate() {
        super.onCreate()

        // Initialize the database as a singleton
        noteDatabase = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "notes_db"
        ).build()
    }
}