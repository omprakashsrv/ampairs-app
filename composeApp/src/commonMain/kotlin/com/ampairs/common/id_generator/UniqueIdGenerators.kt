package com.ampairs.common.id_generator

import com.ampairs.common.id_generator.UniqueIdGenerator.UniqueIdGroup

enum class UniqueIdGenerators(uniqueIdGroup: UniqueIdGroup, allowedChars: String) {
    ALPHANUMERIC_WITH_TIME_MILLI(
        UniqueIdGroup.DATETIMEMILLI, "ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789"
    ),
    ALPHANUMERIC_WITH_TIME(
        UniqueIdGroup.DATETIME,
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789"
    ),
    ALPHANUMERIC_WITH_DATE(UniqueIdGroup.DATE, "ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789"), ALPHANUMERIC(
        UniqueIdGroup.NONE,
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789"
    ),
    ALPHABETS_WITH_TIME(UniqueIdGroup.DATETIME, "ABCDEFGHIJKLMNOPQRSTUVWXYZ"), ALPHABETS_WITH_DATE(
        UniqueIdGroup.DATE,
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    ),
    ALPHABETS(
        UniqueIdGroup.NONE,
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    ),
    NUMERIC_WITH_TIME(UniqueIdGroup.DATETIME, "123456789"), NUMERIC_WITH_DATE(
        UniqueIdGroup.DATE,
        "123456789"
    ),
    NUMERIC(UniqueIdGroup.NONE, "123456789"), UUID(
        UniqueIdGroup.UUID_GEN,
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789"
    );

    private val uniqueIdGenerator: UniqueIdGenerator

    init {
        uniqueIdGenerator = UniqueIdGenerator()
        uniqueIdGenerator.uniqueIdGroup = uniqueIdGroup
        uniqueIdGenerator.allowedChars = allowedChars
    }

    @Throws(IllegalArgumentException::class)
    fun generate(prefix: String, length: Int): String {
        return uniqueIdGenerator.generate(prefix, length)
    }

    @Throws(IllegalArgumentException::class)
    fun generate(length: Int): String {
        return uniqueIdGenerator.generate(length)
    }
}
