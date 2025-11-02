package com.ampairs.common.id_generator

import com.benasher44.uuid.uuid4
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class UniqueIdGenerator {
    /**
     * Get the uniqueIdGroup.
     *
     * @return the uniqueIdGroup
     */
    /**
     * Set the uniqueIdGroup.
     *
     * @param uniqueIdGroup the uniqueIdGroup to set
     */
    var uniqueIdGroup = UniqueIdGroup.DATETIME
    /**
     * Get the allowedChars.
     *
     * @return the allowedChars
     */
    /**
     * Set the allowedChars.
     *
     * @param allowedChars the allowedChars to set
     */
    var allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789"

    /**
     * Generate a random string
     *
     * @return generated random string
     * @throws IllegalArgumentException
     */
    @Throws(IllegalArgumentException::class, IllegalStateException::class)
    fun generate(length: Int): String {
        return generate("", length)
    }

    /**
     * Generate a random string starting from the prefix, and having 'length' size
     *
     * @param prefix
     * @param length
     * @return generated random string
     * @throws IllegalArgumentException
     */
    @Throws(IllegalArgumentException::class, IllegalStateException::class)
    fun generate(prefix: String, length: Int): String {
        val base = uniqueIdGroup.value
        val builder = StringBuilder(prefix)
        builder.append(base)
        require(length >= builder.length + 1) {
            ("Cannot generate random string of length " + length + " for GeneratorType: " + uniqueIdGroup.value)
        }
        appendRandomString(builder, length)
        return builder.substring(0, length).uppercase()
    }

    /**
     * Populates builder with random characters selected from allowedChars having max length
     *
     * @param builder
     * @param length
     */
    @Throws(IllegalStateException::class)
    fun appendRandomString(builder: StringBuilder, length: Int) {
        check(allowedChars.isNotEmpty()) { "AllowedChars cannot be empty string" }
        while (builder.length < length) {
            val uuid = uuid4()
            val uuidString = uuid.toString()
            builder.append(uuidString)
        }
    }

    enum class UniqueIdGroup {
        DATE {
            override val value: String
                get() {
                    val dateChars = setOf("-")
                    var dateTimeString = Clock.System.now()
                        .toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
                    dateTimeString = dateTimeString.substring(0, dateTimeString.indexOf("T"))
                    return dateTimeString.filterNot {
                        dateChars.contains(it.toString())
                    }
                }
        },
        DATETIME {
            override val value: String
                get() {
                    val dateChars = setOf("-", ":", "T")
                    var dateTimeString =
                        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                            .toString()
                    dateTimeString = dateTimeString.substring(0, dateTimeString.indexOf("."))
                    return dateTimeString.filterNot {
                        dateChars.contains(it.toString())
                    }
                }
        },
        DATETIMEMILLI {
            override val value: String
                get() {
                    val dateChars = setOf("-", ":", "T")
                    return Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                        .toString().filterNot {
                            dateChars.contains(it.toString())
                        }
                }
        },
        NONE {

            override val value: String
                get() = ""
        },
        UUID_GEN {

            override val value: String
                get() = uuid4().toString().replace("-".toRegex(), "")

        };

        abstract val value: String
    }
}
