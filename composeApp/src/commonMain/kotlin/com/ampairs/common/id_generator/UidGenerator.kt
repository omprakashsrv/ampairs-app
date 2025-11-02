package com.ampairs.common.id_generator

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Common UID Generator following backend BaseDomain pattern
 * Generates UIDs with format: prefix + datetime + random alphanumeric = 32 characters total
 */
@OptIn(ExperimentalTime::class)
object UidGenerator {

    private const val TOTAL_LENGTH = 32
    private const val ALLOWED_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

    /**
     * Generate a unique ID with specified prefix following backend pattern:
     * - Starts with custom prefix (variable length)
     * - Includes datetime component (14 chars: YYYYMMDDHHMMSS)
     * - Fills remaining with random alphanumeric
     * - Total length: 32 characters
     *
     * @param prefix The prefix to use (e.g., "CUS", "PRD", "ORD", "INV")
     * @return Generated UID with format: {prefix}{datetime}{random}
     *
     * Example: generateUid("CUS") -> CUS20250123143045A1B2C3D4E5F6G7H
     */
    fun generateUid(prefix: String): String {
        require(prefix.isNotBlank()) { "Prefix cannot be blank" }
        require(prefix.length < TOTAL_LENGTH - 14) {
            "Prefix '${prefix}' is too long. Maximum allowed: ${TOTAL_LENGTH - 15} characters"
        }
        require(prefix.all { it.isLetterOrDigit() }) {
            "Prefix '${prefix}' must contain only alphanumeric characters"
        }

        val dateTimeComponent = generateDateTimeComponent()
        val remainingLength = TOTAL_LENGTH - prefix.length - dateTimeComponent.length
        val randomComponent = generateRandomComponent(remainingLength)

        return prefix.uppercase() + dateTimeComponent + randomComponent
    }

    /**
     * Generate datetime component in format: YYYYMMDDHHMMSS (14 characters)
     */
    private fun generateDateTimeComponent(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

        return buildString {
            append(now.year.toString().padStart(4, '0'))
            append(now.monthNumber.toString().padStart(2, '0'))
            append(now.dayOfMonth.toString().padStart(2, '0'))
            append(now.hour.toString().padStart(2, '0'))
            append(now.minute.toString().padStart(2, '0'))
            append(now.second.toString().padStart(2, '0'))
        }
    }

    /**
     * Generate random alphanumeric component
     */
    private fun generateRandomComponent(length: Int): String {
        return buildString {
            repeat(length) {
                append(ALLOWED_CHARS[Random.nextInt(ALLOWED_CHARS.length)])
            }
        }
    }
}