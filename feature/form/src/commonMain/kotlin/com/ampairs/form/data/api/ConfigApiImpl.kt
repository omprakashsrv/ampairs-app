package com.ampairs.form.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.di.AppScope
import com.ampairs.common.httpClient
import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response
import com.ampairs.form.domain.FormField
import com.ampairs.form.domain.FormSchema
import com.ampairs.form.domain.FormSection
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Ktor implementation of the aggregate form-config API. NOTE (spec 011): drafted, compile-gate on device. */
@Inject @SingleIn(AppScope::class) @ContributesBinding(AppScope::class)
class ConfigApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository,
) : ConfigApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun getSchemaSync(
        lastSync: String,
        page: Int,
        size: Int,
        sortBy: String,
        sortDir: String,
    ): PageResponse<FormSchema> {
        val response: Response<PageResponse<FormSchema>> = client.get(
            ApiUrlBuilder.formUrl("v1/config/schema/sync")
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

    override suspend fun pushSchema(schemas: List<FormSchema>): List<FormSchema> {
        val body = schemas.map { schema ->
            FormSchemaPushRequest(
                entityType = schema.entityType,
                baseVersion = schema.version,
                sections = schema.sections,
                fields = schema.fields,
            )
        }
        val response = client.post(ApiUrlBuilder.formUrl("v1/config/schema/sync")) {
            setBody(body)
        }.body<Response<List<FormSchema>>>()
        if (response.error != null) throw Exception(response.error?.message ?: "Failed to push form schema")
        return response.data ?: emptyList()
    }

    override suspend fun getConfigSchema(entityType: String): FormSchema {
        val response: Response<FormSchema> = client.get(
            ApiUrlBuilder.formUrl("v1/config/schema")
        ) {
            parameter("entity_type", entityType)
        }.body()
        return response.data ?: throw Exception("Config schema not found for entity type: $entityType")
    }

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
}

/** Push body matching the backend `FormSchemaRequest` (snake_case, optimistic `base_version`). */
@Serializable
private data class FormSchemaPushRequest(
    @SerialName("entity_type") val entityType: String,
    @SerialName("base_version") val baseVersion: Long?,
    val sections: List<FormSection>,
    val fields: List<FormField>,
)
