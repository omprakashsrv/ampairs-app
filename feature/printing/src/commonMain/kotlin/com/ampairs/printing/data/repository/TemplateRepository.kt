package com.ampairs.printing.data.repository

import com.ampairs.printing.core.model.PrinterClass
import com.ampairs.printing.core.model.Template
import com.ampairs.printing.data.db.PrintTemplateDao
import com.ampairs.printing.data.db.toEntity
import com.ampairs.printing.data.db.toTemplate
import com.ampairs.printing.template.DefaultTemplates
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/**
 * Local-only template store (offline-first). Writes Room with `synced = false` and flags
 * `PENDING_PUSH`; all server traffic lives in the TemplateSyncDelegate. Templates are
 * workspace-shared so every device prints an identical document.
 */
@Inject
class TemplateRepository(
    private val templateDao: PrintTemplateDao,
    private val syncStateDao: SyncStateDao,
) {
    /** All active templates, ordered by document type then printer class (for the setup screen). */
    fun observeAll(): Flow<List<Template>> =
        templateDao.observeAllActive().map { rows -> rows.map { it.toTemplate() } }

    fun observeTemplates(documentType: String): Flow<List<Template>> =
        templateDao.observeByType(documentType).map { rows -> rows.map { it.toTemplate() } }

    suspend fun getTemplate(id: String): Template? = templateDao.getById(id)?.toTemplate()

    /** First active template for a document type (used to pick a default at print time). */
    suspend fun firstTemplate(documentType: String): Template? =
        templateDao.firstByType(documentType)?.toTemplate()

    /**
     * Template for a document type that matches the printer's class (thermal vs page), falling back
     * to any template for that type. This is what makes "the right template for the selected printer
     * type" get sent to the print job.
     */
    suspend fun firstTemplate(documentType: String, printerClass: PrinterClass): Template? =
        templateDao.firstByTypeAndClass(documentType, printerClass.name)?.toTemplate()
            ?: firstTemplate(documentType)

    suspend fun save(template: Template) {
        require(template.id.isNotBlank()) { "Template UID must be set by the ViewModel" }
        templateDao.upsert(template.toEntity(synced = false))
        markPending()
    }

    /** Seed the built-in templates once (first run) so they exist locally AND sync to the backend. */
    suspend fun seedDefaultsIfEmpty() {
        if (templateDao.count() > 0) return
        DefaultTemplates.all().forEach { templateDao.upsert(it.toEntity(synced = false)) }
        markPending()
    }

    /** Re-seed the built-in templates (overwrites by id) — "Restore defaults" in setup. */
    suspend fun restoreDefaults() {
        DefaultTemplates.all().forEach { templateDao.upsert(it.toEntity(synced = false)) }
        markPending()
    }

    private suspend fun markPending() =
        syncStateDao.markPendingPush(SyncEntity.PRINT_TEMPLATE, Clock.System.now().toEpochMilliseconds())
}
