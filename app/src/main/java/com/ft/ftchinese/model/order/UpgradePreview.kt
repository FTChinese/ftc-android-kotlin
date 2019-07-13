package com.ft.ftchinese.model.order

data class UpgradePreview (
        val balance: Double,
        val source: Array<String>,
        val plan: Plan
) {
    fun isPayRequired(): Boolean {
        return balance != 0.0
    }
}
