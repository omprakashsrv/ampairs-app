package com.ampairs.printing.data.api

import com.ampairs.common.model.PageResponse
import com.ampairs.printing.data.db.PrintTemplateEntity
import com.ampairs.printing.data.db.templateJsonIsDefault
import com.ampairs.printing.data.db.templateJsonWithDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Wire model for the workspace-synced print template, mirroring [PrintTemplateEntity]. */
@Serializable
data class TemplateDto(
    val id: String,
    @SerialName("document_type") val documentType: String,
    @SerialName("printer_class") val printerClass: String,
    val name: String,
    @SerialName("template_json") val templateJson: String,
    val version: Long = 1,
    val active: Boolean = true,
    @SerialName("is_default") val isDefault: Boolean = false,
    @SerialName("updated_at") val updatedAt: String? = null,
)

fun PrintTemplateEntity.toDto(): TemplateDto = TemplateDto(
    id = id, documentType = documentType, printerClass = printerClass, name = name,
    templateJson = templateJson, version = version, active = active,
    // is_default is a first-class server field; derive it from the (authoritative-locally) JSON.
    isDefault = templateJsonIsDefault(templateJson), updatedAt = updatedAt,
)

fun TemplateDto.toEntity(synced: Boolean): PrintTemplateEntity = PrintTemplateEntity(
    id = id, documentType = documentType, printerClass = printerClass, name = name,
    // Reconcile the embedded flag with the server's is_default column (server wins on pull).
    templateJson = templateJsonWithDefault(templateJson, isDefault),
    version = version, synced = synced, active = active, updatedAt = updatedAt,
)

/** Workspace template `/sync` contract (canonical: GET pull incl. soft-deleted, POST bulk upsert). */
interface TemplateApi {
    suspend fun getTemplatesSync(
        lastSync: String,
        page: Int = 0,
        size: Int = 100,
        sortBy: String = "updatedAt",
        sortDir: String = "ASC",
    ): PageResponse<TemplateDto>

    suspend fun pushTemplates(templates: List<TemplateDto>): List<TemplateDto>
}
