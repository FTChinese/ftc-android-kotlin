package com.ft.ftchinese.ui.subs.ftcpay

import android.content.res.Resources
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.paywall.CartItemFtc
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.request.OrderParams
import com.ft.ftchinese.repository.FtcPayClient
import com.ft.ftchinese.store.PayIntentStore
import com.ft.ftchinese.ui.base.ConnectionState
import com.ft.ftchinese.ui.base.connectivityState
import com.ft.ftchinese.ui.components.BaseState
import com.ft.ftchinese.ui.repo.PaywallRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class FtcPayState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    resources: Resources,
    connState: State<ConnectionState>,
) : BaseState(scaffoldState, scope, resources, connState) {

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

    fun createOrder(account: Account, payMethod: PayMethod, store: PayIntentStore) {
        val item = cartItem ?: return
        if (!ensureConnected()) {
            return
        }

        val params = OrderParams(
            priceId = item.price.id,
            discountId = item.discount?.id
        )

        when (payMethod) {
            PayMethod.ALIPAY -> {
                createAliOrder(account, params, store)
            }
            PayMethod.WXPAY -> {
                createWxOrder(account, params, store)
            }
            else -> {
                showSnackBar(R.string.toast_no_pay_method)
            }
        }
    }

    private fun createWxOrder(account: Account, params: OrderParams, store: PayIntentStore) {
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
                    store.save(result.data.toPayIntent())
                    paymentIntent = OrderResult.WxPay(result.data)
                }
            }

            progress.value = false
        }
    }

    private fun createAliOrder(account: Account, params: OrderParams, store: PayIntentStore) {
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
                    store.save(result.data.toPayIntent())
                    paymentIntent = OrderResult.AliPay(result.data)
                }
            }

            progress.value = false
        }
    }
}


@Composable
fun rememberFtcPaySate(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    scope: CoroutineScope = rememberCoroutineScope(),
    resources: Resources = LocalContext.current.resources,
    connState: State<ConnectionState> = connectivityState()
) = remember(scaffoldState, resources, connState) {
    FtcPayState(
        scaffoldState = scaffoldState,
        scope = scope,
        resources = resources,
        connState = connState
    )
}
