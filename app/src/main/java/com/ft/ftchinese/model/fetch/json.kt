package com.ft.ftchinese.model.fetch

import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue
import com.beust.klaxon.Klaxon
import com.ft.ftchinese.model.content.ArticleType
import com.ft.ftchinese.model.enums.*
import com.ft.ftchinese.model.order.*
import com.ft.ftchinese.model.reader.LoginMethod
import com.ft.ftchinese.model.reader.UnlinkAnchor
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.ChronoUnit

@Target(AnnotationTarget.FIELD)
annotation class KDate

@Target(AnnotationTarget.FIELD)
annotation class KDateTime

@Target(AnnotationTarget.FIELD)
annotation class KTier

@Target(AnnotationTarget.FIELD)
annotation class KCycle

@Target(AnnotationTarget.FIELD)
annotation class KPriceSource

@Target(AnnotationTarget.FIELD)
annotation class KCarryOverSource

@Target(AnnotationTarget.FIELD)
annotation class KPayMethod

@Target(AnnotationTarget.FIELD)
annotation class KLoginMethod

@Target(AnnotationTarget.FIELD)
annotation class KUnlinkAnchor

@Target(AnnotationTarget.FIELD)
annotation class KOrderUsage

@Target(AnnotationTarget.FIELD)
annotation class KStripeSubStatus

@Target(AnnotationTarget.FIELD)
annotation class KPaymentIntentStatus

@Target(AnnotationTarget.FIELD)
annotation class KArticleType

val dateConverter = object : Converter {
    override fun canConvert(cls: Class<*>): Boolean {
        return cls == LocalDate::class.java
    }

    override fun fromJson(jv: JsonValue): Any? {
        val str = jv.string ?: return null

        return if (str.isNotBlank()) {
            LocalDate.parse(str, DateTimeFormatter.ISO_LOCAL_DATE)
        } else {
            null
        }
    }

    override fun toJson(value: Any): String {
        return if (value is LocalDate) {
            """ "${value.format(DateTimeFormatter.ISO_LOCAL_DATE)}" """
        } else {
            """null"""
        }

    }

}

val dateTimeConverter = object : Converter {
    override fun canConvert(cls: Class<*>): Boolean {
        return cls == ZonedDateTime::class.java
    }

    override fun fromJson(jv: JsonValue): Any? {
        val str = jv.string ?: return null

        return if (str.isNotBlank()) {
            ZonedDateTime
                    .parse(str, DateTimeFormatter.ISO_DATE_TIME)
        } else {
            null
        }
    }

    override fun toJson(value: Any): String {
        return if (value is ZonedDateTime) {
            """ "${value
                    .truncatedTo(ChronoUnit.SECONDS)
                    .withZoneSameInstant(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_DATE_TIME)}" """

        } else {
            """null"""
        }
    }
}

val tierConverter = object : Converter {
    override fun canConvert(cls: Class<*>): Boolean {
        return  cls == Tier::class.java
    }

    override fun fromJson(jv: JsonValue): Any? {
        return Tier.fromString(jv.string)
    }

    override fun toJson(value: Any): String {
        return if (value is Tier) {
            """ "$value" """
        } else {
            """null"""
        }
    }
}

val cycleConverter = object : Converter {
    override fun canConvert(cls: Class<*>): Boolean {
        return cls == Cycle::class.java
    }

    override fun fromJson(jv: JsonValue): Any? {
        return Cycle.fromString(jv.string)
    }

    override fun toJson(value: Any): String {
        return if (value is Cycle) {
            """ "$value" """
        } else {
            """null"""
        }
    }
}

val priceSourceConverter = object : Converter {
    override fun canConvert(cls: Class<*>): Boolean {
        return cls == PriceSource::class.java
    }

    override fun fromJson(jv: JsonValue): Any? {
        return PriceSource.fromString(jv.string)
    }

    override fun toJson(value: Any): String {
        return if (value is PriceSource) {
            """
                "$value"
            """.trimIndent().trim()
        } else {
            """null"""
        }
    }
}

