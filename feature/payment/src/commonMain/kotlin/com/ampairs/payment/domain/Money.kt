package com.ampairs.payment.domain

import kotlin.jvm.JvmInline
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Exact fixed-scale money value type for the party ledger (spec 013, research.md R5).
 *
 * Stored as [Long] **minor units** (paise — 1/100 of the major unit). Integer arithmetic is exact,
 * so a ledger built from [Money] always foots to zero with **no floating-point drift** (SC-006) and
 * is KMP-safe in `commonMain` (no `BigDecimal` on Kotlin/Native).
 *
 * Conversions to/from `Double` (legacy invoice/order totals) and decimal strings (the API wire
 * format, which is `DECIMAL(19,4)` on the backend) round **half-up to 2 dp** at the boundary. Do an
 * invoice's `Double` → [Money] conversion exactly **once**, at posting time.
 */
@JvmInline
value class Money(val minor: Long) : Comparable<Money> {

    val isZero: Boolean get() = minor == 0L
    val isPositive: Boolean get() = minor > 0L
    val isNegative: Boolean get() = minor < 0L

    operator fun plus(other: Money): Money = Money(minor + other.minor)
    operator fun minus(other: Money): Money = Money(minor - other.minor)
    operator fun unaryMinus(): Money = Money(-minor)

    fun abs(): Money = Money(abs(minor))

    /** Apply a sign: DR ⇒ +, CR ⇒ − (matches the balance-math single source of truth). */
    fun signed(direction: Direction): Money =
        if (direction == Direction.DR) abs() else Money(-abs().minor)

    override fun compareTo(other: Money): Int = minor.compareTo(other.minor)

    /** Whole + 2-decimal value as a `Double` (display / legacy interop only — never for ledger math). */
    fun toDouble(): Double = minor / 100.0

    /** Decimal string with exactly 2 dp for the API boundary, e.g. `4000` → `"40.00"`. */
    fun toDecimalString(): String {
        val negative = minor < 0L
        val cents = abs(minor)
        val whole = cents / 100L
        val fraction = (cents % 100L).toString().padStart(2, '0')
        return (if (negative) "-" else "") + whole.toString() + "." + fraction
    }

    companion object {
        val ZERO = Money(0L)

        /** Half-up to 2 dp. e.g. `40.005` → `4001` minor units. */
        fun fromDouble(value: Double): Money =
            Money((value * 100.0).roundToLong())

        fun fromDouble(value: Double?): Money = if (value == null) ZERO else fromDouble(value)

        /**
         * Parse a decimal string (any scale; the backend sends `DECIMAL(19,4)`), rounding half-up to
         * 2 dp. Tolerates a sign, missing fraction, and >2 fractional digits. Blank → [ZERO].
         */
        fun fromDecimalString(value: String?): Money {
            val raw = value?.trim().orEmpty()
            if (raw.isEmpty()) return ZERO
            val negative = raw.startsWith("-")
            val unsigned = raw.removePrefix("-").removePrefix("+")
            val dot = unsigned.indexOf('.')
            val wholePart: String
            val fractionPart: String
            if (dot < 0) {
                wholePart = unsigned.ifEmpty { "0" }
                fractionPart = ""
            } else {
                wholePart = unsigned.substring(0, dot).ifEmpty { "0" }
                fractionPart = unsigned.substring(dot + 1)
            }
            val whole = wholePart.toLongOrNull() ?: 0L
            // Take the first two fractional digits, then round half-up using the third.
            val twoDigits = fractionPart.padEnd(2, '0').take(2)
            val cents = twoDigits.toLongOrNull() ?: 0L
            val roundUp = fractionPart.getOrNull(2)?.let { it in '5'..'9' } ?: false
            var total = whole * 100L + cents + if (roundUp) 1L else 0L
            if (negative) total = -total
            return Money(total)
        }

        fun fromMinor(minor: Long): Money = Money(minor)
    }
}
