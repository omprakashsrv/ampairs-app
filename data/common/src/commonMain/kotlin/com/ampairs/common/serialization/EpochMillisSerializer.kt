package com.ampairs.common.serialization

import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

/**
 * Serializer that accepts backend timestamps as either epoch-millis Long or ISO 8601 String
 * and always deserializes to epoch-millis Long.
 *
 * Needed because some backend DTOs serialize Instant fields as ISO 8601 strings
 * (e.g. "2026-05-25T17:01:04.083622Z") instead of epoch milliseconds.
 */
object EpochMillisSerializer : KSerializer<Long> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("EpochMillis")

    override fun serialize(encoder: Encoder, value: Long) {
        encoder.encodeLong(value)
    }

    override fun deserialize(decoder: Decoder): Long {
        val jsonDecoder = decoder as? JsonDecoder
            ?: return decoder.decodeLong()

        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> when {
                element.isString -> Instant.parse(element.content).toEpochMilliseconds()
                else -> element.content.toLongOrNull()
                    ?: throw SerializationException("Cannot parse epoch millis from '${element.content}'")
            }
            else -> throw SerializationException("Expected Long or ISO string for timestamp, got $element")
        }
    }
}