val carryOverSourceConverter = object : Converter {
    override fun canConvert(cls: Class<*>): Boolean {
        return cls == CarryOverSource::class.java
    }

    override fun fromJson(jv: JsonValue): Any? {
        return CarryOverSource.fromString(jv.string)
    }

    override fun toJson(value: Any): String {
        return if (value is CarryOverSource) {
            """
                "$value"
            """.trimIndent().trim()
        } else {
            """null"""
        }
    }
}

val orderUsageConverter = object : Converter {
    override fun canConvert(cls: Class<*>): Boolean {
        return cls == OrderKind::class.java
    }

    override fun fromJson(jv: JsonValue): Any? {
        return jv.string?.let {
            OrderKind.fromString(it)
        }
    }

    override fun toJson(value: Any): String {
        return if (value is OrderKind) {
            """ "$value" """
        } else {
            """null"""
        }
    }
}

val payMethodConverter = object : Converter {
    override fun canConvert(cls: Class<*>): Boolean {
        return cls == PayMethod::class.java
    }

    override fun fromJson(jv: JsonValue): Any? {
        return PayMethod.fromString(jv.string)
    }

    override fun toJson(value: Any): String {
        return if (value is PayMethod) {
            """ "$value" """
        } else {
            """null"""
        }
    }
}

val loginMethodConverter = object : Converter {
    override fun canConvert(cls: Class<*>): Boolean {
        return cls == LoginMethod::class.java
    }

    override fun fromJson(jv: JsonValue): Any? {
        return LoginMethod.fromString(jv.string)
    }

    override fun toJson(value: Any): String {
        return if (value is LoginMethod) {
            """ "${value.string()}" """
        } else {
            """null"""
        }
    }
}

val unlinkAnchorConverter = object : Converter {
    override fun canConvert(cls: Class<*>): Boolean {
        return cls == UnlinkAnchor::class.java
    }

    override fun fromJson(jv: JsonValue): Any? {
        return UnlinkAnchor.fromString(jv.string)
    }

    override fun toJson(value: Any): String {
        return if (value is UnlinkAnchor) {
            """ "${value.string()}" """
        } else {
            """null"""
        }
    }
}

val stripeSubStatusConverter = object : Converter {
    override fun canConvert(cls: Class<*>): Boolean {
        return cls == StripeSubStatus::class.java
    }

    override fun fromJson(jv: JsonValue): Any? {
        return StripeSubStatus.fromString(jv.string)
    }

    override fun toJson(value: Any): String {
        return if (value is StripeSubStatus) {
            """ "$value" """
        } else {
            """null"""
        }
    }
}

val paymentIntentStatusConverter = object : Converter
{
    override fun canConvert(cls: Class<*>): Boolean {
        return cls == PaymentIntentStatus::class.java
    }

    override fun fromJson(jv: JsonValue): Any? {
        return PaymentIntentStatus.fromString(jv.string)
    }

    override fun toJson(value: Any): String {
        return if (value is PaymentIntentStatus) {
            """ "$value" """
        } else {
            """null"""
        }
    }
}

val articleTypeConverter = object : Converter {
    override fun canConvert(cls: Class<*>): Boolean {
        return cls == ArticleType::class.java
    }

    override fun fromJson(jv: JsonValue): Any? {
        return ArticleType.fromString(jv.string)
    }

    override fun toJson(value: Any): String {
        return if (value is ArticleType) {
            """ "$value" """
        } else {
            """null"""
        }
    }
}

val json = Klaxon()
    .fieldConverter(KDate::class, dateConverter)
    .fieldConverter(KDateTime::class, dateTimeConverter)
    .fieldConverter(KTier::class, tierConverter)
    .fieldConverter(KCycle::class, cycleConverter)
    .fieldConverter(KOrderUsage::class, orderUsageConverter)
    .fieldConverter(KPayMethod::class, payMethodConverter)
    .fieldConverter(KLoginMethod::class, loginMethodConverter)
    .fieldConverter(KUnlinkAnchor::class, unlinkAnchorConverter)
    .fieldConverter(KStripeSubStatus::class, stripeSubStatusConverter)
    .fieldConverter(KPaymentIntentStatus::class, paymentIntentStatusConverter)
    .fieldConverter(KArticleType::class, articleTypeConverter)
    .fieldConverter(KPriceSource::class, priceSourceConverter)
    .fieldConverter(KCarryOverSource::class, carryOverSourceConverter)
