package com.ampairs.form.data.repository

import com.ampairs.form.domain.EntityConfigSchema
import kotlinx.coroutines.flow.Flow

interface ConfigLookup {
    fun observeConfigSchema(entityType: String): Flow<EntityConfigSchema?>
    suspend fun refreshConfig(entityType: String): Result<EntityConfigSchema>
    suspend fun syncFormConfigs(): Result<Int>
}
