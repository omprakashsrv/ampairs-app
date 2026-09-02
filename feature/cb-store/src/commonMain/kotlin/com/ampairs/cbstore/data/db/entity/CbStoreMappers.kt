package com.ampairs.cbstore.data.db.entity

import com.ampairs.cbstore.domain.model.Store
import com.ampairs.cbstore.domain.model.ZonalOffice

// Entity <-> domain mappers. The @Entity classes live in :data:database (same package); these
// mappers stay in the feature module because they reference the cb-store domain models.

fun StoreEntity.toStore(): Store = Store(
    uid = id,
    code = code,
    name = name,
    city = city,
    zonalOfficeId = zonalOfficeId,
    active = active,
    refId = refId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Store.toEntity(): StoreEntity = StoreEntity(
    id = uid,
    code = code,
    name = name,
    city = city,
    zonalOfficeId = zonalOfficeId,
    active = active,
    refId = refId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun ZonalOfficeEntity.toZonalOffice(): ZonalOffice = ZonalOffice(
    uid = id,
    name = name,
    city = city,
    active = active,
    refId = refId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun ZonalOffice.toEntity(): ZonalOfficeEntity = ZonalOfficeEntity(
    id = uid,
    name = name,
    city = city,
    active = active,
    refId = refId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
