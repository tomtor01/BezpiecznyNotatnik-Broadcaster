package com.example.bezpiecznynotatnik.utils

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {

    fun setLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration()
        config.setLocale(locale)

        context.resources.configuration.setLocale(locale)
        context.createConfigurationContext(config)

        return context
    }
}
