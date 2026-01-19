package com.ft.ftchinese.ui.account.stripewallet

import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.model.stripesubs.PaymentSheetParams
import com.ft.ftchinese.model.stripesubs.StripePaymentMethod
import com.ft.ftchinese.ui.util.toast
import com.ft.ftchinese.ui.components.CreateCustomerDialog
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.viewmodel.UserViewModel
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetContract
import com.stripe.android.paymentsheet.PaymentSheetResult

@Composable
fun StripeWalletActivityScreen(
    userViewModel: UserViewModel = viewModel(),
    scaffoldState: ScaffoldState,
    onExit: () -> Unit
) {

    val context = LocalContext.current

    val accountState = userViewModel.accountLiveData.observeAsState()
    val account = accountState.value

    if (account == null) {
        context.toast("Not logged in")
        return
    }

    // Initialize payment configuration
    LaunchedEffect(key1 = Unit) {
        PaymentConfiguration.init(
            context = context,
            publishableKey = BuildConfig.STRIPE_KEY
        )
    }

    // Create stripe instance.
    val stripe = remember {
        Stripe(context, BuildConfig.STRIPE_KEY)
    }

    val walletState = rememberStripeWalletState(
        scaffoldState = scaffoldState
    )

    val launcher = rememberLauncherForActivityResult(
        contract = PaymentSheetContract()
    ) {
        when (it) {
            is PaymentSheetResult.Canceled -> {
                context.toast("支付设置已取消")
            }
            is PaymentSheetResult.Failed -> {
                context.toast("支付设置失败")
            }
            is PaymentSheetResult.Completed -> {
                walletState.retrieveSetupIntent(stripe, account)
            }
        }
    }

    val paymentMethodInUse = walletState.paymentMethodInUse.value

    LaunchedEffect(key1 = walletState.customer) {
        walletState.customer?.let {
            userViewModel.saveAccount(account.withCustomerID(it.id))
        }
    }

    // When user is trying to change current subscription's
    // default payment method, ask for confirmation.
    val (alertPayMethod, setAlertPayMethod) = remember {
        mutableStateOf<StripePaymentMethod?>(null)
    }

    // Show dialog if user is not a stripe customer yet.
    // If user clicked cancel button, exit this activity.
    if (account.stripeId.isNullOrBlank()) {
        CreateCustomerDialog(
            email = account.email,
            onDismiss = onExit,
            onConfirm = {
                walletState.createCustomer(account)
            }
        )
    }

    if (alertPayMethod != null) {
        AlertModifySubsPaymentMethod(
            onDismiss = { setAlertPayMethod(null) },
            onConfirm = {
                walletState.setSubsDefaultPayment(
                    account = account,
                    paymentMethod = alertPayMethod
                )
                setAlertPayMethod(null)
            }
        )
    }

    LaunchedEffect(key1 = walletState.paymentSheetSetup) {
        walletState.paymentSheetSetup?.let{
            launchPaymentSheet(launcher, it)
        }
    }

    // Upon initial loading, fetch user's default payment method.
    LaunchedEffect(key1 = Unit) {
        walletState.loadDefaultPaymentMethod(account)
    }

    ProgressLayout(
        loading = walletState.progress.value,
        modifier = Modifier.fillMaxSize()
    ) {
        StripeWalletScreen(
            loading = walletState.progress.value,
            paymentMethod = paymentMethodInUse?.current,
            isDefault = paymentMethodInUse?.isDefault ?: true,
            onSetDefault = {
                if (account.membership.isStripe) {
                    // Otherwise we are should obtain
                    // user's permission since we are updating
                    // a valid subscription.
                    setAlertPayMethod(it)
                } else {
                    // If not a valid stripe subscription,
                    // set this payment method as default
                    // under customer object
                    walletState.setCustomerDefaultPayment(
                        account = account,
                        paymentMethod = it,
                    )
                }
            },
            onAddCard = {
                walletState.showPaymentSheet(account)
            }
        )
    }

}


private fun launchPaymentSheet(
    launcher: ManagedActivityResultLauncher<PaymentSheetContract.Args, PaymentSheetResult>,
    params: PaymentSheetParams,
) {
    launcher.launch(
        PaymentSheetContract.Args.createSetupIntentArgs(
            clientSecret = params.clientSecret,
            config = PaymentSheet.Configuration(
                merchantDisplayName = "FT中文网信用卡支付",
                customer = PaymentSheet.CustomerConfiguration(
                    id = params.customerId,
                    ephemeralKeySecret = params.ephemeralKey
                )
            )
        )
    )
}
