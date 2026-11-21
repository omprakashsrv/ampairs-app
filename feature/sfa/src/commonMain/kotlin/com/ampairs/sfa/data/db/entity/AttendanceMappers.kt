package com.ampairs.sfa.data.db.entity

import com.ampairs.sfa.domain.model.Attendance

fun AttendanceEntity.toAttendance(): Attendance = Attendance(
    uid = id, repMemberUid = repMemberUid, checkInAt = checkInAt, checkInLatitude = checkInLatitude,
    checkInLongitude = checkInLongitude, checkOutAt = checkOutAt, checkOutLatitude = checkOutLatitude,
    checkOutLongitude = checkOutLongitude, status = status, active = active,
    createdAt = createdAt, updatedAt = updatedAt,
)

fun Attendance.toEntity(): AttendanceEntity = AttendanceEntity(
    id = uid, repMemberUid = repMemberUid, checkInAt = checkInAt, checkInLatitude = checkInLatitude,
    checkInLongitude = checkInLongitude, checkOutAt = checkOutAt, checkOutLatitude = checkOutLatitude,
    checkOutLongitude = checkOutLongitude, status = status, active = active,
    createdAt = createdAt, updatedAt = updatedAt,
)
