package com.ampairs.form.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.di.AppScope
import com.ampairs.common.httpClient
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response
import com.ampairs.form.domain.EntityAttributeDefinition
import com.ampairs.form.domain.EntityConfigSchema
import com.ampairs.form.domain.EntityFieldConfig
import com.ampairs.form.domain.SaveConfigSchemaRequest
import io.ktor.client.call.*
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.*

/**
 * Implementation of ConfigApi using Ktor HTTP client
 */
@Inject @SingleIn(AppScope::class) @ContributesBinding(AppScope::class)
class ConfigApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository
) : ConfigApi {

    private val client = httpClient(engine, tokenRepository)

    private fun <T> emptyPage(page: Int, size: Int): PageResponse<T> = PageResponse(
        content = emptyList(),
        pageNumber = page,
        pageSize = size,
        totalPages = 0,
        totalElements = 0L,
        hasNext = false,
        hasPrevious = false,
        first = true,
        last = true,
    )

    override suspend fun getFieldConfigsSync(
        lastSync: String,
        page: Int,
        size: Int,
        sortBy: String,
        sortDir: String,
    ): PageResponse<EntityFieldConfig> {
        val response: Response<PageResponse<EntityFieldConfig>> = client.get(
            ApiUrlBuilder.formUrl("v1/config/field-configs/sync")
        ) {
            parameter("page", page)
            parameter("size", size)
            parameter("sort_by", sortBy)
            parameter("sort_dir", sortDir)
            if (lastSync.isNotBlank()) parameter("last_sync", lastSync)
        }.body()
        if (response.error != null) throw Exception(response.error?.message ?: "Network error")
        return response.data ?: emptyPage(page, size)
    }

    override suspend fun getAttributeDefinitionsSync(
        lastSync: String,
        page: Int,
        size: Int,
        sortBy: String,
        sortDir: String,
    ): PageResponse<EntityAttributeDefinition> {
        val response: Response<PageResponse<EntityAttributeDefinition>> = client.get(
            ApiUrlBuilder.formUrl("v1/config/attribute-definitions/sync")
        ) {
            parameter("page", page)
            parameter("size", size)
            parameter("sort_by", sortBy)
            parameter("sort_dir", sortDir)
            if (lastSync.isNotBlank()) parameter("last_sync", lastSync)
        }.body()
        if (response.error != null) throw Exception(response.error?.message ?: "Network error")
        return response.data ?: emptyPage(page, size)
    }

    override suspend fun pushFieldConfigs(fieldConfigs: List<EntityFieldConfig>): List<EntityFieldConfig> {
        val response = client.post(
            ApiUrlBuilder.formUrl("v1/config/field-configs/sync")
        ) {
            setBody(fieldConfigs)
        }.body<Response<List<EntityFieldConfig>>>()
        if (response.error != null) throw Exception(response.error?.message ?: "Failed to push field configs")
        return response.data ?: emptyList()
    }

    override suspend fun pushAttributeDefinitions(
        attributeDefinitions: List<EntityAttributeDefinition>
    ): List<EntityAttributeDefinition> {
        val response = client.post(
            ApiUrlBuilder.formUrl("v1/config/attribute-definitions/sync")
        ) {
            setBody(attributeDefinitions)
        }.body<Response<List<EntityAttributeDefinition>>>()
        if (response.error != null) throw Exception(response.error?.message ?: "Failed to push attribute definitions")
        return response.data ?: emptyList()
    }

    override suspend fun getConfigSchema(entityType: String): EntityConfigSchema {
        val response: Response<EntityConfigSchema> = client.get(
            ApiUrlBuilder.formUrl("v1/config/schema")
        ) {
            parameter("entity_type", entityType)
        }.body()

        return response.data ?: throw Exception("Config schema not found for entity type: $entityType")
    }

    override suspend fun getAllConfigSchemas(): List<EntityConfigSchema> {
        val response: Response<List<EntityConfigSchema>> = client.get(
            ApiUrlBuilder.formUrl("v1/config/schemas")
        ).body()

        return response.data ?: emptyList()
    }

    override suspend fun getConfigSchemasSince(lastUpdated: String): List<EntityConfigSchema> {
        val response: Response<List<EntityConfigSchema>> = client.get(
            ApiUrlBuilder.formUrl("v1/config/schemas")
        ) {
            parameter("last_updated", lastUpdated)
        }.body()

        return response.data ?: emptyList()
    }

    override suspend fun updateFieldConfig(fieldConfig: EntityFieldConfig): EntityFieldConfig {
        val response = client.put(
            ApiUrlBuilder.formUrl("v1/config/field-config/${fieldConfig.uid}")
        ) {
            setBody(fieldConfig)
        }.body<Response<EntityFieldConfig>>()

        return response.data ?: throw Exception("Failed to update field config")
    }

    override suspend fun updateAttributeDefinition(attributeDefinition: EntityAttributeDefinition): EntityAttributeDefinition {
        val response = client.put(
            ApiUrlBuilder.formUrl("v1/config/attribute-definition/${attributeDefinition.uid}")
        ) {
            setBody(attributeDefinition)
        }.body<Response<EntityAttributeDefinition>>()

        return response.data ?: throw Exception("Failed to update attribute definition")
    }

    override suspend fun updateFieldConfigs(entityType: String, fieldConfigs: List<EntityFieldConfig>): List<EntityFieldConfig> {
        val response = client.put(
            ApiUrlBuilder.formUrl("v1/config/field-configs/$entityType")
        ) {
            setBody(fieldConfigs)
        }.body<Response<List<EntityFieldConfig>>>()

        return response.data ?: emptyList()
    }

    override suspend fun updateAttributeDefinitions(entityType: String, attributeDefinitions: List<EntityAttributeDefinition>): List<EntityAttributeDefinition> {
        val response = client.put(
            ApiUrlBuilder.formUrl("v1/config/attribute-definitions/$entityType")
        ) {
            setBody(attributeDefinitions)
        }.body<Response<List<EntityAttributeDefinition>>>()

        return response.data ?: emptyList()
    }

    override suspend fun saveConfigSchema(entityType: String, schema: EntityConfigSchema): EntityConfigSchema {
        val request = SaveConfigSchemaRequest(
            fieldConfigs = schema.fieldConfigs,
            attributeDefinitions = schema.attributeDefinitions
        )

        val response = client.post(
            ApiUrlBuilder.formUrl("v1/config/config")
        ) {
            setBody(request)
        }.body<Response<EntityConfigSchema>>()

        return response.data ?: throw Exception("Failed to save config schema for entity type: $entityType")
    }
}
