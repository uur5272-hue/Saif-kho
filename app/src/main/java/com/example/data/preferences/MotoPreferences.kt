package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

class MotoPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("moto_ai_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CUSTOM_API_KEY = "custom_gemini_api_key"
        private const val KEY_LANGUAGE = "selected_language"
        private const val KEY_VOICE_RATE = "speech_rate"
        private const val KEY_VOICE_PITCH = "speech_pitch"
        private const val KEY_VOICE_ENABLED = "voice_responses_enabled"
        private const val KEY_WAKE_WORD_ENABLED = "wake_word_enabled"
        private const val KEY_FLOATING_BUTTON_ENABLED = "floating_button_enabled"
        private const val KEY_DEFAULT_VIDEO_RES = "default_video_resolution"
        private const val KEY_AUTO_SAVE_GALLERY = "auto_save_gallery"
    }

    var customApiKey: String
        get() = prefs.getString(KEY_CUSTOM_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_API_KEY, value.trim()).apply()

    fun getEffectiveApiKey(): String {
        val custom = customApiKey
        if (custom.isNotBlank()) return custom
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key == "MY_GEMINI_API_KEY" || key.isBlank()) "" else key
        } catch (e: Exception) {
            ""
        }
    }

    var selectedLanguage: String
        get() = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    var speechRate: Float
        get() = prefs.getFloat(KEY_VOICE_RATE, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_VOICE_RATE, value).apply()

    var speechPitch: Float
        get() = prefs.getFloat(KEY_VOICE_PITCH, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_VOICE_PITCH, value).apply()

    var isVoiceEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOICE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VOICE_ENABLED, value).apply()

    var isWakeWordEnabled: Boolean
        get() = prefs.getBoolean(KEY_WAKE_WORD_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_WAKE_WORD_ENABLED, value).apply()

    var isFloatingButtonEnabled: Boolean
        get() = prefs.getBoolean(KEY_FLOATING_BUTTON_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_FLOATING_BUTTON_ENABLED, value).apply()

    var defaultVideoResolution: String
        get() = prefs.getString(KEY_DEFAULT_VIDEO_RES, "720p") ?: "720p"
        set(value) = prefs.edit().putString(KEY_DEFAULT_VIDEO_RES, value).apply()

    var autoSaveToGallery: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SAVE_GALLERY, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SAVE_GALLERY, value).apply()
}
