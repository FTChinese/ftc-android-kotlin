package com.ft.ftchinese.models

import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue
import com.beust.klaxon.Klaxon
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
annotation class KPayMethod

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
            value.format(DateTimeFormatter.ISO_LOCAL_DATE)
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
            value
                    .truncatedTo(ChronoUnit.SECONDS)
                    .withZoneSameInstant(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_DATE_TIME)
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
            value.string()
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
            value.string()
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
            value.string()
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
        .fieldConverter(KPayMethod::class, payMethodConverter)