package com.ampairs.notification.data.db.entity

import com.ampairs.notification.domain.model.AppNotification

// Entity <-> domain mappers. The @Entity class lives in :data:database (same package); this
// mapper stays in the feature module because it references the notification domain model.

/** Convert entity to domain/wire model. */
fun NotificationLogEntity.toAppNotification(): AppNotification = AppNotification(
    uid = uid,
    type = type,
    title = title,
    body = body,
    dataPayload = dataPayload,
    read = read,
    active = active,
    updatedAt = updatedAt,
)

/** Convert domain/wire model to entity. */
fun AppNotification.toEntity(): NotificationLogEntity = NotificationLogEntity(
    uid = uid,
    type = type,
    title = title,
    body = body,
    dataPayload = dataPayload,
    read = read,
    active = active,
    updatedAt = updatedAt,
)
