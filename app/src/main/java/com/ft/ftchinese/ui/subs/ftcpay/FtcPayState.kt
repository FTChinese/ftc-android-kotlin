package com.ft.ftchinese.ui.subs.ftcpay

import android.content.Context
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.ftcsubs.AliPayIntent
import com.ft.ftchinese.model.ftcsubs.ConfirmationParams
import com.ft.ftchinese.model.paywall.CartItemFtc
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.request.OrderParams
import com.ft.ftchinese.repository.FtcPayClient
import com.ft.ftchinese.store.InvoiceStore
import com.ft.ftchinese.store.PayIntentStore
import com.ft.ftchinese.tracking.BeginCheckoutParams
import com.ft.ftchinese.tracking.PaySuccessParams
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.util.ConnectionState
import com.ft.ftchinese.ui.util.connectivityState
import com.ft.ftchinese.ui.components.BaseState
import com.ft.ftchinese.ui.repo.PaywallRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FtcPayState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    connState: State<ConnectionState>,
    context: Context
) : BaseState(scaffoldState, scope, context.resources, connState) {

    private val tracker = StatsTracker.getInstance(context)
    private val payIntentStore = PayIntentStore.getInstance(context)
    private val invoiceStore = InvoiceStore.getInstance(context)

    var membershipUpdated by mutableStateOf<Membership?>(null)
        private set

    var cartItem by mutableStateOf<CartItemFtc?>(null)
        private set

    var paymentIntent by mutableStateOf<OrderResult?>(null)
        private set

    fun loadFtcCheckoutItem(
        priceId: String,
        membership: Membership
    ) {
        progress.value = true
        cartItem = PaywallRepo.ftcCheckoutItem(
            priceId = priceId,
            m = membership,
        )
        progress.value = false
    }

    fun createOrder(account: Account, payMethod: PayMethod, item: CartItemFtc) {
        if (!ensureConnected()) {
            return
        }

        val params = OrderParams(
            priceId = item.price.id,
            discountId = item.discount?.id
        )

        scope.launch(Dispatchers.IO) {
            tracker.beginCheckOut(
                BeginCheckoutParams.ofFtc(
                    item = item,
                    method = payMethod
                )
            )
        }

        when (payMethod) {
            PayMethod.ALIPAY -> {
                createAliOrder(account, params)
            }
            PayMethod.WXPAY -> {
                createWxOrder(account, params)
            }
            else -> {
                showSnackBar(R.string.toast_no_pay_method)
            }
        }
    }

    private fun createWxOrder(account: Account, params: OrderParams) {
        progress.value = true
        scope.launch {
            val result = FtcPayClient.asyncCreateWxOrder(
                account = account,
                params = params
            )

            when (result) {
                is FetchResult.LocalizedError -> {
                    showSnackBar(result.msgId)
                }
                is FetchResult.TextError -> {
                    showSnackBar(result.text)
                }
                is FetchResult.Success -> {
                    payIntentStore.save(result.data.toPayIntent())
                    paymentIntent = OrderResult.WxPay(result.data)
                }
            }

            progress.value = false
        }
    }

    private fun createAliOrder(account: Account, params: OrderParams) {
        progress.value = true
        scope.launch {
            val result = FtcPayClient.asyncCreateAliOrder(
                account = account,
                params = params
            )

            when (result) {
                is FetchResult.LocalizedError -> {
                    showSnackBar(result.msgId)
                }
                is FetchResult.TextError -> {
                    showSnackBar(result.text)
                }
                is FetchResult.Success -> {
                    payIntentStore.save(result.data.toPayIntent())
                    paymentIntent = OrderResult.AliPay(result.data)
                }
            }

            progress.value = false
        }
    }

    fun handleAliPayResult(
        account: Account,
        aliPayIntent: AliPayIntent,
        payResult: Map<String, String>
    ) {
        progress.value = true

        val resultStatus = payResult["resultStatus"]

        if (resultStatus != "9000") {
            val msg = payResult["memo"] ?: resources.getString(R.string.wxpay_failed)
            showSnackBar(msg)
            tracker.payFailed(aliPayIntent.price.edition)

            progress.value = false
            return
        }

        // Confirm on device.
        val currentMember = account.membership
        val confirmed = ConfirmationParams(
            order = aliPayIntent.order,
            member = currentMember
        ).buildResult()

        invoiceStore.save(confirmed)
        val pi = aliPayIntent.toPayIntent()
        payIntentStore.save(pi.withConfirmed(confirmed.order))

        showSnackBar(R.string.subs_success)
        progress.value = false

        membershipUpdated = confirmed.membership

        tracker.paySuccess(PaySuccessParams.ofFtc(pi))
    }
}


@Composable
fun rememberFtcPaySate(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    connState: State<ConnectionState> = connectivityState(),
    scope: CoroutineScope = rememberCoroutineScope(),
    context: Context = LocalContext.current,
) = remember(scaffoldState, connState) {
    FtcPayState(
        scaffoldState = scaffoldState,
        scope = scope,
        connState = connState,
        context = context
    )
}
