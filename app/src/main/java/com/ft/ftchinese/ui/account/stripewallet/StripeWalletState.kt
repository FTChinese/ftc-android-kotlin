package com.ft.ftchinese.ui.account.stripewallet

import android.content.res.Resources
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.stripesubs.PaymentSheetParams
import com.ft.ftchinese.repository.StripeClient
import com.ft.ftchinese.ui.base.ConnectionState
import com.ft.ftchinese.ui.base.ToastMessage
import com.ft.ftchinese.ui.base.connectivityState
import com.ft.ftchinese.ui.components.BaseState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class StripeWalletState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    resources: Resources,
    connState: State<ConnectionState>,
) : BaseState(scaffoldState, scope, resources, connState) {

    private var _paymentSheetParams: PaymentSheetParams? = null

    var paymentSheetSetup by mutableStateOf<PaymentSheetParams?>(null)
        private set

    fun clearSheetParams() {
        _paymentSheetParams = null
    }

    fun showPaymentSheet(account: Account) {
        if (_paymentSheetParams != null) {
            paymentSheetSetup = _paymentSheetParams
        }

        createSetupIntent(account)
    }

    private fun createSetupIntent(account: Account) {
        val customerId = account.stripeId ?: return
        if (!ensureConnected()) {
            return
        }

        progress.value = true
        scope.launch {
            val result = StripeClient.asyncSetupWithEphemeral(
                customerId
            )

            when (result) {
                is FetchResult.LocalizedError -> {
                    showSnackBar(result.msgId)
                }
                is FetchResult.TextError -> {
                    showSnackBar(result.text)
                }
                is FetchResult.Success -> {
                    _paymentSheetParams = result.data
                    showPaymentSheet(account)
                }
            }
        }
    }
}

@Composable
fun rememberStripeWalletState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    scope: CoroutineScope = rememberCoroutineScope(),
    resources: Resources = LocalContext.current.resources,
    connState: State<ConnectionState> = connectivityState()
) = remember(scaffoldState, resources, connState) {
    StripeWalletState(
        scaffoldState = scaffoldState,
        scope = scope,
        resources = resources,
        connState = connState
    )
}
