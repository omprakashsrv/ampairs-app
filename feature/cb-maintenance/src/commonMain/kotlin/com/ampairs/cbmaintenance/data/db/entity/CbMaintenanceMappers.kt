package com.ampairs.cbmaintenance.data.db.entity

import com.ampairs.cbmaintenance.domain.model.AssetCategoryAlias
import com.ampairs.cbmaintenance.domain.model.ChecklistItemResult
import com.ampairs.cbmaintenance.domain.model.PmEntry
import com.ampairs.cbmaintenance.domain.model.PmSchedule
import com.ampairs.cbmaintenance.domain.model.Ticket
import com.ampairs.cbmaintenance.domain.model.TicketBucket
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

// Entity <-> domain mappers. The @Entity classes live in :data:database (same package). JSON-list
// columns are stored as JSON strings; encode/decode here so no Room type converter is needed.

private val json = Json { ignoreUnknownKeys = true }
private val stringListSerializer = ListSerializer(String.serializer())
private val checklistSerializer = ListSerializer(ChecklistItemResult.serializer())

private fun List<String>?.encodeStrings(): String? =
    this?.takeIf { it.isNotEmpty() }?.let { json.encodeToString(stringListSerializer, it) }

private fun String?.decodeStrings(): List<String>? =
    this?.takeIf { it.isNotBlank() }?.let { runCatching { json.decodeFromString(stringListSerializer, it) }.getOrNull() }

private fun List<ChecklistItemResult>?.encodeChecklist(): String? =
    this?.takeIf { it.isNotEmpty() }?.let { json.encodeToString(checklistSerializer, it) }

private fun String?.decodeChecklist(): List<ChecklistItemResult>? =
    this?.takeIf { it.isNotBlank() }?.let { runCatching { json.decodeFromString(checklistSerializer, it) }.getOrNull() }

// --- PmSchedule ---------------------------------------------------------------------------------
fun PmScheduleEntity.toPmSchedule(): PmSchedule = PmSchedule(
    uid = id,
    department = department,
    assetCategory = assetCategory,
    taskName = taskName,
    checklist = checklist.decodeStrings(),
    frequencyUnit = frequencyUnit,
    frequencyInterval = frequencyInterval,
    active = active,
    refId = refId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun PmSchedule.toEntity(): PmScheduleEntity = PmScheduleEntity(
    id = uid,
    department = department,
    assetCategory = assetCategory,
    taskName = taskName,
    checklist = checklist.encodeStrings(),
    frequencyUnit = frequencyUnit,
    frequencyInterval = frequencyInterval,
    active = active,
    refId = refId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// --- PmEntry ------------------------------------------------------------------------------------
fun PmEntryEntity.toPmEntry(): PmEntry = PmEntry(
    uid = id,
    storeId = storeId,
    zonalOfficeId = zonalOfficeId,
    assetCategory = assetCategory,
    pmScheduleId = pmScheduleId,
    source = source,
    dueDate = dueDate,
    status = status,
    assignedToEmployeeId = assignedToEmployeeId,
    assistedByEmployeeIds = assistedByEmployeeIds.decodeStrings(),
    completedAt = completedAt,
    completedByEmployeeId = completedByEmployeeId,
    checklistResult = checklistResult.decodeChecklist(),
    ticketId = ticketId,
    active = active,
    refId = refId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun PmEntry.toEntity(): PmEntryEntity = PmEntryEntity(
    id = uid,
    storeId = storeId,
    zonalOfficeId = zonalOfficeId,
    assetCategory = assetCategory,
    pmScheduleId = pmScheduleId,
    source = source,
    dueDate = dueDate,
    status = status,
    assignedToEmployeeId = assignedToEmployeeId,
    assistedByEmployeeIds = assistedByEmployeeIds.encodeStrings(),
    completedAt = completedAt,
    completedByEmployeeId = completedByEmployeeId,
    checklistResult = checklistResult.encodeChecklist(),
    ticketId = ticketId,
    active = active,
    refId = refId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// --- Ticket -------------------------------------------------------------------------------------
fun TicketEntity.toTicket(): Ticket = Ticket(
    uid = id,
    storeId = storeId,
    zonalOfficeId = zonalOfficeId,
    assetCategory = assetCategory,
    subCategory = subCategory,
    ticketBucketId = ticketBucketId,
    description = description,
    status = status,
    assignedToEmployeeId = assignedToEmployeeId,
    assistedByEmployeeIds = assistedByEmployeeIds.decodeStrings(),
    raisedByEmployeeId = raisedByEmployeeId,
    raisedAt = raisedAt,
    resolvedAt = resolvedAt,
    originPmEntryId = originPmEntryId,
    suggestedSparePart = suggestedSparePart,
    active = active,
    refId = refId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Ticket.toEntity(): TicketEntity = TicketEntity(
    id = uid,
    storeId = storeId,
    zonalOfficeId = zonalOfficeId,
    assetCategory = assetCategory,
    subCategory = subCategory,
    ticketBucketId = ticketBucketId,
    description = description,
    status = status,
    assignedToEmployeeId = assignedToEmployeeId,
    assistedByEmployeeIds = assistedByEmployeeIds.encodeStrings(),
    raisedByEmployeeId = raisedByEmployeeId,
    raisedAt = raisedAt,
    resolvedAt = resolvedAt,
    originPmEntryId = originPmEntryId,
    suggestedSparePart = suggestedSparePart,
    active = active,
    refId = refId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// --- AssetCategoryAlias -------------------------------------------------------------------------
fun AssetCategoryAliasEntity.toAlias(): AssetCategoryAlias = AssetCategoryAlias(
    uid = id,
    canonical = canonical,
    alias = alias,
    active = active,
    refId = refId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun AssetCategoryAlias.toEntity(): AssetCategoryAliasEntity = AssetCategoryAliasEntity(
    id = uid,
    canonical = canonical,
    alias = alias,
    active = active,
    refId = refId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// --- TicketBucket -------------------------------------------------------------------------------
fun TicketBucketEntity.toTicketBucket(): TicketBucket = TicketBucket(
    uid = id,
    department = department,
    category = category,
    subCategory1 = subCategory1,
    subCategory2 = subCategory2,
    active = active,
    refId = refId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun TicketBucket.toEntity(): TicketBucketEntity = TicketBucketEntity(
    id = uid,
    department = department,
    category = category,
    subCategory1 = subCategory1,
    subCategory2 = subCategory2,
    active = active,
    synced = true,
    refId = refId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
