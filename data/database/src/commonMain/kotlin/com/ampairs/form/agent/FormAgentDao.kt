package com.ampairs.form.agent

import androidx.room3.Dao
import androidx.room3.Query
import com.ampairs.form.data.db.FormFieldEntity

/**
 * Agent-facing read-only DAO for form-schema introspection — separate from ConfigRepository's full
 * aggregate assembly. Backs the assistant's form actions (schema lookup, field fill, field suggestion)
 * directly over the `form_field` table (no schema impact).
 */
@Dao
interface FormAgentDao {
    /** Visible fields for an entity type, in display order. */
    @Query("SELECT * FROM form_field WHERE entityType = :entityType AND visible = 1 ORDER BY displayOrder ASC")
    suspend fun visibleFields(entityType: String): List<FormFieldEntity>

    /** A single field by its key within an entity type (visible or not — caller checks). */
    @Query("SELECT * FROM form_field WHERE entityType = :entityType AND fieldKey = :fieldKey LIMIT 1")
    suspend fun fieldByKey(entityType: String, fieldKey: String): FormFieldEntity?
}
