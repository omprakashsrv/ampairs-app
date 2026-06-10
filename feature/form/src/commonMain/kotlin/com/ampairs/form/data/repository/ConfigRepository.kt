package com.ampairs.form.data.repository

import com.ampairs.form.FORM_SYNC_BATCH_SIZE
import com.ampairs.form.data.api.ConfigApi
import com.ampairs.form.data.db.FormFieldDao
import com.ampairs.form.data.db.FormSchemaDao
import com.ampairs.form.data.db.FormSchemaEntity
import com.ampairs.form.data.db.FormSectionDao
import com.ampairs.form.data.db.buildSchema
import com.ampairs.form.data.db.toEntity
import com.ampairs.form.data.db.toLegacySchema
import com.ampairs.form.domain.EntityConfigSchema
import com.ampairs.form.domain.FieldDataType
import com.ampairs.form.domain.FieldSource
import com.ampairs.form.domain.FormField
import com.ampairs.form.domain.FormSchema
import com.ampairs.form.domain.FormSection
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Form-config repository. Local-only writes (Room); the server round-trip lives in
 * [com.ampairs.form.sync.FormSyncDelegate] on the aggregate /sync feed. Projects the unified
 * [FormSchema] into the legacy [EntityConfigSchema] read-model for existing consumers/editor, and
 * exposes [observeSchema] for the schema-driven renderer.
 *
 * NOTE (spec 011): drafted, compile-gate on device. The legacy [saveConfigSchema] path is interim and
 * lossy (collapses sections to a default and resets unknown data types) — superseded by the US2 editor.
 */
