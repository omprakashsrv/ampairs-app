package com.ampairs.tax.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.di.AppScope
import com.ampairs.common.httpClient
import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response
import com.ampairs.tax.domain.model.MasterTaxCode
import com.ampairs.tax.domain.model.TaxCalculationMethod
import com.ampairs.tax.domain.model.TaxCode
import com.ampairs.tax.domain.model.TaxComponentCategory
import com.ampairs.tax.domain.model.TaxComponentType
import com.ampairs.tax.domain.model.TaxConfiguration
import com.ampairs.tax.domain.model.TaxRule
import com.ampairs.tax.domain.model.WorkspaceTaxComponent
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Inject @SingleIn(AppScope::class) @ContributesBinding(AppScope::class)
class TaxConfigurationApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository
) : TaxConfigurationApi {

    private val httpClient = httpClient(engine, tokenRepository)

    override suspend fun getWorkspaceConfiguration(): Result<TaxConfiguration> {
        return try {
            val response: Response<TaxConfiguration> =
                httpClient.get(ApiUrlBuilder.taxUrl("v1/configurations")).body()
            response.toResult()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createWorkspaceConfiguration(config: TaxConfiguration): Result<TaxConfiguration> {
        return try {
            val response: Response<TaxConfiguration> =
                httpClient.post(ApiUrlBuilder.taxUrl("v1/configurations")) {
                    contentType(ContentType.Application.Json)
                    setBody(config)
                }.body()
            response.toResult()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateWorkspaceConfiguration(config: TaxConfiguration): Result<TaxConfiguration> {
        return try {
            val response: Response<TaxConfiguration> =
                httpClient.put(ApiUrlBuilder.taxUrl("v1/configurations")) {
                    contentType(ContentType.Application.Json)
                    setBody(config)
                }.body()
            response.toResult()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Backend param is "q", not "query"
    override suspend fun searchMasterTaxCodes(
        query: String,
        countryCode: String,
        codeType: String?,
        category: String?,
        page: Int,
        size: Int
    ): Result<PageResponse<MasterTaxCode>> {
        return try {
            val params = buildMap<String, String> {
                put("q", query)
                put("countryCode", countryCode)
                if (codeType != null) put("codeType", codeType)
                if (category != null) put("category", category)
                put("page", page.toString())
                put("size", size.toString())
            }
            val response: Response<PageResponse<MasterTaxCode>> =
                httpClient.get(ApiUrlBuilder.taxUrl("v1/master-codes/search", params)).body()
            response.toResult()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMasterTaxCode(codeId: String): Result<MasterTaxCode> {
        return try {
            val response: Response<MasterTaxCode> =
                httpClient.get(ApiUrlBuilder.taxUrl("v1/master-codes/$codeId")).body()
            response.toResult()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Backend returns PageResponse, "size" is the correct param name (not "limit")
    override suspend fun getPopularTaxCodes(
        countryCode: String,
        industry: String?,
        limit: Int
    ): Result<PageResponse<MasterTaxCode>> {
        return try {
            val params = buildMap<String, String> {
                put("countryCode", countryCode)
                if (industry != null) put("industry", industry)
                put("size", limit.toString())
            }
            val response: Response<PageResponse<MasterTaxCode>> =
                httpClient.get(ApiUrlBuilder.taxUrl("v1/master-codes/popular", params)).body()
            response.toResult()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTaxCodes(
        modifiedAfter: Long?,
        page: Int,
        size: Int
    ): Result<PageResponse<TaxCode>> {
        return try {
            val params = buildMap<String, String> {
                if (modifiedAfter != null) put("modifiedAfter", modifiedAfter.toString())
                put("page", page.toString())
                put("size", size.toString())
            }
            val response: Response<PageResponse<TaxCode>> =
                httpClient.get(ApiUrlBuilder.taxUrl("v1/codes", params)).body()
            response.toResult()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun subscribeToTaxCode(
        masterTaxCodeId: String,
        customTaxRuleId: String?,
        isFavorite: Boolean,
        notes: String?
    ): Result<TaxCode> {
        return try {
            val response: Response<TaxCode> =
                httpClient.post(ApiUrlBuilder.taxUrl("v1/codes/subscribe")) {
                    contentType(ContentType.Application.Json)
                    setBody(SubscribeTaxCodeRequest(masterTaxCodeId, customTaxRuleId, isFavorite, notes))
                }.body()
            response.toResult()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unsubscribeFromTaxCode(workspaceTaxCodeId: String): Result<Unit> {
        return try {
            httpClient.delete(ApiUrlBuilder.taxUrl("v1/codes/$workspaceTaxCodeId"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Backend: PATCH /tax/v1/codes/{taxCodeId}
    override suspend fun updateTaxCodeConfig(
        taxCodeId: String,
        isFavorite: Boolean?,
        notes: String?,
        customName: String?,
    ): Result<TaxCode> {
        return try {
            val response: Response<TaxCode> =
                httpClient.patch(ApiUrlBuilder.taxUrl("v1/codes/$taxCodeId")) {
                    contentType(ContentType.Application.Json)
                    setBody(UpdateTaxCodeConfigRequest(isFavorite, notes, customName))
                }.body()
            response.toResult()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun bulkSubscribeTaxCodes(
        masterTaxCodeIds: List<String>,
        applyDefaultRules: Boolean
    ): Result<BulkSubscribeResult> {
        return try {
            val response: Response<BulkSubscribeResult> =
                httpClient.post(ApiUrlBuilder.taxUrl("v1/codes/bulk-subscribe")) {
                    contentType(ContentType.Application.Json)
                    setBody(BulkSubscribeRequest(masterTaxCodeIds, applyDefaultRules))
                }.body()
            response.toResult()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Backend sends a simplified DTO — map to the richer domain model with defaults
    override suspend fun getComponentTypes(countryCode: String): Result<List<TaxComponentType>> {
        return try {
            val response: Response<List<TaxComponentTypeResponse>> =
                httpClient.get(ApiUrlBuilder.taxUrl("v1/component-types/$countryCode")).body()
            response.toResult { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // TODO: no backend endpoint yet — will return 404
    override suspend fun getWorkspaceComponents(
        modifiedAfter: Long?,
        page: Int,
        size: Int
    ): Result<PageResponse<WorkspaceTaxComponent>> {
        return try {
            val params = buildMap<String, String> {
                if (modifiedAfter != null) put("modifiedAfter", modifiedAfter.toString())
                put("page", page.toString())
                put("size", size.toString())
            }
            val response: Response<PageResponse<WorkspaceTaxComponent>> =
                httpClient.get(ApiUrlBuilder.taxUrl("v1/components", params)).body()
            response.toResult()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTaxRules(
        modifiedAfter: Long?,
        page: Int,
        size: Int
    ): Result<PageResponse<TaxRule>> {
        return try {
            val params = buildMap<String, String> {
                if (modifiedAfter != null) put("modifiedAfter", modifiedAfter.toString())
                put("page", page.toString())
                put("size", size.toString())
            }
            val response: Response<PageResponse<TaxRule>> =
                httpClient.get(ApiUrlBuilder.taxUrl("v1/rules", params)).body()
            response.toResult()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTaxRulesByTaxCode(taxCodeId: String): Result<List<TaxRule>> {
        return try {
            val response: Response<List<TaxRule>> =
                httpClient.get(ApiUrlBuilder.taxUrl("v1/rules/tax-code/$taxCodeId")).body()
            response.toResult()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Backend: PUT /tax/v1/rules/{ruleId} accepts UpdateTaxRuleRequest (subset of TaxRule)
    override suspend fun updateTaxRule(ruleId: String, rule: TaxRule): Result<TaxRule> {
        return try {
            val response: Response<TaxRule> =
                httpClient.put(ApiUrlBuilder.taxUrl("v1/rules/$ruleId")) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        UpdateTaxRuleRequest(
                            jurisdiction = rule.jurisdiction.ifBlank { null },
                            jurisdictionLevel = rule.jurisdictionLevel.ifBlank { null },
                            componentComposition = rule.componentComposition.ifEmpty { null },
                            isActive = rule.isActive,
                        )
                    )
                }.body()
            response.toResult()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Backend: DELETE /tax/v1/rules/{ruleId}
    override suspend fun deleteTaxRule(ruleId: String): Result<Unit> {
        return try {
            httpClient.delete(ApiUrlBuilder.taxUrl("v1/rules/$ruleId"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // TODO: no backend POST /tax/v1/rules endpoint yet — will return 405
    override suspend fun createTaxRule(rule: TaxRule): Result<TaxRule> {
        return try {
            val response: Response<TaxRule> =
                httpClient.post(ApiUrlBuilder.taxUrl("v1/rules")) {
                    contentType(ContentType.Application.Json)
                    setBody(rule)
                }.body()
            response.toResult()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // TODO: no backend POST /tax/v1/rules/bulk-import endpoint yet — will return 404
    override suspend fun bulkImportTaxRules(rules: List<TaxRule>): Result<BulkImportResult> {
        return try {
            val response: Response<BulkImportResult> =
                httpClient.post(ApiUrlBuilder.taxUrl("v1/rules/bulk-import")) {
                    contentType(ContentType.Application.Json)
                    setBody(rules)
                }.body()
            response.toResult()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private fun <T> Response<T>.toResult(): Result<T> =
        if (data != null && error == null) Result.success(data!!)
        else Result.failure(Exception(error?.toString() ?: "Unknown error"))

    private fun <T, R> Response<T>.toResult(transform: (T) -> R): Result<R> =
        if (data != null && error == null) Result.success(transform(data!!))
        else Result.failure(Exception(error?.toString() ?: "Unknown error"))
}

// ── Request DTOs ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalSerializationApi::class)
@Serializable
private data class SubscribeTaxCodeRequest(
    @SerialName("master_tax_code_id") val masterTaxCodeId: String,
    @SerialName("custom_tax_rule_id") val customTaxRuleId: String? = null,
    // @EncodeDefault: global encodeDefaults=false would drop this when false; backend requires it present
    @EncodeDefault @SerialName("is_favorite") val isFavorite: Boolean = false,
    val notes: String? = null,
)

@Serializable
private data class UpdateTaxCodeConfigRequest(
    @SerialName("is_favorite") val isFavorite: Boolean? = null,
    val notes: String? = null,
    @SerialName("custom_name") val customName: String? = null,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
private data class BulkSubscribeRequest(
    @SerialName("master_tax_code_ids") val masterTaxCodeIds: List<String>,
    @EncodeDefault @SerialName("apply_default_rules") val applyDefaultRules: Boolean = true,
)

@Serializable
private data class UpdateTaxRuleRequest(
    val jurisdiction: String? = null,
    @SerialName("jurisdiction_level") val jurisdictionLevel: String? = null,
    @SerialName("component_composition") val componentComposition: Map<String, com.ampairs.tax.domain.model.ComponentComposition>? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
)

// ── TaxComponentType response DTO (matches backend TaxComponentTypeDto) ──────

@Serializable
private data class TaxComponentTypeResponse(
    val id: String,
    val name: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("country_code") val countryCode: String,
    @SerialName("tax_type") val taxType: String,
    @SerialName("is_compound") val isCompound: Boolean = false,
    @SerialName("calculation_method") val calculationMethod: String,
    val description: String? = null,
)

private fun TaxComponentTypeResponse.toDomain(): TaxComponentType {
    val method = runCatching { TaxCalculationMethod.valueOf(calculationMethod) }
        .getOrDefault(TaxCalculationMethod.PERCENTAGE)
    return TaxComponentType(
        id = id,
        countryCode = countryCode,
        componentCode = name,
        componentName = displayName,
        shortName = name,
        displayName = displayName,
        componentCategory = TaxComponentCategory.FEDERAL,
        calculationMethod = method,
        isMandatory = true,
        defaultRate = null,
        sortOrder = 0,
        metadata = buildMap {
            put("tax_type", taxType)
            if (isCompound) put("is_compound", "true")
            if (description != null) put("description", description)
        },
        isActive = true,
    )
}
