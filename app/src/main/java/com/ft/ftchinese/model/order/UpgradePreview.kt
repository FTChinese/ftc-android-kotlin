package com.ft.ftchinese.model.order

data class UpgradePreview (
        val id: String,
        val balance: Double,
        val sources: Array<String>,
        val plan: Plan
) {
    fun isPayRequired(): Boolean {
        return plan.netPrice != 0.0
    }
}
