package com.ampairs.form.data.db

import com.ampairs.form.domain.EntityAttributeDefinition
import com.ampairs.form.domain.EntityConfigSchema
import com.ampairs.form.domain.EntityFieldConfig
import com.ampairs.form.domain.FieldDataType
import com.ampairs.form.domain.FieldSource
import com.ampairs.form.domain.FormField
import com.ampairs.form.domain.FormSchema
import com.ampairs.form.domain.FormSection
import com.ampairs.form.domain.OptionSource
import com.ampairs.form.domain.validation.ValidationRule
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Mappers between the domain [FormSchema] aggregate, the Room rows, and the legacy
 * [EntityConfigSchema] read-model that existing consumers (customer/business) + the editor still use.
 *
 * NOTE (spec 011): drafted, compile-gate on device.
 */
internal val formJson = Json { ignoreUnknownKeys = true; encodeDefaults = false }

// ---- Room rows -> domain ---------------------------------------------------------------------

fun List<FormSectionEntity>.toDomainSections(): List<FormSection> =
    map { FormSection(uid = it.uid, name = it.name, displayOrder = it.displayOrder, visible = it.visible) }

fun FormFieldEntity.toDomainField(): FormField = FormField(
    uid = uid,
    source = runCatching { FieldSource.valueOf(source.uppercase()) }.getOrDefault(FieldSource.CUSTOM),
    fieldKey = fieldKey,
    displayName = displayName,
    dataType = runCatching { FieldDataType.valueOf(dataType.uppercase()) }.getOrDefault(FieldDataType.TEXT),
    widgetKey = widgetKey,
    sectionUid = sectionUid,
    visible = visible,
    mandatory = mandatory,
    enabled = enabled,
    displayOrder = displayOrder,
    defaultValue = defaultValue,
    optionSource = optionSource?.let { runCatching { OptionSource.valueOf(it.uppercase()) }.getOrNull() },
    enumValues = enumValues?.let { runCatching { formJson.decodeFromString<List<String>>(it) }.getOrNull() },
    dynamicSourceKey = dynamicSourceKey,
    validationRules = validationRules?.let { runCatching { formJson.decodeFromString<List<ValidationRule>>(it) }.getOrNull() },
    placeholder = placeholder,
    helpText = helpText,
)

fun buildSchema(
    entityType: String,
    header: FormSchemaEntity?,
    sections: List<FormSectionEntity>,
    fields: List<FormFieldEntity>,
): FormSchema = FormSchema(
    uid = entityType,
    entityType = entityType,
    version = header?.version ?: 0,
    sections = sections.toDomainSections(),
    fields = fields.map { it.toDomainField() },
    lastUpdated = header?.updatedAt,
)

// ---- domain -> Room rows ---------------------------------------------------------------------

fun FormSection.toEntity(entityType: String) = FormSectionEntity(
    uid = uid, entityType = entityType, name = name, displayOrder = displayOrder, visible = visible,
)

fun FormField.toEntity(entityType: String) = FormFieldEntity(
    uid = uid,
    entityType = entityType,
    source = source.name.lowercase(),
    fieldKey = fieldKey,
    displayName = displayName,
    dataType = dataType.name.lowercase(),
    widgetKey = widgetKey,
    sectionUid = sectionUid,
    visible = visible,
    mandatory = mandatory,
    enabled = enabled,
    displayOrder = displayOrder,
    defaultValue = defaultValue,
    optionSource = optionSource?.name?.lowercase(),
    enumValues = enumValues?.let { formJson.encodeToString(it) },
    dynamicSourceKey = dynamicSourceKey,
    validationRules = validationRules?.let { formJson.encodeToString(it) },
    placeholder = placeholder,
    helpText = helpText,
)

// ---- FormSchema -> legacy EntityConfigSchema projection (for existing consumers/editor) -------

fun FormSchema.toLegacySchema(): EntityConfigSchema {
    val sectionName: Map<String, String> = sections.associate { it.uid to it.name }
    val fieldConfigs = fields.map { f ->
        EntityFieldConfig(
            uid = f.uid,
            entityType = entityType,
            fieldName = f.fieldKey,
            displayName = f.displayName,
            visible = f.visible,
            mandatory = f.mandatory,
            enabled = f.enabled,
            displayOrder = f.displayOrder,
            placeholder = f.placeholder,
            helpText = f.helpText,
            defaultValue = f.defaultValue,
            updatedAt = lastUpdated,
        )
    }
    val attributeDefinitions = fields.filter { it.source == FieldSource.CUSTOM }.map { f ->
        EntityAttributeDefinition(
            uid = f.uid,
            entityType = entityType,
            attributeKey = f.fieldKey,
            displayName = f.displayName,
            dataType = f.dataType.name,
            visible = f.visible,
            mandatory = f.mandatory,
            enabled = f.enabled,
            displayOrder = f.displayOrder,
            category = sectionName[f.sectionUid],
            defaultValue = f.defaultValue,
            enumValues = f.enumValues,
            placeholder = f.placeholder,
            helpText = f.helpText,
            updatedAt = lastUpdated,
        )
    }
    return EntityConfigSchema(fieldConfigs = fieldConfigs, attributeDefinitions = attributeDefinitions, lastUpdated = lastUpdated)
}
