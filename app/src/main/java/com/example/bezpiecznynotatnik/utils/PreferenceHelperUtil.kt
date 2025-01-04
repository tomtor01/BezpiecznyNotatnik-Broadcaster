package com.example.bezpiecznynotatnik.utils

import android.content.Context
import android.content.SharedPreferences

object PreferenceHelper {
    private lateinit var prefs: SharedPreferences
    private const val KEY_LANGUAGE = "key_language"

    fun setLanguage(context: Context, languageCode: String) {
        prefs = context.getSharedPreferences("SecureNotesPrefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_LANGUAGE, languageCode)
            .apply()
    }

    fun getLanguage(context: Context?): String? {
        if (context == null) return null
        prefs = context.getSharedPreferences("SecureNotesPrefs", Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, null)
    }
}
