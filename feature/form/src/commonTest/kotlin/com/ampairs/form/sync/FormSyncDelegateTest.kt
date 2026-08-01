package com.ampairs.form.sync

import com.ampairs.common.model.PageResponse
import com.ampairs.form.data.api.ConfigApi
import com.ampairs.form.data.db.FormFieldDao
import com.ampairs.form.data.db.FormFieldEntity
import com.ampairs.form.data.db.FormSchemaDao
import com.ampairs.form.data.db.FormSchemaEntity
import com.ampairs.form.data.db.FormSectionDao
import com.ampairs.form.data.db.FormSectionEntity
import com.ampairs.form.domain.FormField
import com.ampairs.form.domain.FormSchema
import com.ampairs.form.domain.FormSection
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncResult
import com.ampairs.sync.db.SyncPersistStatus
import com.ampairs.sync.db.SyncStateDao
import com.ampairs.sync.db.SyncStateEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Sync semantics of [FormSyncDelegate] over hand-rolled fakes (spec 011, T024/T053/T056):
 * - Optimistic-version conflict recovery: a rejected push rebases onto the server's current version
 *   and retries once; the header self-heals even when the retry also fails (no permanent 409 loop).
 * - Pull replaces clean local aggregates (delete-by-absence) but skips dirty ones (local wins).
 */
class FormSyncDelegateTest {

    // ---- fakes -----------------------------------------------------------------------------------

    private class FakeSchemaDao : FormSchemaDao {
        val rows = mutableMapOf<String, FormSchemaEntity>()
        override suspend fun getByEntityType(entityType: String) = rows[entityType]
        override suspend fun getDirty() = rows.values.filter { it.dirty }
        override suspend fun upsert(schema: FormSchemaEntity) { rows[schema.entityType] = schema }
        override suspend fun setDirty(entityType: String, dirty: Boolean) {
            rows[entityType]?.let { rows[entityType] = it.copy(dirty = dirty) }
        }
    }

    private class FakeSectionDao : FormSectionDao {
        val rows = mutableListOf<FormSectionEntity>()
        private val state = MutableStateFlow<List<FormSectionEntity>>(emptyList())
        override fun observeByEntityType(entityType: String): Flow<List<FormSectionEntity>> =
            state.map { list -> list.filter { it.entityType == entityType } }
        override suspend fun getByEntityType(entityType: String) = rows.filter { it.entityType == entityType }
        override suspend fun insertAll(sections: List<FormSectionEntity>) { rows += sections; state.value = rows.toList() }
        override suspend fun deleteByEntityType(entityType: String) {
            rows.removeAll { it.entityType == entityType }; state.value = rows.toList()
        }
    }

    private class FakeFieldDao : FormFieldDao {
        val rows = mutableListOf<FormFieldEntity>()
        private val state = MutableStateFlow<List<FormFieldEntity>>(emptyList())
        override fun observeByEntityType(entityType: String): Flow<List<FormFieldEntity>> =
            state.map { list -> list.filter { it.entityType == entityType } }
        override suspend fun getByEntityType(entityType: String) = rows.filter { it.entityType == entityType }
        override suspend fun insertAll(fields: List<FormFieldEntity>) { rows += fields; state.value = rows.toList() }
        override suspend fun deleteByEntityType(entityType: String) {
            rows.removeAll { it.entityType == entityType }; state.value = rows.toList()
        }
    }

    private class FakeSyncStateDao : SyncStateDao {
        var lastSyncedAtIso: String? = null
        override fun observeAll(): Flow<List<SyncStateEntity>> = MutableStateFlow(emptyList())
        override fun observe(entity: SyncEntity): Flow<SyncStateEntity?> = MutableStateFlow(null)
        override suspend fun getAll(): List<SyncStateEntity> = emptyList()
        override suspend fun getPending(): List<SyncStateEntity> = emptyList()
        override suspend fun upsert(state: SyncStateEntity) {}
        override suspend fun upsertStatus(
            entity: SyncEntity,
            status: SyncPersistStatus,
            lastSyncedAt: Long?,
            pendingCount: Int,
            errorMessage: String?,
            now: Long,
        ) {}
        override suspend fun getLastSyncedAtIso(entity: SyncEntity): String? = lastSyncedAtIso
        override suspend fun setLastSyncedAtIso(entity: SyncEntity, iso: String) { lastSyncedAtIso = iso }
        override suspend fun markPendingPush(entity: SyncEntity, now: Long) {}
        override suspend fun deleteAll() {}
    }

    /** Scripted API: push behavior is a queue of outcomes; records every base_version pushed. */
    private class FakeConfigApi : ConfigApi {
        var serverVersion: Long = 0
        var pullPages: List<FormSchema> = emptyList()
        val pushedBaseVersions = mutableListOf<Long>()
        var failPushesWithStaleVersion = false

        override suspend fun getSchemaSync(lastSync: String, page: Int, size: Int, sortBy: String, sortDir: String) =
            PageResponse(
                content = if (page == 0) pullPages else emptyList(),
                pageNumber = page, pageSize = size, totalPages = 1,
                totalElements = pullPages.size.toLong(),
                hasNext = false, hasPrevious = false, first = true, last = true,
            )

        override suspend fun pushSchema(schemas: List<FormSchema>): List<FormSchema> {
            val pushed = schemas.single()
            pushedBaseVersions += pushed.version
            if (failPushesWithStaleVersion && pushed.version < serverVersion) {
                throw Exception("409 CONFLICT base_version=${pushed.version}, current=$serverVersion")
            }
            serverVersion = maxOf(serverVersion, pushed.version) + 1
            return listOf(pushed.copy(version = serverVersion, lastUpdated = "2026-06-10T12:00:00"))
        }

