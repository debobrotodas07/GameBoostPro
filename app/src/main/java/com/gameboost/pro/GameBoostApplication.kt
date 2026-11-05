package com.gameboost.pro

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GameBoostApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("advantage")
        instance = this
        initializePreferences()
    }

    companion object {
        @Suppress("UNUSED_PROPERTY")
        var instance: GameBoostApplication? = null
            private set

        val dataStore by lazy { preferencesDataStore("gb_settings") }
        private var prefs: SharedPreferences? = null

        fun getPrefs(): SharedPreferences = prefs ?: instance?.getSharedPreferences("gb_prefs", Context.MODE_PRIVATE)!!
            also { prefs = it }

        fun isRooted(): Boolean {
            return try {
                Runtime.getRuntime().exec("su -c id").waitFor() == 0
            } catch (e: Exception) {
                false
            }
    }

    private fun initializePreferences() {
        CoroutineScope(Dispatchers.IO).launch {
            val defaultPrefs = getPrefs()
            if (!defaultPrefs.contains("auto_patch")) {
                defaultPrefs.edit().putBoolean("auto_patch", false).apply()
            }
            if (!defaultPrefs.contains("start_on_boot")) {
                defaultPrefs.edit().putBoolean("start_on_boot", true).apply()
            }
            if (!defaultPrefs.contains("auto_detect_games")) {
                defaultPrefs.edit().putBoolean("auto_detect_games", true).apply()
            }
            if (!defaultPrefs.contains("show_floating_sidebar")) {
                defaultPrefs.edit().putBoolean("show_floating_sidebar", true).apply()
            }
            if (!defaultPrefs.contains("root_mode_override")) {
                defaultPrefs.edit().putBoolean("root_mode_override", false).apply()
            }
        }
    }
}