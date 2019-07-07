package com.ft.ftchinese.model.order

import android.os.Parcelable
import com.ft.ftchinese.util.*
import kotlinx.android.parcel.Parcelize
import org.threeten.bp.LocalDate
import org.threeten.bp.ZonedDateTime

/**
 * An order created from server when user pressed pay button.
 */
@Parcelize
class Order(
        val id: String,
        val listPrice: Double,
        val netPrice: Double,

        @KTier
        val tier: Tier,

        @KCycle
        val cycle: Cycle,

        val cycleCount: Long = 1,
        val extraDays: Long = 0,

        @KOrderUsage
        val usageType: OrderUsage?,

        val balance: Double? = null,

        @KPayMethod
        val payMethod: PayMethod? = null,

        @KDateTime
        val createdAt: ZonedDateTime = ZonedDateTime.now(),

        @KDateTime
        val confirmedAt: ZonedDateTime? = null,

        @KDate
        var startDate: LocalDate? = null,

        @KDate
        var endDate: LocalDate? = null
) : Parcelable {

        fun priceInCent(): Long {
                return (netPrice*100).toLong()
        }
}
