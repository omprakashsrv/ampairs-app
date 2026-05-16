package com.ampairs.form.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request DTO for bulk save/update of configuration schema
 * Maps to backend EntityConfigSchemaRequest
 */
@Serializable
data class SaveConfigSchemaRequest(
    @SerialName("field_configs")
    val fieldConfigs: List<EntityFieldConfig> = emptyList(),
    @SerialName("attribute_definitions")
    val attributeDefinitions: List<EntityAttributeDefinition> = emptyList()
)
