package com.ft.ftchinese.ui.subs

import android.os.Parcelable
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.tracking.GAAction
import com.ft.ftchinese.tracking.GACategory
import com.ft.ftchinese.tracking.PaywallSource
import com.ft.ftchinese.tracking.PaywallTracker
import kotlinx.parcelize.Parcelize

@Parcelize
data class SubscriptionEntryIntent(
    val tier: Tier? = null,
    val ccode: String? = null,
    val from: String? = null,
    val offerHint: String? = null,
    val priceHint: String? = null,
    val sourceUri: String = "",
    val sourceScheme: String = "",
) : Parcelable {
    val premiumFirst: Boolean
        get() = tier == Tier.PREMIUM

    fun campaignCcode(): String? {
        return ccode
    }
}

fun trackSubscriptionEntry(entry: SubscriptionEntryIntent?) {
    if (entry == null) {
        return
    }

    val campaign = entry.campaignCcode()
    if (campaign.isNullOrBlank()) {
        PaywallTracker.from = null
        return
    }

    val tierLabel = entry.tier?.symbol ?: "unknown"
    PaywallTracker.from = PaywallSource(
        id = campaign,
        type = "promotion",
        title = "${entry.sourceScheme}://$tierLabel",
        category = GACategory.SUBSCRIPTION,
        action = GAAction.DISPLAY,
        label = "${entry.sourceScheme}/$tierLabel"
    )
}
