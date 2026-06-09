package com.ampairs.form.domain

import com.ampairs.form.domain.validation.ValidationRule
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Unified form schema aggregate (mirrors the backend `FormSchemaResponse`). The whole schema for one
 * entity type is transferred as one record over `/config/schema/sync`.
 *
 * NOTE (spec 011): drafted but NOT yet compile-gated in this environment (KMP toolchain unavailable).
 * Verify with `./gradlew shared:compileKotlinIosSimulatorArm64` etc. before relying on it.
 */
@Serializable
data class FormSchema(
    val uid: String,
    @SerialName("entity_type") val entityType: String,
    val version: Long = 0,
    val sections: List<FormSection> = emptyList(),
    val fields: List<FormField> = emptyList(),
    @SerialName("last_updated") val lastUpdated: String? = null,
) {
    /** Fields that should render, in section/display order. */
    fun visibleFields(): List<FormField> = fields.filter { it.visible }.sortedBy { it.displayOrder }

    fun mandatoryFields(): List<FormField> = visibleFields().filter { it.mandatory }

    /** Visible sections (ordered) each paired with their visible fields (ordered). */
    fun sectionsWithFields(): List<Pair<FormSection, List<FormField>>> {
        val visible = visibleFields()
        val defaultGroup = FormSection(uid = "__general__", name = "General", displayOrder = Int.MAX_VALUE)
        val knownSectionUids = sections.map { it.uid }.toSet()
        return (sections.filter { it.visible } + defaultGroup)
            .sortedBy { it.displayOrder }
            .map { section ->
                val sectionFields = visible.filter {
                    it.sectionUid == section.uid ||
                        (section.uid == defaultGroup.uid && it.sectionUid !in knownSectionUids)
                }
                section to sectionFields
            }
            .filter { it.second.isNotEmpty() }
    }

    fun isFieldVisible(fieldKey: String): Boolean = fields.any { it.fieldKey == fieldKey && it.visible }
}

@Serializable
data class FormSection(
    val uid: String,
    val name: String,
    @SerialName("display_order") val displayOrder: Int = 0,
    val visible: Boolean = true,
)

@Serializable
data class FormField(
    val uid: String,
    val source: FieldSource = FieldSource.CUSTOM,
    @SerialName("field_key") val fieldKey: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("data_type") val dataType: FieldDataType = FieldDataType.TEXT,
    @SerialName("widget_key") val widgetKey: String? = null,
    @SerialName("section_uid") val sectionUid: String,
    val visible: Boolean = true,
    val mandatory: Boolean = false,
    val enabled: Boolean = true,
    @SerialName("display_order") val displayOrder: Int = 0,
    @SerialName("default_value") val defaultValue: String? = null,
    @SerialName("option_source") val optionSource: OptionSource? = null,
    @SerialName("enum_values") val enumValues: List<String>? = null,
    @SerialName("dynamic_source_key") val dynamicSourceKey: String? = null,
    @SerialName("validation_rules") val validationRules: List<ValidationRule>? = null,
    val placeholder: String? = null,
    @SerialName("help_text") val helpText: String? = null,
)
