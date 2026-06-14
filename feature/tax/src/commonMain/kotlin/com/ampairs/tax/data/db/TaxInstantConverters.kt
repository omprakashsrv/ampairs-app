package com.ampairs.tax.data.db

import androidx.room.TypeConverter
import kotlin.time.Instant

/**
 * Room converters for [kotlin.time.Instant].
 *
 * Time is stored on-disk as epoch-millis (INTEGER) so the column affinity is unchanged from the
 * previous `Long` columns — no schema migration is required — while all entity/DAO declarations use
 * [Instant] instead of a raw `Long`.
 */
object TaxInstantConverters {

    @TypeConverter
    fun instantToEpochMillis(instant: Instant?): Long? = instant?.toEpochMilliseconds()

    @TypeConverter
    fun epochMillisToInstant(millis: Long?): Instant? = millis?.let { Instant.fromEpochMilliseconds(it) }
}
