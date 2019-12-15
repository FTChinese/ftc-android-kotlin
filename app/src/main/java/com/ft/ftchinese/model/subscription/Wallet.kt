package com.ft.ftchinese.model.subscription

import android.os.Parcelable
import com.ft.ftchinese.util.KDateTime
import kotlinx.android.parcel.Parcelize
import org.threeten.bp.ZonedDateTime

@Parcelize
data class Wallet(
        val balance: Double = 0.0,
        @KDateTime
        val createdAt: ZonedDateTime = ZonedDateTime.now()
) : Parcelable
