package com.silas.omaster.util

import android.content.Context
import com.silas.omaster.util.UrlConstants
import timber.log.Timber

object UpdateConfigManager {
    private const val PREFS_NAME = "omaster_update_prefs"
    private const val KEY_PRESET_URL = "preset_update_url"

    const val DEFAULT_PRESET_URL = UrlConstants.PRESET_OPPO

    fun getPresetUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PRESET_URL, DEFAULT_PRESET_URL) ?: DEFAULT_PRESET_URL
    }

    fun setPresetUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PRESET_URL, url).apply()
        Timber.d("Saved preset update URL: $url")
    }
}
