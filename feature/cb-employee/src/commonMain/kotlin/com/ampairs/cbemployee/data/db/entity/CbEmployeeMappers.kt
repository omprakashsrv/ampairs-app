package com.ampairs.cbemployee.data.db.entity

import com.ampairs.cbemployee.domain.model.Employee
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

// Entity <-> domain mappers. The @Entity lives in :data:database (same package). `mappedStoreIds`
// is a JSON string in Room; encode/decode it here so no Room type converter is needed.

private val json = Json { ignoreUnknownKeys = true }
private val stringListSerializer = ListSerializer(String.serializer())

private fun List<String>?.encodeOrNull(): String? =
    this?.takeIf { it.isNotEmpty() }?.let { json.encodeToString(stringListSerializer, it) }

private fun String?.decodeStringList(): List<String>? =
    this?.takeIf { it.isNotBlank() }?.let {
        runCatching { json.decodeFromString(stringListSerializer, it) }.getOrNull()
    }

fun EmployeeEntity.toEmployee(): Employee = Employee(
    uid = id,
    employeeNo = employeeNo,
    name = name,
    role = role,
    email = email,
    mobile = mobile,
    reportsToEmployeeId = reportsToEmployeeId,
    zonalOfficeId = zonalOfficeId,
    mappedStoreIds = mappedStoreIds.decodeStringList(),
    userId = userId,
    active = active,
    refId = refId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Employee.toEntity(): EmployeeEntity = EmployeeEntity(
    id = uid,
    employeeNo = employeeNo,
    name = name,
    role = role,
    email = email,
    mobile = mobile,
    reportsToEmployeeId = reportsToEmployeeId,
    zonalOfficeId = zonalOfficeId,
    mappedStoreIds = mappedStoreIds.encodeOrNull(),
    userId = userId,
    active = active,
    refId = refId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
