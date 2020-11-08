package com.ft.ftchinese.model.subscription

import com.ft.ftchinese.model.reader.Membership
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.ChronoUnit

data class ConfirmationResult (
        val order: Order,
        val membership: Membership
)
