package com.ft.ftchinese.ui.account.stripewallet

import android.content.res.Resources
import android.util.Log
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.stripesubs.PaymentSheetParams
import com.ft.ftchinese.model.stripesubs.StripeCustomer
import com.ft.ftchinese.model.stripesubs.StripePaymentMethod
import com.ft.ftchinese.repository.StripeClient
import com.ft.ftchinese.ui.components.BaseState
import com.ft.ftchinese.ui.util.ConnectionState
import com.ft.ftchinese.ui.util.connectivityState
import com.stripe.android.ApiResultCallback
import com.stripe.android.Stripe
import com.stripe.android.model.SetupIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "StripeWallet"

open class StripeWalletState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    resources: Resources,
    connState: State<ConnectionState>,
) : BaseState(scaffoldState, scope, resources, connState) {

    var customer by mutableStateOf<StripeCustomer?>(null)
        private set

    var defaultPaymentMethod by mutableStateOf<StripePaymentMethod?>(null)
        private set

    var paymentMethodSelected by mutableStateOf<StripePaymentMethod?>(null)

    var paymentMethodInUse = derivedStateOf<PaymentMethodInUse?> {
        if (paymentMethodSelected != null) {
            PaymentMethodInUse(
                current = paymentMethodSelected,
                isDefault = paymentMethodSelected?.id == defaultPaymentMethod?.id
            )
        } else {
            PaymentMethodInUse(
                current = defaultPaymentMethod,
                isDefault = true,
            )
        }
    }

    private var _paymentSheetParams: PaymentSheetParams? = null

    var paymentSheetSetup by mutableStateOf<PaymentSheetParams?>(null)
        private set

    fun createCustomer(account: Account) {
        if (!ensureConnected()) {
            return
        }

        progress.value = true
        scope.launch {
            val result = StripeClient.asyncCreateCustomer(account)
            progress.value = false

            when (result) {
                is FetchResult.LocalizedError -> {
                   showSnackBar(result.msgId)
                }
                is FetchResult.TextError -> {
                    showSnackBar(result.text)
                }
                is FetchResult.Success -> {
                    customer = result.data
                }
            }
        }
    }

    fun loadDefaultPaymentMethod(account: Account) {
        if (!ensureConnected()) {
            return
        }

        if (account.stripeId.isNullOrBlank()) {
            showSnackBar("Error: not a stripe customer!")
            return
        }

        progress.value = true
        scope.launch {
            val result = StripeClient.asyncLoadDefaultPaymentMethod(
                account
            )

            progress.value = false
            when (result) {
                is FetchResult.LocalizedError -> {
//                    showSnackBar(result.msgId)
                }
                is FetchResult.TextError -> {
//                    showSnackBar(result.text)
                }
                is FetchResult.Success -> {
                     defaultPaymentMethod = result.data
                }
            }
        }
    }

    // When user do not have a valid stripe subscription, we should still permit
    // setting a default payment method.
    fun setCustomerDefaultPayment(account: Account, paymentMethod: StripePaymentMethod) {
        if (!ensureConnected()) {
            return
        }

        val pmId = paymentMethod.id
        if (pmId.isBlank()) {
            showSnackBar(R.string.stripe_no_payment_selected)
            return
        }

        progress.value = true
        scope.launch {
            val result = StripeClient.asyncSetCusDefaultPayment(
                account = account,
                paymentMethodId = pmId,
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
                    defaultPaymentMethod = paymentMethod
                }
            }
        }
    }

    // Set default payment method when user still has a valid subscription.
    fun setSubsDefaultPayment(account: Account, paymentMethod: StripePaymentMethod) {
        if (!ensureConnected()) {
            return
        }

        val pmId = paymentMethod.id
        if (pmId.isBlank()) {
            showSnackBar(R.string.stripe_no_payment_selected)
            return
        }

        progress.value = true
        scope.launch {
            val result = StripeClient.asyncSetSubsDefaultPayment(
                account = account,
                paymentMethodId = pmId,
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
                    defaultPaymentMethod = paymentMethod
                }
            }
        }
    }

    fun showPaymentSheet(account: Account) {
        _paymentSheetParams?.let {
            paymentSheetSetup = it.copy()
            return
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
                account.isTest,
                customerId
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
                    _paymentSheetParams = result.data
                    showPaymentSheet(account)
                }
            }
        }
    }

    // Retrieve setup intent after it is being used so that
    // we could know which payment method was handled.
    // The payment method is retrieved from two sources:
    // 1. Use stripe SDK, which might given null, then
    // 2. Use the payment method id to retrieve it from our server.
    fun retrieveSetupIntent(stripe: Stripe, account: Account) {
        paymentSheetSetup?.let {
            Log.i(TAG, "Retrieving setup intent")
            progress.value = true
            stripe.retrieveSetupIntent(
                clientSecret = it.clientSecret,
                callback = object : ApiResultCallback<SetupIntent> {
                    override fun onError(e: Exception) {
                        progress.value = false
                        e.message?.let { showSnackBar(it) }
                    }

                    override fun onSuccess(result: SetupIntent) {
                        // The SetupIntent#paymentMethod might be null.
                        // SetupIntent#paymentMethodId should always exists.
                        Log.i(TAG, "Setup intent retrieved $result")
                        Log.i(TAG, "Payment method ${result.paymentMethod}, id ${result.paymentMethodId}")

                        loadPaymentMethod(result, account.isTest)
                    }
                }
            )

            clearSheetParams()
        }

    }

    private fun clearSheetParams() {
        _paymentSheetParams = null
    }

    private fun loadPaymentMethod(setupIntent: SetupIntent, isTest: Boolean) {
        val rawPm = setupIntent.paymentMethod
        val pmId = setupIntent.paymentMethodId
        // Here paymentMethod field is null,
        // paymentMethodId is populated.
        if (rawPm != null) {
            paymentMethodSelected = StripePaymentMethod.newInstance(rawPm)
            progress.value = false
            return
        }

        if (pmId.isNullOrBlank()) {
            showSnackBar("Error: missing payment method id")
            return
        }

        scope.launch {
            val result = StripeClient.asyncRetrievePaymentMethod(
                isTest,
                pmId,
                true
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
                    paymentMethodSelected = result.data
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
