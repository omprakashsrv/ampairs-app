package com.ampairs.printing.data.repository

import com.ampairs.printing.core.model.Template
import com.ampairs.printing.data.db.PrintTemplateDao
import com.ampairs.printing.data.db.toEntity
import com.ampairs.printing.data.db.toTemplate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

/**
 * Local-only template store (offline-first). Writes Room with `synced = false` and flags
 * `PENDING_PUSH`; all server traffic lives in the TemplateSyncDelegate (added in the sync phase).
 * Templates are workspace-shared so every device prints an identical document.
 */
@Inject
class TemplateRepository(
    private val templateDao: PrintTemplateDao,
    private val syncStateDao: SyncStateDao,
) {
    fun observeTemplates(documentType: String): Flow<List<Template>> =
        templateDao.observeByType(documentType).map { rows -> rows.map { it.toTemplate() } }

    suspend fun getTemplate(id: String): Template? = templateDao.getById(id)?.toTemplate()

    suspend fun save(template: Template) {
        require(template.id.isNotBlank()) { "Template UID must be set by the ViewModel" }
        templateDao.upsert(template.toEntity(synced = false))
        markPending()
    }

    private suspend fun markPending() =
        syncStateDao.markPendingPush(SyncEntity.PRINT_TEMPLATE, Clock.System.now().toEpochMilliseconds())
}