@Inject
class ConfigRepository(
    private val api: ConfigApi,
    private val schemaDao: FormSchemaDao,
    private val sectionDao: FormSectionDao,
    private val fieldDao: FormFieldDao,
    private val syncStateDao: SyncStateDao,
) : ConfigLookup {

    private fun observeAggregate(entityType: String): Flow<FormSchema?> =
        combine(
            sectionDao.observeByEntityType(entityType),
            fieldDao.observeByEntityType(entityType),
        ) { sections, fields ->
            if (sections.isEmpty() && fields.isEmpty()) null
            else buildSchema(entityType, header = null, sections = sections, fields = fields)
        }

    override fun observeSchema(entityType: String): Flow<FormSchema?> = observeAggregate(entityType)

    override fun observeConfigSchema(entityType: String): Flow<EntityConfigSchema?> =
        combine(
            sectionDao.observeByEntityType(entityType),
            fieldDao.observeByEntityType(entityType),
        ) { sections, fields ->
            if (sections.isEmpty() && fields.isEmpty()) null
            else buildSchema(entityType, header = null, sections = sections, fields = fields).toLegacySchema()
        }

    /** UI-invoked read: fetch the aggregate, cache it (unless local has unsynced edits), project to legacy. */
    suspend fun getConfigSchema(entityType: String): Result<EntityConfigSchema> {
        return try {
            val schema = api.getConfigSchema(entityType)
            if (schemaDao.getByEntityType(entityType)?.dirty != true) replaceLocal(schema, dirty = false)
            Result.success(schema.toLegacySchema())
        } catch (e: Exception) {
            observeConfigSchema(entityType).first()?.let { return Result.success(it) }
            Result.failure(e)
        }
    }

    override suspend fun refreshConfig(entityType: String): Result<EntityConfigSchema> = getConfigSchema(entityType)

    /** Legacy full pull (kept for the existing list callers). New code: emit(TriggerPull(FORM)). */
    override suspend fun syncFormConfigs(): Result<Int> = try {
        val lastSync = syncStateDao.getLastSyncedAtIso(SyncEntity.FORM) ?: ""
        var total = 0
        var maxServerTime = ""
        var page = 0
        do {
            val response = api.getSchemaSync(lastSync, page, FORM_SYNC_BATCH_SIZE)
            for (schema in response.content) {
                if (schemaDao.getByEntityType(schema.entityType)?.dirty != true) replaceLocal(schema, dirty = false)
                schema.lastUpdated?.takeIf { it.isNotBlank() && it > maxServerTime }?.let { maxServerTime = it }
                total++
            }
            page++
        } while (response.hasNext && total < 10_000)
        if (maxServerTime.isNotBlank()) syncStateDao.setLastSyncedAtIso(SyncEntity.FORM, maxServerTime)
        Result.success(total)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Local-only write of an edited schema (aggregate). Marks the aggregate dirty + PENDING_PUSH. */
    @OptIn(ExperimentalTime::class)
    suspend fun saveSchema(schema: FormSchema): Result<FormSchema> = try {
        replaceLocal(schema, dirty = true)
        syncStateDao.markPendingPush(SyncEntity.FORM, Clock.System.now().toEpochMilliseconds())
        Result.success(schema)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Legacy editor save — converts the old read-model to an aggregate (interim, lossy). */
    @OptIn(ExperimentalTime::class)
    suspend fun saveConfigSchema(entityType: String, schema: EntityConfigSchema): Result<EntityConfigSchema> = try {
        saveSchema(schema.toAggregate(entityType))
        Result.success(schema)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun replaceLocal(schema: FormSchema, dirty: Boolean) {
        sectionDao.deleteByEntityType(schema.entityType)
        fieldDao.deleteByEntityType(schema.entityType)
        sectionDao.insertAll(schema.sections.map { it.toEntity(schema.entityType) })
        fieldDao.insertAll(schema.fields.map { it.toEntity(schema.entityType) })
        // Never regress the push base: an editor save carries the version its working copy was
        // loaded with, which may predate a newer pull/conflict-recovery — keep the max so the
        // next push presents the freshest base_version.
        val knownVersion = schemaDao.getByEntityType(schema.entityType)?.version ?: 0
        schemaDao.upsert(
            FormSchemaEntity(
                entityType = schema.entityType,
                version = maxOf(schema.version, knownVersion),
                dirty = dirty,
                updatedAt = schema.lastUpdated,
            )
        )
    }
}

/** Interim legacy→aggregate conversion: all fields under a single default section. */
private fun EntityConfigSchema.toAggregate(entityType: String): FormSchema {
    val generalUid = "$entityType-general"
    val section = FormSection(uid = generalUid, name = "General", displayOrder = 0)
    val standardFields = fieldConfigs.map { fc ->
        FormField(
            uid = fc.uid,
            source = FieldSource.STANDARD,
            fieldKey = fc.fieldName,
            displayName = fc.displayName,
            dataType = FieldDataType.TEXT,
            sectionUid = generalUid,
            visible = fc.visible,
            mandatory = fc.mandatory,
            enabled = fc.enabled,
            displayOrder = fc.displayOrder,
            defaultValue = fc.defaultValue,
            placeholder = fc.placeholder,
            helpText = fc.helpText,
        )
    }
    val customFields = attributeDefinitions.map { ad ->
        FormField(
            uid = ad.uid,
            source = FieldSource.CUSTOM,
            fieldKey = ad.attributeKey,
            displayName = ad.displayName,
            dataType = ad.dataType.toFieldDataType(),
            sectionUid = generalUid,
            visible = ad.visible,
            mandatory = ad.mandatory,
            enabled = ad.enabled,
            displayOrder = ad.displayOrder,
            defaultValue = ad.defaultValue,
            enumValues = ad.enumValues,
            placeholder = ad.placeholder,
            helpText = ad.helpText,
        )
    }
    return FormSchema(uid = entityType, entityType = entityType, sections = listOf(section), fields = standardFields + customFields)
}

private fun String.toFieldDataType(): FieldDataType = when (uppercase()) {
    "NUMBER" -> FieldDataType.NUMBER
    "BOOLEAN" -> FieldDataType.BOOLEAN
    "DATE" -> FieldDataType.DATE
    "ENUM" -> FieldDataType.CHOICE
    else -> FieldDataType.TEXT
}
