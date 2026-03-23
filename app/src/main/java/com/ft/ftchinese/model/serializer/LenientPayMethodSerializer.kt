package com.ft.ftchinese.model.serializer

import com.ft.ftchinese.model.enums.PayMethod
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

object LenientPayMethodSerializer : KSerializer<PayMethod?> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("LenientPayMethod", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): PayMethod? {
        if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            if (element == JsonNull) {
                return null
            }
            if (element is JsonPrimitive) {
                return PayMethod.fromString(element.content.trim().lowercase())
            }
            return null
        }

        return PayMethod.fromString(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: PayMethod?) {
        if (value == null) {
            encoder.encodeNull()
            return
        }
        encoder.encodeString(value.toString())
    }
}
