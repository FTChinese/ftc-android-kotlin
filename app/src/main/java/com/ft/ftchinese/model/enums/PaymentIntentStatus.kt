package com.ft.ftchinese.model.enums

enum class PaymentIntentStatus(val symbol: String) {
    RequiresPaymentMethod("requires_payment_method"),
    RequiresConfirmation("requires_confirmation"),
    RequiresAction("requires_action"),
    Processing("processing"),
    RequiresCapture("requires_capture"),
    Canceled("canceled"),
    Succeeded("succeeded");

    override fun toString(): String {
        return symbol
    }

    companion object {
        private val stringToEnum: Map<String, PaymentIntentStatus> = values().associateBy { it.symbol }

        @JvmStatic
        fun fromString(symbol: String?): PaymentIntentStatus? {
            return if (symbol == null) null else stringToEnum[symbol]
        }
    }
}
