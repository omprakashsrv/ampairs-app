package com.ampairs.tax.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.httpClient
import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response
import com.ampairs.tax.domain.model.MasterTaxCode
import com.ampairs.tax.domain.model.TaxCode
import com.ampairs.tax.domain.model.TaxComponentType
import com.ampairs.tax.domain.model.TaxConfiguration
import com.ampairs.tax.domain.model.TaxRule
import com.ampairs.tax.domain.model.WorkspaceTaxComponent
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

/**
 * Tax Configuration API Implementation
 */
class TaxConfigurationApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository
) : TaxConfigurationApi {

    private val httpClient = httpClient(engine, tokenRepository)

    override suspend fun getWorkspaceConfiguration(): Result<TaxConfiguration> {
        return try {
            val url = ApiUrlBuilder.taxUrl("v1/configuration")
            val response: Response<TaxConfiguration> = httpClient.get(url).body()

            if (response.data != null && response.error == null) {
                Result.success(response.data!!)
            } else {
                Result.failure(Exception(response.error?.toString() ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateWorkspaceConfiguration(
        config: TaxConfiguration
    ): Result<TaxConfiguration> {
        return try {
            val url = ApiUrlBuilder.taxUrl("v1/configuration")
            val response: Response<TaxConfiguration> = httpClient.put(url) {
                contentType(ContentType.Application.Json)
                setBody(config)
            }.body()

            if (response.data != null && response.error == null) {
                Result.success(response.data!!)
            } else {
                Result.failure(Exception(response.error?.toString() ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchMasterTaxCodes(
        query: String,
        countryCode: String,
        codeType: String?,
        category: String?,
        page: Int,
        size: Int
    ): Result<PageResponse<MasterTaxCode>> {
        return try {
            val url = ApiUrlBuilder.taxUrl(
                "v1/master-code/search",
                mapOf(
                    "query" to query,
                    "countryCode" to countryCode,
                    "codeType" to codeType,
                    "category" to category,
                    "page" to page.toString(),
                    "size" to size.toString()
                ).filterValues { it != null } as Map<String, String>
            )
            val response: Response<PageResponse<MasterTaxCode>> = httpClient.get(url).body()

            if (response.data != null && response.error == null) {
                Result.success(response.data!!)
            } else {
                Result.failure(Exception(response.error?.toString() ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMasterTaxCode(codeId: String): Result<MasterTaxCode> {
        return try {
            val url = ApiUrlBuilder.taxUrl("v1/master-code/$codeId")
            val response: Response<MasterTaxCode> = httpClient.get(url).body()

            if (response.data != null && response.error == null) {
                Result.success(response.data!!)
            } else {
                Result.failure(Exception(response.error?.toString() ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPopularTaxCodes(
        countryCode: String,
        industry: String?,
        limit: Int
    ): Result<List<MasterTaxCode>> {
        return try {
            val url = ApiUrlBuilder.taxUrl(
                "v1/master-code/popular",
                mapOf(
                    "countryCode" to countryCode,
                    "industry" to industry,
                    "limit" to limit.toString()
                ).filterValues { it != null } as Map<String, String>
            )
            val response: Response<List<MasterTaxCode>> = httpClient.get(url).body()

            if (response.data != null && response.error == null) {
                Result.success(response.data!!)
            } else {
                Result.failure(Exception(response.error?.toString() ?: "Unknown error"))
            }
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
            val url = ApiUrlBuilder.taxUrl(
                "v1/code",
                mapOf(
                    "modifiedAfter" to modifiedAfter?.toString(),
                    "page" to page.toString(),
                    "size" to size.toString()
                ).filterValues { it != null } as Map<String, String>
            )
            val response: Response<PageResponse<TaxCode>> = httpClient.get(url).body()

            if (response.data != null && response.error == null) {
                Result.success(response.data!!)
            } else {
                Result.failure(Exception(response.error?.toString() ?: "Unknown error"))
            }
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
            val url = ApiUrlBuilder.taxUrl("v1/code/subscribe")
            val request = SubscribeTaxCodeRequest(
                masterTaxCodeId = masterTaxCodeId,
                customTaxRuleId = customTaxRuleId,
                isFavorite = isFavorite,
                notes = notes
            )
            val response: Response<TaxCode> = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()

            if (response.data != null && response.error == null) {
                Result.success(response.data!!)
            } else {
                Result.failure(Exception(response.error?.toString() ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unsubscribeFromTaxCode(
        workspaceTaxCodeId: String
    ): Result<Unit> {
        return try {
            val url = ApiUrlBuilder.taxUrl("v1/code/$workspaceTaxCodeId")
            httpClient.delete(url)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun bulkSubscribeTaxCodes(
        masterTaxCodeIds: List<String>,
        applyDefaultRules: Boolean
    ): Result<BulkSubscribeResult> {
        return try {
            val url = ApiUrlBuilder.taxUrl("v1/code/bulk-subscribe")
            val request = BulkSubscribeRequest(
                masterTaxCodeIds = masterTaxCodeIds,
                applyDefaultRules = applyDefaultRules
            )
            val response: Response<BulkSubscribeResult> = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()

            if (response.data != null && response.error == null) {
                Result.success(response.data!!)
            } else {
                Result.failure(Exception(response.error?.toString() ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getComponentTypes(countryCode: String): Result<List<TaxComponentType>> {
        return try {
            val url = ApiUrlBuilder.taxUrl("v1/component-type/$countryCode")
            val response: Response<List<TaxComponentType>> = httpClient.get(url).body()

            if (response.data != null && response.error == null) {
                Result.success(response.data!!)
            } else {
                Result.failure(Exception(response.error?.toString() ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getWorkspaceComponents(
        modifiedAfter: Long?
    ): Result<List<WorkspaceTaxComponent>> {
        return try {
            val url = ApiUrlBuilder.taxUrl(
                "v1/component",
                mapOf("modifiedAfter" to modifiedAfter?.toString()).filterValues { it != null } as Map<String, String>
            )
            val response: Response<List<WorkspaceTaxComponent>> = httpClient.get(url).body()

            if (response.data != null && response.error == null) {
                Result.success(response.data!!)
            } else {
                Result.failure(Exception(response.error?.toString() ?: "Unknown error"))
            }
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
            val url = ApiUrlBuilder.taxUrl(
                "v1/rule",
                mapOf(
                    "modifiedAfter" to modifiedAfter?.toString(),
                    "page" to page.toString(),
                    "size" to size.toString()
                ).filterValues { it != null } as Map<String, String>
            )
            val response: Response<PageResponse<TaxRule>> = httpClient.get(url).body()

            if (response.data != null && response.error == null) {
                Result.success(response.data!!)
            } else {
                Result.failure(Exception(response.error?.toString() ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createTaxRule(
        rule: TaxRule
    ): Result<TaxRule> {
        return try {
            val url = ApiUrlBuilder.taxUrl("v1/rule")
            val response: Response<TaxRule> = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(rule)
            }.body()

            if (response.data != null && response.error == null) {
                Result.success(response.data!!)
            } else {
                Result.failure(Exception(response.error?.toString() ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTaxRule(
        ruleId: String,
        rule: TaxRule
    ): Result<TaxRule> {
        return try {
            val url = ApiUrlBuilder.taxUrl("v1/rule/$ruleId")
            val response: Response<TaxRule> = httpClient.put(url) {
                contentType(ContentType.Application.Json)
                setBody(rule)
            }.body()

            if (response.data != null && response.error == null) {
                Result.success(response.data!!)
            } else {
                Result.failure(Exception(response.error?.toString() ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun bulkImportTaxRules(
        rules: List<TaxRule>
    ): Result<BulkImportResult> {
        return try {
            val url = ApiUrlBuilder.taxUrl("v1/rule/bulk-import")
            val response: Response<BulkImportResult> = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(rules)
            }.body()

            if (response.data != null && response.error == null) {
                Result.success(response.data!!)
            } else {
                Result.failure(Exception(response.error?.toString() ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Request DTOs
@Serializable
private data class SubscribeTaxCodeRequest(
    val masterTaxCodeId: String,
    val customTaxRuleId: String? = null,
    val isFavorite: Boolean = false,
    val notes: String? = null
)

@Serializable
private data class BulkSubscribeRequest(
    val masterTaxCodeIds: List<String>,
    val applyDefaultRules: Boolean = true
)
