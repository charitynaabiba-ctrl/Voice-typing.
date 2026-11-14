package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dictationDataStore: DataStore<Preferences> by preferencesDataStore(name = "voxtype_preferences")

data class SpeechLanguageOption(
  val code: String,
  val displayName: String,
  val nativeHint: String,
)

val SUPPORTED_LANGUAGES =
  listOf(
    SpeechLanguageOption("auto", "Auto-Detect", "85+ languages & code-switching"),
    SpeechLanguageOption("en-US", "English (US)", "American English"),
    SpeechLanguageOption("en-GB", "English (UK)", "British English"),
    SpeechLanguageOption("es", "Español", "Spanish"),
    SpeechLanguageOption("fr", "Français", "French"),
    SpeechLanguageOption("de", "Deutsch", "German"),
    SpeechLanguageOption("pt", "Português", "Portuguese"),
    SpeechLanguageOption("hi", "हिन्दी", "Hindi"),
    SpeechLanguageOption("ja", "日本語", "Japanese"),
  )

data class DictationPreferences(
  val languageCode: String = "auto",
  val autoCopyOnStop: Boolean = false,
  val hapticFeedback: Boolean = true,
  val keepScreenOn: Boolean = true,
) {
  val selectedLanguage: SpeechLanguageOption
    get() = SUPPORTED_LANGUAGES.firstOrNull { it.code == languageCode } ?: SUPPORTED_LANGUAGES.first()
}

class DictationPreferencesRepository(private val context: Context) {

  private object Keys {
    val LANGUAGE_CODE = stringPreferencesKey("language_code")
    val AUTO_COPY_ON_STOP = booleanPreferencesKey("auto_copy_on_stop")
    val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
    val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
  }

  val preferences: Flow<DictationPreferences> =
    context.dictationDataStore.data.map { prefs ->
      DictationPreferences(
        languageCode = prefs[Keys.LANGUAGE_CODE] ?: "auto",
        autoCopyOnStop = prefs[Keys.AUTO_COPY_ON_STOP] ?: false,
        hapticFeedback = prefs[Keys.HAPTIC_FEEDBACK] ?: true,
        keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: true,
      )
    }

  suspend fun setLanguageCode(code: String) {
    context.dictationDataStore.edit { it[Keys.LANGUAGE_CODE] = code }
  }

  suspend fun setAutoCopyOnStop(enabled: Boolean) {
    context.dictationDataStore.edit { it[Keys.AUTO_COPY_ON_STOP] = enabled }
  }

  suspend fun setHapticFeedback(enabled: Boolean) {
    context.dictationDataStore.edit { it[Keys.HAPTIC_FEEDBACK] = enabled }
  }

  suspend fun setKeepScreenOn(enabled: Boolean) {
    context.dictationDataStore.edit { it[Keys.KEEP_SCREEN_ON] = enabled }
  }
}
