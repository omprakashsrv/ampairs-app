package com.ampairs.common.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Instant

/**
 * Serializer for [kotlin.time.Instant] that accepts backend timestamps as either an ISO 8601
 * string (e.g. "2026-05-25T17:01:04.083622Z") or an epoch-millis number, and always emits the
 * ISO 8601 form the backend expects.
 *
 * Use this for every timestamp field on a serializable model instead of representing time as a
 * raw `Long`.
 */
object InstantSerializer : KSerializer<Instant> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Instant {
        val jsonDecoder = decoder as? JsonDecoder
            ?: return Instant.parse(decoder.decodeString())

        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> when {
                element.isString -> Instant.parse(element.content)
                else -> element.content.toLongOrNull()?.let { Instant.fromEpochMilliseconds(it) }
                    ?: throw SerializationException("Cannot parse Instant from '${element.content}'")
            }
            else -> throw SerializationException("Expected ISO string or epoch millis for timestamp, got $element")
        }
    }
}
