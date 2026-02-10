package com.ft.ftchinese.model.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

object LenientBooleanSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("LenientBoolean", PrimitiveKind.BOOLEAN)

    override fun deserialize(decoder: Decoder): Boolean {
        if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            if (element is JsonPrimitive) {
                element.booleanOrNull?.let { return it }
                element.intOrNull?.let { return it != 0 }
                element.longOrNull?.let { return it != 0L }
                if (element.isString) {
                    val s = element.content.trim().lowercase()
                    return when (s) {
                        "true", "1", "yes", "y" -> true
                        "false", "0", "no", "n", "" -> false
                        else -> s.toIntOrNull()?.let { it != 0 } ?: false
                    }
                }
            }
            return false
        }

        return decoder.decodeBoolean()
    }

    override fun serialize(encoder: Encoder, value: Boolean) {
        encoder.encodeBoolean(value)
    }
}
