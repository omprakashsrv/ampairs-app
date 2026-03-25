package com.ampairs.common.id_generator

object IdUtils {
    fun generateUniqueId(idPrefix: String, idLength: Int): String {
        return UniqueIdGenerators.ALPHANUMERIC_WITH_TIME_MILLI.generate(
            idPrefix,
            idLength
        )
    }
}
