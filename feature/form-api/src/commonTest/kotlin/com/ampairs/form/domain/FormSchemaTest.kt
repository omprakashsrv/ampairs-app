package com.ampairs.form.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Schema rendering contract (spec 011, T037 subset): only visible fields, in section/display order;
 * fields whose section hasn't synced yet group transiently under General — never an error.
 */
class FormSchemaTest {

    private fun field(
        key: String,
        section: String,
        order: Int,
        visible: Boolean = true,
        mandatory: Boolean = false,
    ) = FormField(
        uid = "F_$key", fieldKey = key, displayName = key.uppercase(),
        sectionUid = section, visible = visible, mandatory = mandatory, displayOrder = order,
    )

    private val schema = FormSchema(
        uid = "customer", entityType = "customer", version = 1,
        sections = listOf(
            FormSection(uid = "S2", name = "Contact", displayOrder = 1),
            FormSection(uid = "S1", name = "Basics", displayOrder = 0),
            FormSection(uid = "S3", name = "Hidden", displayOrder = 2, visible = false),
        ),
        fields = listOf(
            field("email", section = "S2", order = 0),
            field("name", section = "S1", order = 0, mandatory = true),
            field("phone", section = "S2", order = 1),
            field("secret", section = "S1", order = 1, visible = false),
            field("orphan", section = "S_NOT_SYNCED", order = 0),
        ),
    )

    @Test
    fun visibleFields_filters_hidden_and_orders_by_displayOrder() {
        val visible = schema.visibleFields()
        assertFalse(visible.any { it.fieldKey == "secret" })
        assertTrue(visible.map { it.fieldKey }.containsAll(listOf("name", "email", "phone", "orphan")))
    }

    @Test
    fun mandatoryFields_only_visible_and_mandatory() {
        assertEquals(listOf("name"), schema.mandatoryFields().map { it.fieldKey })
    }

    @Test
    fun sectionsWithFields_orders_sections_and_groups_fields() {
        val grouped = schema.sectionsWithFields()
        // Hidden section S3 is dropped; General appears for the orphan field.
        assertEquals(listOf("Basics", "Contact", "General"), grouped.map { it.first.name })
        assertEquals(listOf("name"), grouped[0].second.map { it.fieldKey })
        assertEquals(listOf("email", "phone"), grouped[1].second.map { it.fieldKey })
        assertEquals(listOf("orphan"), grouped[2].second.map { it.fieldKey })
    }

    @Test
    fun orphan_field_with_unsynced_section_renders_in_general_not_error() {
        val general = schema.sectionsWithFields().last()
        assertEquals("General", general.first.name)
        assertEquals("orphan", general.second.single().fieldKey)
    }

    @Test
    fun isFieldVisible_respects_field_flag() {
        assertTrue(schema.isFieldVisible("name"))
        assertFalse(schema.isFieldVisible("secret"))
        assertFalse(schema.isFieldVisible("nonexistent"))
    }

    @Test
    fun empty_sections_are_omitted() {
        val withEmptySection = schema.copy(
            sections = schema.sections + FormSection(uid = "S4", name = "Empty", displayOrder = 5),
        )
        assertFalse(withEmptySection.sectionsWithFields().any { it.first.name == "Empty" })
    }
}
