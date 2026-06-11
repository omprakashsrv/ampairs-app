package com.ampairs.form.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Where a field's value is stored. Mirrors the backend `FieldSource`. */
@Serializable
enum class FieldSource {
    @SerialName("standard") STANDARD,
    @SerialName("custom") CUSTOM,
}

/** Field input data type. Mirrors the backend `FieldDataType` (incl. CHOICE / MULTI_CHOICE / CUSTOM). */
@Serializable
enum class FieldDataType {
    @SerialName("text") TEXT,
    @SerialName("textarea") TEXTAREA,
    @SerialName("number") NUMBER,
    @SerialName("boolean") BOOLEAN,
    @SerialName("date") DATE,
    @SerialName("choice") CHOICE,
    @SerialName("multi_choice") MULTI_CHOICE,
    @SerialName("custom") CUSTOM;

    val isChoice: Boolean get() = this == CHOICE || this == MULTI_CHOICE
}

/** Option origin for CHOICE / MULTI_CHOICE fields. Mirrors the backend `OptionSource`. */
@Serializable
enum class OptionSource {
    @SerialName("static") STATIC,
    @SerialName("dynamic") DYNAMIC,
}
