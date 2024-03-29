package com.ft.ftchinese.model.conversion

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.threeten.bp.DateTimeUtils
import org.threeten.bp.Instant
import org.threeten.bp.ZonedDateTime
import java.util.*

@Serializable
data class AdEvent(
    val timestamp: Double,
    @SerialName("campaign_id")
    val campaignId: Long,
) {
    // Do not call this method is attributed is false.
    fun isNotInLookBackWindow(days: Long): Boolean {
        if (days <= 0 || days >= 365) {
            return true
        }

        val startTime = ZonedDateTime
            .now()
            .plusDays(0 - days)

        val clickTime = ZonedDateTime.ofInstant(
            Instant.ofEpochSecond(timestamp.toLong()),
            DateTimeUtils.toZoneId(
                TimeZone.getDefault())
        )

        return startTime.isAfter(clickTime)
    }
}

@Serializable
data class ConversionTrackingResponse(
    @SerialName("ad_events")
    val adEvents: List<AdEvent>,
    val errors: List<String>,
    val attributed: Boolean
) {
    fun hasErrors(): Boolean {
        return errors.isNotEmpty()
    }

    fun isTimestampInvalid(): Boolean {
        return !errors.find {
            it == "timestamp_invalid"
        }
            .isNullOrBlank()
    }

    fun findLatestEvent(): AdEvent? {
        if (!attributed) {
            return null
        }

        if (adEvents.isEmpty()) {
            return null
        }

        return adEvents.maxByOrNull { it.timestamp }
    }
}
