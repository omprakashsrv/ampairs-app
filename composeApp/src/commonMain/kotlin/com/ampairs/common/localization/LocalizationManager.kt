package com.ampairs.common.localization

import com.ampairs.common.config.AppPreferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Manages language preferences and provides localized strings
 */
class LocalizationManager(
    private val appPreferences: AppPreferencesDataStore
) {
    /**
     * Current selected language as a Flow
     */
    val currentLanguage: Flow<Language> = appPreferences.getLanguagePreference().map { languageCode ->
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
        appPreferences.setLanguagePreference(language.code)
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
