package com.ampairs.common.localization

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Manages language preferences and provides localized strings
 */
class LocalizationManager(
    private val dataStore: DataStore<Preferences>
) {
    private val languageKey = stringPreferencesKey("app_language")

    /**
     * Current selected language as a Flow
     */
    val currentLanguage: Flow<Language> = dataStore.data.map { preferences ->
        val languageCode = preferences[languageKey] ?: Language.ENGLISH.code
        Language.fromCode(languageCode)
    }

    /**
     * Current strings based on selected language
     */
    val strings: Flow<Strings> = currentLanguage.map { language ->
        getStringsForLanguage(language)
    }

    /**
     * Set the app language
     */
    suspend fun setLanguage(language: Language) {
        dataStore.edit { preferences ->
            preferences[languageKey] = language.code
        }
    }

    /**
     * Get strings for a specific language
     */
    fun getStringsForLanguage(language: Language): Strings {
        return when (language) {
            Language.ENGLISH -> EnglishStrings
            Language.HINDI -> HindiStrings
        }
    }

    companion object {
        /**
         * Get default language (English)
         */
        fun getDefaultLanguage(): Language = Language.ENGLISH

        /**
         * Get default strings (English)
         */
        fun getDefaultStrings(): Strings = EnglishStrings
    }
}
