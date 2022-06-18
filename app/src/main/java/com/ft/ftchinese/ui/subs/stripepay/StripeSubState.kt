package com.ft.ftchinese.ui.subs.stripepay

import android.content.Context
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
import com.ft.ftchinese.model.stripesubs.Idempotency
import com.ft.ftchinese.model.stripesubs.StripeSubsResult
import com.ft.ftchinese.model.stripesubs.SubParams
import com.ft.ftchinese.repository.StripeClient
import com.ft.ftchinese.tracking.BeginCheckoutParams
import com.ft.ftchinese.tracking.PaySuccessParams
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.account.stripewallet.StripeWalletState
import com.ft.ftchinese.ui.util.ConnectionState
import com.ft.ftchinese.ui.util.connectivityState
import com.ft.ftchinese.ui.repo.PaywallRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class StripeSubState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    connState: State<ConnectionState>,
    context: Context
) : StripeWalletState(scaffoldState, scope, context.resources, connState) {

    private val tracker = StatsTracker.getInstance(context)
    private val idempotency = Idempotency.getInstance(context)

    fun clearIdempotency() {
        idempotency.clear()
    }

    var cartItem by mutableStateOf<CartItemStripe?>(null)
        private set

    private val isUpdate: Boolean
        get() = cartItem?.intent?.kind == IntentKind.Upgrade

    var subsResult by mutableStateOf<StripeSubsResult?>(null)
        private set

    var failure by mutableStateOf<FailureStatus?>(null)
        private set

    fun clearFailureState() {
        failure = null
    }

    fun loadCheckoutItem(
        priceId: String,
        trialId: String?,
        membership: Membership
    ) {
        progress.value = true
        cartItem = PaywallRepo.stripeCheckoutItem(
            priceId = priceId,
            trialId = trialId,
            m = membership
        )

        progress.value = false
        if (cartItem == null) {
            showSnackBar("Error finding stripe price!")
        }
    }

    fun subscribe(account: Account, itemStripe: CartItemStripe) {
        if (!ensureConnected()) {
            return
        }

        if (isUpdate) {
            idempotency.clear()
        }

        when (itemStripe.intent.kind) {
            IntentKind.Create -> {
                showSnackBar(R.string.creating_subscription)
                createSub(account, itemStripe)
            }
            IntentKind.Upgrade, IntentKind.Downgrade -> {
                showSnackBar(R.string.confirm_upgrade)
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
        val params = SubParams(
            priceId = item.recurring.id,
            introductoryPriceId = item.trial?.id,
            defaultPaymentMethod = paymentMethodSelected?.id,
            idempotency = idempotency.retrieveKey(),
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
                    handleSubsResult(result.data)
                }
            }

            progress.value = false
        }
    }

    private fun handleSubsResult(result: StripeSubsResult) {
        if (result.subs.paymentIntent?.requiresAction == false) {
            showSnackBar(R.string.subs_success)
            subsResult = result

            cartItem?.let {
                tracker.paySuccess(PaySuccessParams.ofStripe(it))
            }
            return
        }

        if (result.subs.paymentIntent?.clientSecret == null) {
            idempotency.clear()
            failure = FailureStatus.Message("订阅失败！请重试或更换支付方式")
            return
        }

        failure = FailureStatus.NextAction(result.subs.paymentIntent.clientSecret)
    }

    private fun updateSub(account: Account, item: CartItemStripe) {
        progress.value = true
        val params = SubParams(
            priceId = item.recurring.id,
            introductoryPriceId = item.trial?.id,
            defaultPaymentMethod = paymentMethodSelected?.id,
            idempotency = idempotency.retrieveKey(),
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
                    handleSubsResult(result.data)
                }
            }

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
