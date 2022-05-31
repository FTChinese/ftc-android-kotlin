package com.ft.ftchinese.ui.account.stripewallet

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.CreateCustomerDialog
import com.ft.ftchinese.ui.base.ToastMessage
import com.ft.ftchinese.ui.base.toast
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.theme.OTheme
import com.ft.ftchinese.viewmodel.UserViewModel
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.launch

private const val TAG = "StripeWalletActivity"

/**
 * See https://github.com/stripe/stripe-android/blob/master/example/src/main/java/com/stripe/example/activity/ComposeExampleActivity.kt
 */
class StripeWalletActivity : ComponentActivity() {
    private lateinit var walletViewModel: StripeWalletViewModel
    private lateinit var paymentSheet: PaymentSheet
    private lateinit var stripe: Stripe

    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PaymentConfiguration.init(this, BuildConfig.STRIPE_KEY)
        stripe = Stripe(
            this,
            BuildConfig.STRIPE_KEY
        )

        paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)

        walletViewModel = ViewModelProvider(this)[StripeWalletViewModel::class.java]

        setContent {
            OTheme {
                val scaffoldState = rememberScaffoldState()
                val scope = rememberCoroutineScope()

                Scaffold(
                    topBar = {
                        Toolbar(
                            heading = stringResource(id = R.string.stripe_setting),
                            onBack = {
                                finish()
                            }
                        )
                    },
                    scaffoldState = scaffoldState
                ) {
                    StripeSetupActivityScreen(
                        walletViewModel = walletViewModel,
                        onExit = {
                            finish()
                        },
                        showSnackBar = { msg ->
                            scope.launch {
                                scaffoldState.snackbarHostState.showSnackbar(msg)
                            }
                        }
                    )
                }
            }
        }

        walletViewModel.toastLiveData.observe(this) {
            when (it) {
                is ToastMessage.Resource -> toast(it.id)
                is ToastMessage.Text -> toast(it.text)
            }
        }

        walletViewModel.setupLiveData.observe(this) {
            if (it == null) {
                return@observe
            }

            paymentSheet.presentWithSetupIntent(
                setupIntentClientSecret = it.clientSecret,
                configuration = PaymentSheet.Configuration(
                    merchantDisplayName = "FTC Stripe Pay Setting",
                    customer = PaymentSheet.CustomerConfiguration(
                        id = it.customerId,
                        ephemeralKeySecret = it.ephemeralKey
                    )
                )
            )
        }
    }

    private fun onPaymentSheetResult(paymentSheetResult: PaymentSheetResult) {
        when (paymentSheetResult) {
            is PaymentSheetResult.Canceled -> {
                toast("支付设置已取消")
            }
            is PaymentSheetResult.Failed -> {
                toast("支付设置失败")
            }
            is PaymentSheetResult.Completed -> {
                onPaymentSheetCompleted()
            }
        }
    }

    private fun onPaymentSheetCompleted() {
        Log.i(TAG, "Setup success")

        walletViewModel.setupLiveData.value?.let {
            walletViewModel.progressLiveData.value = true

            Log.i(TAG, "Retrieving setup intent")
            stripe.retrieveSetupIntent(
                clientSecret = it.clientSecret,
                callback = walletViewModel.onSetupIntentRetrieved
            )

            walletViewModel.clearPaymentSheet()
        }
    }

    companion object {
        @JvmStatic
        fun start(context: Context?) {
            context?.startActivity(Intent(context, StripeWalletActivity::class.java))
        }
    }
}

@Composable
private fun StripeSetupActivityScreen(
    walletViewModel: StripeWalletViewModel,
    userViewModel: UserViewModel = viewModel(),
    onExit: () -> Unit,
    showSnackBar: (String) -> Unit
) {

    val loading by walletViewModel.progressLiveData.observeAsState(false)
    val paymentMethod by walletViewModel.paymentMethodInUse.observeAsState()
    val customer by walletViewModel.customerLiveData.observeAsState()

    val accountState = userViewModel.accountLiveData.observeAsState()
    val account = accountState.value

    if (account == null) {
        showSnackBar("Not logged in")
        return
    }

    LaunchedEffect(key1 = customer) {
        customer?.let {
            userViewModel.saveAccount(account.withCustomerID(it.id))
        }
    }

    // Show dialog if user is not a stripe customer yet.
    // If user clicked cancel button, exit this activity.
    if (account.stripeId.isNullOrBlank()) {
        CreateCustomerDialog(
            email = account.email,
            onDismiss = onExit,
            onConfirm = {
                walletViewModel.createCustomer(account)
            }
        )
    }

    // Upon initial loading, fetch user's default payment method.
    LaunchedEffect(key1 = Unit) {
        walletViewModel.loadDefaultPaymentMethod(account)
    }

    StripeWalletScreen(
        loading = loading,
        paymentMethod = paymentMethod?.current,
        isDefault = paymentMethod?.isDefault ?: false,
        onSetDefault = {
           walletViewModel.setDefaultPaymentMethod(
               account = account,
               paymentMethod = it,
           )
        },
        onAddCard = {
            walletViewModel.showPaymentSheet(account)
        }
    )
}

