package com.ampairs.common.localization

import kotlinx.serialization.Serializable

/**
 * Supported languages in the application
 */
@Serializable
enum class Language(
    val code: String,
    val displayName: String,
    val nativeName: String
) {
    ENGLISH("en", "English", "English"),
    HINDI("hi", "Hindi", "हिंदी");

    companion object {
        fun fromCode(code: String): Language {
            return entries.find { it.code == code } ?: ENGLISH
        }
    }
}
