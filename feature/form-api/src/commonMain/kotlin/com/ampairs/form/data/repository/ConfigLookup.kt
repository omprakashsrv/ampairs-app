package com.ampairs.form.data.repository

import com.ampairs.form.domain.EntityConfigSchema
import com.ampairs.form.domain.FormSchema
import kotlinx.coroutines.flow.Flow

/**
 * Read surface consumed by domain features. Legacy methods project the unified [FormSchema] into the
 * old [EntityConfigSchema] read-model so existing consumers keep working; [observeSchema] exposes the
 * new aggregate for the schema-driven renderer (US1).
 */
interface ConfigLookup {
    fun observeConfigSchema(entityType: String): Flow<EntityConfigSchema?>
    suspend fun refreshConfig(entityType: String): Result<EntityConfigSchema>
    suspend fun syncFormConfigs(): Result<Int>

    /** The unified aggregate for [entityType] (null until first synced/seeded). */
    fun observeSchema(entityType: String): Flow<FormSchema?>
}
