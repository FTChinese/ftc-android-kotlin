package com.ft.ftchinese.model.subscription

import android.os.Parcelable
import com.ft.ftchinese.model.fetch.KDateTime
import kotlinx.parcelize.Parcelize
import org.threeten.bp.ZonedDateTime

@Parcelize
data class Wallet(
        val balance: Double = 0.0,
        @KDateTime
        val createdAt: ZonedDateTime = ZonedDateTime.now()
) : Parcelable
