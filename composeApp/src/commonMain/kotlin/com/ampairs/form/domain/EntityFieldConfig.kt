package com.ampairs.form.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Generic field configuration for standard entity fields
 * Controls visibility, validation, and behavior of predefined fields
 */
@Serializable
data class EntityFieldConfig(
    val uid: String,
    @SerialName("entity_type")
    val entityType: String,
    @SerialName("field_name")
    val fieldName: String,
    @SerialName("display_name")
    val displayName: String,
    val visible: Boolean = true,
    val mandatory: Boolean = false,
    val enabled: Boolean = true,
    @SerialName("display_order")
    val displayOrder: Int = 0,
    @SerialName("validation_type")
    val validationType: String? = null,
    @SerialName("validation_params")
    val validationParams: Map<String, String>? = null,
    val placeholder: String? = null,
    @SerialName("help_text")
    val helpText: String? = null,
    @SerialName("default_value")
    val defaultValue: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)
