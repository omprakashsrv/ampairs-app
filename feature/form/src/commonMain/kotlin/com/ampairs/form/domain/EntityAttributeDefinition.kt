package com.ampairs.form.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Generic attribute definition for any entity type
 * Supports custom fields based on business vertical (retail, wholesale, etc.)
 */
@Serializable
data class EntityAttributeDefinition(
    val uid: String,
    @SerialName("entity_type")
    val entityType: String,
    @SerialName("attribute_key")
    val attributeKey: String,
    @SerialName("display_name")
    val displayName: String,
    @SerialName("data_type")
    val dataType: String = AttributeDataType.STRING,
    val visible: Boolean = true,
    val mandatory: Boolean = false,
    val enabled: Boolean = true,
    @SerialName("display_order")
    val displayOrder: Int = 0,
    val category: String? = null,
    @SerialName("default_value")
    val defaultValue: String? = null,
    @SerialName("validation_type")
    val validationType: String? = null,
    @SerialName("validation_params")
    val validationParams: Map<String, String>? = null,
    @SerialName("enum_values")
    val enumValues: List<String>? = null,
    val placeholder: String? = null,
    @SerialName("help_text")
    val helpText: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)
