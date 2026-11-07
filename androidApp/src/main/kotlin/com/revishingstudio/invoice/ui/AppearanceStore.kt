package com.revishingstudio.invoice.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/** Remembers the Light / Dark / System choice on this phone. It is not part of the backup. */
class AppearanceStore(context: Context) {
    private val prefs = context.getSharedPreferences("ui", Context.MODE_PRIVATE)

    var appearance: Appearance by mutableStateOf(
        prefs.getString(KEY, null)?.let { name -> Appearance.entries.firstOrNull { it.name == name } } ?: Appearance.SYSTEM,
    )
        private set

    fun set(value: Appearance) {
        appearance = value
        prefs.edit().putString(KEY, value.name).apply()
    }

    private companion object {
        const val KEY = "appearance"
    }
}

val LocalAppearanceStore = staticCompositionLocalOf<AppearanceStore> { error("AppearanceStore not provided") }
