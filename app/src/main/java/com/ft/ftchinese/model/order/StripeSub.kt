package com.ft.ftchinese.model.order

import com.ft.ftchinese.util.KDateTime
import com.ft.ftchinese.util.KStripeSubStatus
import org.threeten.bp.ZonedDateTime

/**
 * Handle initial payment outcome
 * 1. Payment success
 * 2. Payment failure
 * 3. Payment requires customer action.
 */
data class StripeSub(
        val cancelAtPeriodEnd: Boolean,
        @KDateTime
        val created: ZonedDateTime,
        @KDateTime
        val currentPeriodEnd: ZonedDateTime,
        @KDateTime
        val currentPeriodStart: ZonedDateTime,
        @KDateTime
        val endedAt: ZonedDateTime? = null,
        val latestInvoice: StripeInvoice,

        // lets us know whether we can provision the good or service associated with the subscription.
        @KStripeSubStatus
        val status: StripeSubStatus?
) {
        // The payment is complete, and you should promptly provision access to the good or service.
        fun succeeded(): Boolean {
            return status == StripeSubStatus.Active && latestInvoice.paymentIntent.status == PaymentIntentStatus.Succeeded
        }

        // Ask user to change payment method.
        fun failure(): Boolean {
                return status == StripeSubStatus.Incomplete && latestInvoice.paymentIntent.status == PaymentIntentStatus.RequiresPaymentMethod
        }

        // Some payment methods require additional steps, such as authentication, in order to complete the payment process.
        fun requiresAction(): Boolean {
                return status == StripeSubStatus.Incomplete && latestInvoice.paymentIntent.status == PaymentIntentStatus.RequiresAction
        }
}
