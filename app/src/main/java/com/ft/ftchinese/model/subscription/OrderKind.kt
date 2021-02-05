package com.ft.ftchinese.model.subscription

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class OrderKind(val code: String) : Parcelable {
    CREATE("create"),
    RENEW("renew"),
    UPGRADE("upgrade"),
    ADD_ON("add_on");

    override fun toString(): String {
        return code
    }

    companion object{

        private val STRING_TO_ENUM: Map<String, OrderKind> = values().associateBy {
            it.code
        }

        @JvmStatic
        fun fromString(symbol: String?): OrderKind? {
            if (symbol == null) {
                return null
            }
            return STRING_TO_ENUM[symbol]
        }
    }
}

enum class SubsKind {
    Create, // Also applicable to expired membership.
    Renew,
    SwitchToYear,
    UpgradeToPrm,
    StdAddOn,
    PremAddOn
}

/**
 * Describe what kind of action an member could take for
 * next step.
 */
data class NextSteps(
    val subsKinds: List<SubsKind>,
    val message: String? = null,
) {
    val canReSubscribe: Boolean
        get() = subsKinds.contains(SubsKind.Create)

    val canRenew: Boolean
        get() = subsKinds.contains(SubsKind.Renew)

    val canSwitchCycle: Boolean
        get() = subsKinds.contains(SubsKind.SwitchToYear)

    val canUpgradePrm: Boolean
        get() = subsKinds.contains(SubsKind.UpgradeToPrm)

    val canStdAddOn: Boolean
        get() = subsKinds.contains(SubsKind.StdAddOn)

    val canPrmAddOn: Boolean
        get() = subsKinds.contains(SubsKind.PremAddOn)
}
