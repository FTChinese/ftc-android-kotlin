package com.ft.ftchinese.tracking

import com.ft.ftchinese.model.content.Language
import com.ft.ftchinese.model.content.Teaser

/**
 * Paywall might triggered from an article, or it might be
 * opened by user from drawer
 */
data class PaywallSource(
        val id: String,
        val type: String,
        val title: String,
        val language: Language? = null,
        val category: String,
        val action: String,
        val label: String
)

object PaywallTracker {
    var from: PaywallSource? = null

    fun fromArticle(item: Teaser?) {
        if (item == null) {
            from = null
            return
        }

        from = PaywallSource(
            id = item.id,
            type = item.type.toString(),
            title = item.title,
            language = item.langVariant,
            category = GACategory.SUBSCRIPTION,
            action = GAAction.DISPLAY,
            label = item.buildGALabel()
        )
    }

    fun fromDrawer() {
        from = PaywallSource(
            id = "action_subscription",
            type = "drawer",
            title = "User initiated",
            category = GACategory.SUBSCRIPTION,
            action = GAAction.DISPLAY,
            label = "drawer/action_subscription"
        )
    }

    // From MemberActivity's upgrade button.
    fun fromUpgrade() {
        from = PaywallSource(
            id = "upgrade_btn",
            type = "MemberActivity",
            title = "User initiated",
            category = GACategory.SUBSCRIPTION,
            action = GAAction.DISPLAY,
            label = "MemberActivity/upgrade_btn"
        )
    }

    fun fromRenew() {
        from = PaywallSource(
            id = "renew_btn",
            type = "MemberActivity",
            title = "User initiated",
            category = GACategory.SUBSCRIPTION,
            action = GAAction.DISPLAY,
            label = "MemberActivity/renew_btn"
        )
    }
}
