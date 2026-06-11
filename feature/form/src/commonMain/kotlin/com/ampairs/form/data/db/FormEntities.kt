package com.ampairs.form.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local mirror of the unified form aggregate (spec 011). Three tables joined on `entityType`:
 * [FormSchemaEntity] header (+ optimistic `version`, `dirty` flag) and its [FormSectionEntity] /
 * [FormFieldEntity] members. JSON columns are stored as strings. No soft-delete — removal is by
 * absence (the delegate replaces the aggregate on pull).
 *
 * NOTE (spec 011): drafted, compile-gate on device before relying on it.
 */
@Entity(tableName = "form_schema")
data class FormSchemaEntity(
    @PrimaryKey val entityType: String,
    val version: Long = 0,
    val dirty: Boolean = false,
    val updatedAt: String? = null,
)

@Entity(tableName = "form_section")
data class FormSectionEntity(
    @PrimaryKey val uid: String,
    val entityType: String,
    val name: String,
    val displayOrder: Int = 0,
    val visible: Boolean = true,
)

@Entity(tableName = "form_field")
data class FormFieldEntity(
    @PrimaryKey val uid: String,
    val entityType: String,
    val source: String,
    val fieldKey: String,
    val displayName: String,
    val dataType: String,
    val widgetKey: String? = null,
    val sectionUid: String,
    val visible: Boolean = true,
    val mandatory: Boolean = false,
    val enabled: Boolean = true,
    val displayOrder: Int = 0,
    val defaultValue: String? = null,
    val optionSource: String? = null,
    val enumValues: String? = null,        // JSON array string
    val dynamicSourceKey: String? = null,
    val validationRules: String? = null,   // JSON array string
    val placeholder: String? = null,
    val helpText: String? = null,
)
