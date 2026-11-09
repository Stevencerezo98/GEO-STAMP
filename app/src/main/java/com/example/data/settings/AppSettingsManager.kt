package com.example.data.settings

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import com.example.util.StampBgStyle
import com.example.util.StampPosition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode(val key: String, val title: String, val description: String) {
    SYSTEM("system", "Automático", "Sigue la preferencia del sistema operativo"),
    LIGHT("light", "Siempre Claro", "Colores nítidos y fondo blanco luminoso"),
    DARK("dark", "Siempre Oscuro", "Fondo oscuro de alto contraste y menor consumo de batería");

    companion object {
        fun fromKey(key: String?): AppThemeMode {
            return entries.find { it.key.equals(key, ignoreCase = true) } ?: SYSTEM
        }
    }
}

class AppSettingsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Theme
    private val _themeMode = MutableStateFlow(getStoredThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    // Stamp Elements Switches (Directly matching user requirements)
    private val _includeDate = MutableStateFlow(prefs.getBoolean(KEY_INCLUDE_DATE, true))
    val includeDate: StateFlow<Boolean> = _includeDate.asStateFlow()

    private val _includeTime = MutableStateFlow(prefs.getBoolean(KEY_INCLUDE_TIME, true))
    val includeTime: StateFlow<Boolean> = _includeTime.asStateFlow()

    private val _includeGps = MutableStateFlow(prefs.getBoolean(KEY_INCLUDE_GPS, true))
    val includeGps: StateFlow<Boolean> = _includeGps.asStateFlow()

    private val _includeAddress = MutableStateFlow(prefs.getBoolean(KEY_INCLUDE_ADDRESS, true))
    val includeAddress: StateFlow<Boolean> = _includeAddress.asStateFlow()

    private val _includeCustomNotes = MutableStateFlow(prefs.getBoolean(KEY_INCLUDE_CUSTOM_NOTES, true))
    val includeCustomNotes: StateFlow<Boolean> = _includeCustomNotes.asStateFlow()

    private val _customNoteText = MutableStateFlow(prefs.getString(KEY_CUSTOM_NOTE_TEXT, "") ?: "")
    val customNoteText: StateFlow<String> = _customNoteText.asStateFlow()

    private val _stampPosition = MutableStateFlow(
        StampPosition.entries.find { it.name == prefs.getString(KEY_STAMP_POSITION, StampPosition.BOTTOM_RIGHT.name) } ?: StampPosition.BOTTOM_RIGHT
    )
    val stampPosition: StateFlow<StampPosition> = _stampPosition.asStateFlow()

    private val _stampTextColor = MutableStateFlow(prefs.getInt(KEY_STAMP_TEXT_COLOR, Color.WHITE))
    val stampTextColor: StateFlow<Int> = _stampTextColor.asStateFlow()

    private val _stampBgStyle = MutableStateFlow(
        StampBgStyle.entries.find { it.name == prefs.getString(KEY_STAMP_BG_STYLE, StampBgStyle.GLASS_CARD.name) } ?: StampBgStyle.GLASS_CARD
    )
    val stampBgStyle: StateFlow<StampBgStyle> = _stampBgStyle.asStateFlow()

    private fun getStoredThemeMode(): AppThemeMode {
        val storedKey = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.key)
        return AppThemeMode.fromKey(storedKey)
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.key).apply()
        _themeMode.value = mode
    }

    fun setIncludeDate(value: Boolean) {
        prefs.edit().putBoolean(KEY_INCLUDE_DATE, value).apply()
        _includeDate.value = value
    }

    fun setIncludeTime(value: Boolean) {
        prefs.edit().putBoolean(KEY_INCLUDE_TIME, value).apply()
        _includeTime.value = value
    }

    fun setIncludeGps(value: Boolean) {
        prefs.edit().putBoolean(KEY_INCLUDE_GPS, value).apply()
        _includeGps.value = value
    }

    fun setIncludeAddress(value: Boolean) {
        prefs.edit().putBoolean(KEY_INCLUDE_ADDRESS, value).apply()
        _includeAddress.value = value
    }

    fun setIncludeCustomNotes(value: Boolean) {
        prefs.edit().putBoolean(KEY_INCLUDE_CUSTOM_NOTES, value).apply()
        _includeCustomNotes.value = value
    }

    fun setCustomNoteText(text: String) {
        prefs.edit().putString(KEY_CUSTOM_NOTE_TEXT, text).apply()
        _customNoteText.value = text
    }

    fun setStampPosition(pos: StampPosition) {
        prefs.edit().putString(KEY_STAMP_POSITION, pos.name).apply()
        _stampPosition.value = pos
    }

    fun setStampTextColor(color: Int) {
        prefs.edit().putInt(KEY_STAMP_TEXT_COLOR, color).apply()
        _stampTextColor.value = color
    }

    fun setStampBgStyle(style: StampBgStyle) {
        prefs.edit().putString(KEY_STAMP_BG_STYLE, style.name).apply()
        _stampBgStyle.value = style
    }

    companion object {
        private const val PREFS_NAME = "geostamp_settings_prefs"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_INCLUDE_DATE = "include_date"
        private const val KEY_INCLUDE_TIME = "include_time"
        private const val KEY_INCLUDE_GPS = "include_gps"
        private const val KEY_INCLUDE_ADDRESS = "include_address"
        private const val KEY_INCLUDE_CUSTOM_NOTES = "include_custom_notes"
        private const val KEY_CUSTOM_NOTE_TEXT = "custom_note_text"
        private const val KEY_STAMP_POSITION = "stamp_position"
        private const val KEY_STAMP_TEXT_COLOR = "stamp_text_color"
        private const val KEY_STAMP_BG_STYLE = "stamp_bg_style"

        @Volatile
        private var INSTANCE: AppSettingsManager? = null

        fun getInstance(context: Context): AppSettingsManager {
            return INSTANCE ?: synchronized(this) {
                val instance = AppSettingsManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
