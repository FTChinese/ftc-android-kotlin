package com.ft.ftchinese.ui.subs.stripepay

import android.content.Context
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.paywall.CartItemStripe
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.stripesubs.StripePaymentMethod
import com.ft.ftchinese.model.stripesubs.StripeSubsResult
import com.ft.ftchinese.repository.StripeClient
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.base.ConnectionState
import com.ft.ftchinese.ui.base.connectivityState
import com.ft.ftchinese.ui.components.BaseState
import com.ft.ftchinese.ui.repo.PaywallRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class StripeSubState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    connState: State<ConnectionState>,
    context: Context
) : BaseState(scaffoldState, scope, context.resources, connState) {

    private val tracker = StatsTracker.getInstance(context)

    var cartItem by mutableStateOf<CartItemStripe?>(null)
        private set

    var subsResult by mutableStateOf<StripeSubsResult?>(null)
        private set

    var defaultPaymentMethod by mutableStateOf<StripePaymentMethod?>(null)
        private set

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

    fun loadDefaultPaymentMethod(account: Account) {
        if (!ensureConnected()) {
            return
        }

        val cusId = account.stripeId
        if (cusId.isNullOrBlank()) {
            showSnackBar("Error: Not a stripe customer!")
            return
        }

        progress.value = true
        scope.launch {

            val result = StripeClient.asyncLoadDefaultPaymentMethod(
                account = account
            )
            progress.value = false

            when (result) {
                is FetchResult.LocalizedError -> {
                    showSnackBar(result.msgId)
                }
                is FetchResult.TextError -> {
                    showSnackBar(result.text)
                }
                is FetchResult.Success -> {
                    defaultPaymentMethod = result.data
                }
            }
        }
    }

    fun subscribe(account: Account) {
        if (!ensureConnected()) {
            return
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
