package com.ft.ftchinese.model.stripesubs

import android.os.Parcelable
import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.serializer.DateTimeAsStringSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.threeten.bp.ZonedDateTime

@Parcelize
@Serializable
data class StripePendingChange(
    val kind: String = "",
    val scheduleId: String = "",
    val targetTier: Tier? = null,
    val targetCycle: Cycle? = null,
    val targetPriceId: String = "",
    @Serializable(with = DateTimeAsStringSerializer::class)
    val effectiveAt: ZonedDateTime? = null,
) : Parcelable {
    val isDowngrade: Boolean
        get() = kind.equals("downgrade", ignoreCase = true)

    fun targets(tier: Tier?): Boolean {
        return targetTier != null && targetTier == tier
    }
}
