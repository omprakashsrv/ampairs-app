package com.ampairs.payment.agent

import androidx.room3.Dao
import androidx.room3.Query

/**
 * Read-only aggregate queries that back the assistant's curated payment reports — kept **separate**
 * from the operational [com.ampairs.payment.data.db.dao.PaymentVoucherDao] so report concerns don't
 * leak into the CRUD/sync DAO. Queries are over the existing `payment_voucher` table (no schema
 * impact).
 *
 * `voucher_date` is stored as an ISO-8601 instant string (Instant.toString()), so the period bounds
 * must also be ISO instants — lexical comparison over ISO-8601 UTC is the correct order.
 */
@Dao
interface PaymentAgentDao {

    /** Total payments by direction ("RECEIVED"/"PAID"), all time, in minor units (paise). */
    @Query("SELECT COALESCE(SUM(total_minor), 0) FROM payment_voucher WHERE direction = :direction AND active = 1")
    suspend fun sumByDirection(direction: String): Long

    /** Total payments by direction within a half-open period, in minor units (paise). */
    @Query(
        "SELECT COALESCE(SUM(total_minor), 0) FROM payment_voucher " +
            "WHERE direction = :direction AND active = 1 AND voucher_date >= :start AND voucher_date < :end",
    )
    suspend fun sumByDirectionBetween(direction: String, start: String, end: String): Long
}
