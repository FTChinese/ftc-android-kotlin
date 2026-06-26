package com.ft.ftchinese.ui.subs.stripepay

import android.content.Context
import android.util.Log
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.paywall.CartItemStripe
import com.ft.ftchinese.model.paywall.IntentKind
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.stripesubs.CouponApplied
import com.ft.ftchinese.model.stripesubs.StripeInvoicePreview
import com.ft.ftchinese.model.stripesubs.StripePaymentMethod
import com.ft.ftchinese.model.stripesubs.StripeSubsResult
import com.ft.ftchinese.repository.ApiConfig
import com.ft.ftchinese.repository.StripeClient
import com.ft.ftchinese.tracking.BeginCheckoutParams
import com.ft.ftchinese.tracking.PaySuccessParams
import com.ft.ftchinese.tracking.PaywallTracker
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.account.stripewallet.StripeWalletState
import com.ft.ftchinese.ui.repo.PaywallRepo
import com.ft.ftchinese.ui.util.ConnectionState
import com.ft.ftchinese.ui.util.connectivityState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "StripeSubState"

class StripeSubState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    connState: State<ConnectionState>,
    context: Context
) : StripeWalletState(scaffoldState, scope, context.resources, connState) {

    private val tracker = StatsTracker.getInstance(context)

    var cartItem by mutableStateOf<CartItemStripe?>(null)
        private set

    var subsResult by mutableStateOf<StripeSubsResult?>(null)
        private set

    private var checkingCoupon by mutableStateOf(false)

    var couponApplied by mutableStateOf<CouponApplied?>(null)
        private set

    var invoicePreview by mutableStateOf<StripeInvoicePreview?>(null)
        private set

    var invoicePreviewError by mutableStateOf<String?>(null)
        private set

    private var invoicePreviewKey by mutableStateOf("")
    private var previewingInvoice by mutableStateOf(false)

    val loadingState = derivedStateOf {
        progress.value || checkingCoupon || previewingInvoice
    }

    var failure by mutableStateOf<FailureStatus?>(null)
        private set

    private fun campaignFrom(ccode: String?): String? {
        return ccode?.let { "android" }
    }

    private fun effectivePaymentMethod(): StripePaymentMethod? {
        return paymentMethodInUse.value?.current
    }

    fun clearFailureState() {
        failure = null
    }

    fun loadCatalogCheckoutItem(item: CartItemStripe) {
        progress.value = true
        invoicePreview = null
        invoicePreviewError = null
        invoicePreviewKey = ""
        cartItem = item
        progress.value = false
    }

    fun loadCheckoutItem(
        priceId: String,
        trialId: String?,
        couponId: String?,
        membership: Membership
    ) {
        progress.value = true
        invoicePreview = null
        invoicePreviewError = null
        invoicePreviewKey = ""
        cartItem = PaywallRepo.stripeCheckoutItem(
            priceId = priceId,
            trialId = trialId,
            couponId = couponId,
            m = membership
        )

        progress.value = false
        if (cartItem == null) {
            showSnackBar("Error finding stripe price!")
        }
    }

    fun findCouponApplied(
        api: ApiConfig,
        ftcId: String,
        subsId: String,
    ) {
        checkingCoupon = true
        scope.launch {
            val result = StripeClient.asyncLoadCouponApplied(
                api = api,
                ftcId = ftcId,
                subsId = subsId,
            )
            when (result) {
                is FetchResult.LocalizedError -> {
//                    showSnackBar(result.msgId)
                }
                is FetchResult.TextError -> {
//                    showSnackBar(result.text)
                }
                is FetchResult.Success -> {
                    result.data.let {
                        if (it.couponId.isNotBlank()) {
                            couponApplied = it
                        }
                    }
                }
            }
            checkingCoupon = false
        }
    }

    fun previewSubscriptionUpdate(account: Account, item: CartItemStripe) {
        if (item.intent.kind != IntentKind.Upgrade) {
            invoicePreview = null
            invoicePreviewError = null
            invoicePreviewKey = ""
            return
        }

        if (!ensureConnected()) {
            invoicePreview = null
            invoicePreviewError = resources.getString(R.string.stripe_invoice_preview_failed)
            return
        }

        val subsId = account.membership.stripeSubsId
        if (subsId.isNullOrBlank()) {
            invoicePreview = null
            invoicePreviewError = resources.getString(R.string.stripe_invoice_preview_failed)
            return
        }

        val nextKey = listOf(
            account.id,
            subsId,
            item.recurring.id,
            item.recurring.currency,
            item.coupon?.id ?: "",
        ).joinToString(":")

        if (nextKey == invoicePreviewKey && invoicePreview != null) {
            return
        }

        invoicePreviewKey = nextKey
        invoicePreview = null
        invoicePreviewError = null
        previewingInvoice = true
        scope.launch {
            val result = StripeClient.asyncPreviewUpdateSubs(
                account = account,
                params = item.subsParams(
                    payMethod = effectivePaymentMethod(),
                ),
            )

            when (result) {
                is FetchResult.Success -> {
                    invoicePreview = result.data
                    invoicePreviewError = null
                }
                is FetchResult.LocalizedError -> {
                    val message = resources.getString(result.msgId)
                    Log.w(TAG, "Failed to preview Stripe subscription update: $message")
                    showSnackBar(R.string.stripe_invoice_preview_failed)
                    invoicePreview = null
                    invoicePreviewError = resources.getString(R.string.stripe_invoice_preview_failed)
                }
                is FetchResult.TextError -> {
                    Log.w(TAG, "Failed to preview Stripe subscription update: ${result.text}")
                    showSnackBar(R.string.stripe_invoice_preview_failed)
                    invoicePreview = null
                    invoicePreviewError = resources.getString(R.string.stripe_invoice_preview_failed)
                }
            }
            previewingInvoice = false
        }
    }

    fun subscribe(account: Account, itemStripe: CartItemStripe) {
        if (!ensureConnected()) {
            return
        }

        when (itemStripe.intent.kind) {
            IntentKind.Create,
            IntentKind.OneTimeToAutoRenew -> {
                showSnackBar(R.string.creating_subscription)
                createSub(
                    account,
                    itemStripe,
                )
            }
            IntentKind.Upgrade,
            IntentKind.Downgrade,
            IntentKind.SwitchInterval,
            IntentKind.CancelScheduledChange,
            IntentKind.ApplyCoupon -> {
                showSnackBar(R.string.updating_subscription)
                updateSub(account, itemStripe)
            }
            else -> {
                showSnackBar(R.string.unknown_order_kind)
            }
        }
        tracker.beginCheckOut(BeginCheckoutParams.ofStripe(itemStripe))
    }

    private fun createSub(account: Account, item: CartItemStripe) {
        progress.value = true
        val ccode = PaywallTracker.campaignCcode()
        val params = item.subsParams(
            effectivePaymentMethod(),
            ccode = ccode,
            from = campaignFrom(ccode),
        )

        scope.launch {

            val result = StripeClient.asyncCreateSubs(
                account = account,
                params = params
            )

            when (result) {
                is FetchResult.LocalizedError -> {
                    showSnackBar(result.msgId)
                    tracker.payFailed(item.recurring.edition)
                }
                is FetchResult.TextError -> {
                    showSnackBar(result.text)
                    tracker.payFailed(item.recurring.edition)
                }
                is FetchResult.Success -> {
                    handleSubsResult(result.data, item)
                }
            }

            progress.value = false
        }
    }

    private fun handleSubsResult(result: StripeSubsResult, item: CartItemStripe) {
        val paymentIntent = result.subs.paymentIntent
        if (paymentIntent == null || paymentIntent.requiresAction == false) {
            showSnackBar(R.string.subs_success)
            subsResult = result

            tracker.paySuccess(PaySuccessParams.ofStripe(item))
            return
        }

        if (paymentIntent.clientSecret == null) {
            failure = FailureStatus.Message("订阅失败！请重试或更换支付方式")
            return
        }

        failure = FailureStatus.NextAction(paymentIntent.clientSecret)
    }

    private fun updateSub(account: Account, item: CartItemStripe) {
        progress.value = true
        val ccode = PaywallTracker.campaignCcode()
        val params = item.subsParams(
            payMethod = effectivePaymentMethod(),
            prorationDate = invoicePreview?.prorationDate?.takeIf {
                item.intent.kind == IntentKind.Upgrade && it > 0
            },
            ccode = ccode,
            from = campaignFrom(ccode),
        )

        scope.launch {
            val result = StripeClient.asyncUpdateSubs(
                account = account,
                params = params
            )

            when (result) {
                is FetchResult.LocalizedError -> {
                    showSnackBar(result.msgId)
                    tracker.payFailed(item.recurring.edition)
                }
                is FetchResult.TextError -> {
                    showSnackBar(result.text)
                    tracker.payFailed(item.recurring.edition)
                }
                is FetchResult.Success -> {
                    handleSubsResult(result.data, item)
                }
            }

            progress.value = false
        }
    }
}

@Composable
fun rememberStripeSubState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    connState: State<ConnectionState> = connectivityState(),
    scope: CoroutineScope = rememberCoroutineScope(),
    context: Context = LocalContext.current,
) = remember(scaffoldState, connState) {
    StripeSubState(
        scaffoldState = scaffoldState,
        scope = scope,
        connState = connState,
        context = context
    )
}
