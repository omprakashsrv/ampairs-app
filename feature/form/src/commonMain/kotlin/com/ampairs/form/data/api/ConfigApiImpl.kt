package com.ampairs.form.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.httpClient
import com.ampairs.common.model.Response
import com.ampairs.form.domain.EntityAttributeDefinition
import com.ampairs.form.domain.EntityConfigSchema
import com.ampairs.form.domain.EntityFieldConfig
import io.ktor.client.call.*
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.*

/**
 * Implementation of ConfigApi using Ktor HTTP client
 */
class ConfigApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository
) : ConfigApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun getConfigSchema(entityType: String): EntityConfigSchema {
        val response: Response<EntityConfigSchema> = client.get(
            ApiUrlBuilder.formUrl("v1/schema")
        ) {
            parameter("entity_type", entityType)
        }.body()

        return response.data ?: throw Exception("Config schema not found for entity type: $entityType")
    }

    override suspend fun getAllConfigSchemas(): List<EntityConfigSchema> {
        val response: Response<List<EntityConfigSchema>> = client.get(
            ApiUrlBuilder.formUrl("v1/schemas")
        ).body()

        return response.data ?: emptyList()
    }

    override suspend fun getConfigSchemasSince(lastUpdated: String): List<EntityConfigSchema> {
        val response: Response<List<EntityConfigSchema>> = client.get(
            ApiUrlBuilder.formUrl("v1/schemas")
        ) {
            parameter("last_updated", lastUpdated)
        }.body()

        return response.data ?: emptyList()
    }

    override suspend fun updateFieldConfig(fieldConfig: EntityFieldConfig): EntityFieldConfig {
        val response = client.put(
            ApiUrlBuilder.formUrl("v1/field-config/${fieldConfig.uid}")
        ) {
            setBody(fieldConfig)
        }.body<Response<EntityFieldConfig>>()

        return response.data ?: throw Exception("Failed to update field config")
    }

    override suspend fun updateAttributeDefinition(attributeDefinition: EntityAttributeDefinition): EntityAttributeDefinition {
        val response = client.put(
            ApiUrlBuilder.formUrl("v1/attribute-definition/${attributeDefinition.uid}")
        ) {
            setBody(attributeDefinition)
        }.body<Response<EntityAttributeDefinition>>()

        return response.data ?: throw Exception("Failed to update attribute definition")
    }

    override suspend fun updateFieldConfigs(entityType: String, fieldConfigs: List<EntityFieldConfig>): List<EntityFieldConfig> {
        val response = client.put(
            ApiUrlBuilder.formUrl("v1/field-configs/$entityType")
        ) {
            setBody(fieldConfigs)
        }.body<Response<List<EntityFieldConfig>>>()

        return response.data ?: emptyList()
    }

    override suspend fun updateAttributeDefinitions(entityType: String, attributeDefinitions: List<EntityAttributeDefinition>): List<EntityAttributeDefinition> {
        val response = client.put(
            ApiUrlBuilder.formUrl("v1/attribute-definitions/$entityType")
        ) {
            setBody(attributeDefinitions)
        }.body<Response<List<EntityAttributeDefinition>>>()

        return response.data ?: emptyList()
    }

    override suspend fun saveConfigSchema(entityType: String, schema: EntityConfigSchema): EntityConfigSchema {
        val request = com.ampairs.form.domain.SaveConfigSchemaRequest(
            fieldConfigs = schema.fieldConfigs,
            attributeDefinitions = schema.attributeDefinitions
        )

        val response = client.post(
            ApiUrlBuilder.formUrl("v1/config")
        ) {
            setBody(request)
        }.body<Response<EntityConfigSchema>>()

        return response.data ?: throw Exception("Failed to save config schema for entity type: $entityType")
    }
}
