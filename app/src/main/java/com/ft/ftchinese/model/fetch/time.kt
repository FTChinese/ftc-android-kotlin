package com.ft.ftchinese.model.fetch

import org.threeten.bp.DateTimeException
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeParseException

/**
 * Parse ISO8601 data string.
 */
fun parseLocalDate(str: String?): LocalDate? {
    if (str.isNullOrBlank()) {
        return null
    }

    return try {
        LocalDate.parse(str)
    } catch (e: DateTimeParseException) {
        null
    }
}

fun formatLocalDate(d: LocalDate?): String? {
    return try {
        d?.format(DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (e: DateTimeException) {
        null
    }
}

fun formatLocalDate(isoDate: ZonedDateTime): String {
    return isoDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
}

/**
 * Parses ISO8601 date time string in UTC.
 * @throws DateTimeParseException
 */
fun parseISODateTime(str: String?): ZonedDateTime? {
    if (str.isNullOrBlank()) {
        return null
    }
    return try {
        ZonedDateTime
                .parse(str, DateTimeFormatter.ISO_DATE_TIME)
    } catch (e: DateTimeParseException) {
        null
    }
}

fun formatISODateTime(dt: ZonedDateTime?): String? {
    return try {
        dt?.format(DateTimeFormatter.ISO_DATE_TIME)
    } catch (e: DateTimeException) {
        null
    }
}

fun formatSQLDateTime(dt: LocalDateTime): String {
    return try {
        dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    } catch (e: Exception) {
        ""
    }
}
