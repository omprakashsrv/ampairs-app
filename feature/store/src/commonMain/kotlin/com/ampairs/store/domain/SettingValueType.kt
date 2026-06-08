package com.ampairs.store.domain

/**
 * How a setting's stored string value is interpreted. Mirrors the backend `SettingValueType`.
 */
enum class SettingValueType {
    BOOLEAN,
    INT,
    DECIMAL,
    STRING,
    ENUM,
    JSON;

    companion object {
        fun fromName(name: String?): SettingValueType =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: STRING
    }
}
