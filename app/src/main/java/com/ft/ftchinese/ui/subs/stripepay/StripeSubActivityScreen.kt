package com.ft.ftchinese.ui.subs.stripepay

import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.model.stripesubs.PaymentSheetParams
import com.ft.ftchinese.repository.ApiConfig
import com.ft.ftchinese.ui.components.CreateCustomerDialog
import com.ft.ftchinese.ui.components.ErrorDialog
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.util.toast
import com.ft.ftchinese.viewmodel.UserViewModel
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetContract
import com.stripe.android.paymentsheet.PaymentSheetResult

@Composable
fun StripeSubActivityScreen(
    userViewModel: UserViewModel,
    scaffoldState: ScaffoldState,
    priceId: String?,
    trialId: String?,
    couponId: String?,
    onSuccess: () -> Unit,
    onCancelled: () -> Unit
) {

    val context = LocalContext.current

    val accountState = userViewModel.accountLiveData.observeAsState()
    val account = accountState.value

    if (account == null) {
        context.toast("Not logged in")
        return
    }

    val apiConfig = remember {
        ApiConfig.ofSubs(account.isTest)
    }

    val paymentState = rememberStripeSubState(
        scaffoldState = scaffoldState
    )

    LaunchedEffect(key1 = Unit) {
        if (priceId.isNullOrBlank()) {
            context.toast("Error: price id not provided!")
        } else {
            paymentState.loadCheckoutItem(
                priceId = priceId,
                trialId = trialId,
                couponId = couponId,
                membership = account.membership
            )
        }
    }

    // Payment sheet launcher
    val launcher = rememberLauncherForActivityResult(
        contract = PaymentSheetContract()
    ) {
        when (it) {
            is PaymentSheetResult.Canceled -> {
                context.toast("支付设置已取消")
            }
            is PaymentSheetResult.Failed -> {
                context.toast(formatPaymentSheetFailure(it.error))
            }
            is PaymentSheetResult.Completed -> {
                paymentState.retrieveSetupIntent(context, account)
            }
        }
    }

    // Update account after customer created.
    LaunchedEffect(key1 = paymentState.customer) {
        paymentState.customer?.let {
            userViewModel.saveAccount(account.withCustomerID(it.id))
        }
    }

    // Ask user to create a stripe customer registered yet.
    if (account.stripeId.isNullOrBlank()) {
        CreateCustomerDialog(
            email = account.email,
            onDismiss = onCancelled,
            onConfirm = {
                paymentState.createCustomer(account)
            }
        )
    }

    // Show payment sheet if payment sheet setup present.
    LaunchedEffect(key1 = paymentState.paymentSheetSetup) {
        paymentState.paymentSheetSetup?.let {
            launchPaymentSheet(context, launcher, it)
        }
    }

    // Update membership after subscription created/updated.
    LaunchedEffect(key1 = paymentState.subsResult) {
        paymentState.subsResult?.let {
            userViewModel.saveMembership(it.membership)
        }
    }

    // Load default payment method upon ui initialization.
    LaunchedEffect(key1 = Unit) {
        paymentState.loadDefaultPaymentMethod(account)
    }

    // If there's coupon, check whether user have already used any coupon
    // during the lifecycle of current subscription period.
    LaunchedEffect(key1 = paymentState.cartItem) {
        // Only check duplicate redeem when intent is apply coupon.
        // If user is switching cycle or tier, we simply let user go ahead.
        if (paymentState.cartItem?.isApplyCoupon != true) {
            return@LaunchedEffect
        }

        // TODO: avoid multiple request.
        account.membership.stripeSubsId?.let {
            paymentState.findCouponApplied(
                api = apiConfig,
                ftcId = account.id,
                subsId = it,
            )
        }
    }

    // Handle payment sheet failure.
    paymentState.failure?.let {
        when (it) {
            is FailureStatus.Message -> {
                ErrorDialog(
                    text = it.message,
                    onDismiss = {
                        paymentState.clearFailureState()
                    }
                )
            }
            is FailureStatus.NextAction -> {
                AlertAuthentication(
                    onConfirm = {
                        paymentState.clearFailureState()
                    },
                    onDismiss = {
                        paymentState.clearFailureState()
                    }
                )
            }
        }
    }

    ProgressLayout(
        loading = paymentState.loadingState.value,
        modifier = Modifier.fillMaxSize()
    ) {
        paymentState.cartItem?.let {
            StripePayScreen(
                cartItem = it,
                loading = paymentState.loadingState.value,
                mode = apiConfig.mode,
                paymentMethod = paymentState.paymentMethodInUse.value?.current,
                subs = paymentState.subsResult?.subs,
                couponApplied = paymentState.couponApplied,
                onPaymentMethod = {
                      paymentState.showPaymentSheet(account)
                },
                onSubscribe = {
                    paymentState.subscribe(account, it)
                },
                onDone = onSuccess
            )
        }
    }
}

private fun launchPaymentSheet(
    context: android.content.Context,
    launcher: ManagedActivityResultLauncher<PaymentSheetContract.Args, PaymentSheetResult>,
    params: PaymentSheetParams,
) {
    PaymentConfiguration.init(
        context = context,
        publishableKey = params.publishableKey
    )

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

private fun formatPaymentSheetFailure(error: Throwable): String {
    return if (BuildConfig.DEBUG) {
        "支付设置失败: ${error.message ?: error.javaClass.simpleName}"
    } else {
        "支付设置失败"
    }
}