        override suspend fun getConfigSchema(entityType: String): FormSchema =
            FormSchema(uid = entityType, entityType = entityType, version = serverVersion)
    }

    // ---- fixtures --------------------------------------------------------------------------------

    private val api = FakeConfigApi()
    private val schemaDao = FakeSchemaDao()
    private val sectionDao = FakeSectionDao()
    private val fieldDao = FakeFieldDao()
    private val syncStateDao = FakeSyncStateDao()

    private val delegate = FormSyncDelegate(api, schemaDao, sectionDao, fieldDao, syncStateDao)

    private fun localDirtyCustomer(version: Long) {
        schemaDao.rows["customer"] = FormSchemaEntity(entityType = "customer", version = version, dirty = true)
        sectionDao.rows += FormSectionEntity(uid = "S1", entityType = "customer", name = "Basics")
        fieldDao.rows += FormFieldEntity(
            uid = "F1", entityType = "customer", source = "standard", fieldKey = "name",
            displayName = "Name", dataType = "text", sectionUid = "S1",
        )
    }

    // ---- push: conflict recovery -----------------------------------------------------------------

    @Test
    fun stale_push_rebases_on_server_version_and_retries_successfully() = runTest {
        localDirtyCustomer(version = 0)
        api.serverVersion = 1
        api.failPushesWithStaleVersion = true

        val result = delegate.pushPendingToServer()

        assertTrue(result is SyncResult.Success)
        // First attempt with the stale base (0), retry with the rebased server version (1).
        assertEquals(listOf(0L, 1L), api.pushedBaseVersions)
        val header = schemaDao.rows["customer"]!!
        assertEquals(2L, header.version, "header adopts the server-resolved version")
        assertFalse(header.dirty, "aggregate is clean after a successful recovered push")
    }

    @Test
    fun failed_retry_still_heals_the_header_version_so_the_next_cycle_uses_a_fresh_base() = runTest {
        localDirtyCustomer(version = 0)
        api.serverVersion = 5
        api.failPushesWithStaleVersion = true
        // Sabotage the retry too: server version moves again between getConfigSchema and push.
        val movingApi = object : ConfigApi by api {
            override suspend fun getConfigSchema(entityType: String): FormSchema {
                val schema = api.getConfigSchema(entityType)
                api.serverVersion += 1   // someone else pushes in between
                return schema
            }
        }
        val racingDelegate = FormSyncDelegate(movingApi, schemaDao, sectionDao, fieldDao, syncStateDao)

        val result = racingDelegate.pushPendingToServer()

        assertTrue(result is SyncResult.Failure)
        val header = schemaDao.rows["customer"]!!
        assertEquals(5L, header.version, "header self-healed to the version read during recovery")
        assertTrue(header.dirty, "still dirty — next cycle retries with the fresh base")
    }

    @Test
    fun matching_base_version_pushes_clean_first_try() = runTest {
        localDirtyCustomer(version = 3)
        api.serverVersion = 3
        api.failPushesWithStaleVersion = true

        val result = delegate.pushPendingToServer()

        assertTrue(result is SyncResult.Success)
        assertEquals(listOf(3L), api.pushedBaseVersions, "no recovery round needed")
        assertEquals(4L, schemaDao.rows["customer"]!!.version)
    }

    // ---- pull: local-dirty-wins + replace --------------------------------------------------------

    @Test
    fun pull_replaces_clean_aggregates_but_skips_dirty_ones() = runTest {
        // Local: clean customer aggregate with a field the server no longer has.
        schemaDao.rows["customer"] = FormSchemaEntity(entityType = "customer", version = 1, dirty = false)
        fieldDao.rows += FormFieldEntity(
            uid = "STALE", entityType = "customer", source = "custom", fieldKey = "old",
            displayName = "Old", dataType = "text", sectionUid = "S1",
        )
        // Local: dirty product aggregate with an unpushed edit.
        schemaDao.rows["product"] = FormSchemaEntity(entityType = "product", version = 1, dirty = true)
        fieldDao.rows += FormFieldEntity(
            uid = "LOCAL_EDIT", entityType = "product", source = "custom", fieldKey = "mine",
            displayName = "Mine", dataType = "text", sectionUid = "S1",
        )

        api.pullPages = listOf(
            FormSchema(
                uid = "customer", entityType = "customer", version = 2,
                sections = listOf(FormSection(uid = "S1", name = "Basics")),
                fields = listOf(
                    FormField(uid = "F_NEW", fieldKey = "name", displayName = "Name", sectionUid = "S1"),
                ),
                lastUpdated = "2026-06-10T12:30:00",
            ),
            FormSchema(uid = "product", entityType = "product", version = 9, lastUpdated = "2026-06-10T12:31:00"),
        )

        val result = delegate.pullFromServer()

        assertTrue(result is SyncResult.Success)
        // Clean aggregate replaced: stale member gone (delete-by-absence), server member present.
        val customerFields = fieldDao.rows.filter { it.entityType == "customer" }
        assertEquals(listOf("F_NEW"), customerFields.map { it.uid })
        assertEquals(2L, schemaDao.rows["customer"]!!.version)
        // Dirty aggregate untouched: local edit survives, version unchanged.
        assertTrue(fieldDao.rows.any { it.uid == "LOCAL_EDIT" })
        assertEquals(1L, schemaDao.rows["product"]!!.version)
        // Checkpoint advanced to the max server timestamp.
        assertEquals("2026-06-10T12:31:00", syncStateDao.lastSyncedAtIso)
    }
}
