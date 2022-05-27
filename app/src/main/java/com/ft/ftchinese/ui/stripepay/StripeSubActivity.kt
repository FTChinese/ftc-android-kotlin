package com.ft.ftchinese.ui.stripepay

import android.annotation.SuppressLint
import android.app.Activity
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.model.paywall.CartItemStripe
import com.ft.ftchinese.ui.MemberActivity
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.components.*
import com.ft.ftchinese.ui.theme.OTheme
import com.ft.ftchinese.viewmodel.UserViewModel
import com.stripe.android.*
import kotlinx.coroutines.launch
import org.jetbrains.anko.toast

/**
 * See https://stripe.com/docs/mobile/android/basic
 */
class StripeSubActivity : ScopedAppActivity() {

    private lateinit var subsViewModel: StripeSubViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var ephemeralKeyViewModel: EphemeralKeyViewModel

    private lateinit var stripe: Stripe

    // The core of this integration is the PaymentSession class.
    // It uses CustomerSession to launch full-screen activities
    // to collect and store payment information,
    // and can also be used to collect shipping info.
    // To work with PaymentSession, you’ll need to:
    // 1. Create a PaymentSessionConfig object
    // 2. Implement a PaymentSessionListener
    private lateinit var paymentSession: PaymentSession

    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Stripe.
        PaymentConfiguration.init(
            this,
            BuildConfig.STRIPE_KEY
        )
        stripe = Stripe(
            this,
            BuildConfig.STRIPE_KEY
        )

        subsViewModel = ViewModelProvider(this)[StripeSubViewModel::class.java]
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        ephemeralKeyViewModel = ViewModelProvider(this)[EphemeralKeyViewModel::class.java]

        // Monitoring network status.
        connectionLiveData.observe(this) {
            ephemeralKeyViewModel.isNetworkAvailable.value = it
        }

        intent.getParcelableExtra<CartItemStripe>(EXTRA_CHECKOUT_ITEM)?.let {
            subsViewModel.putIntoCart(it)
        }

        setupCustomerSession()

        setContent { 
            OTheme {
                val scaffoldState = rememberScaffoldState()
                val scope = rememberCoroutineScope()
                
                Scaffold(
                    topBar = {
                        Toolbar(
                            heading = stringResource(id = SubsScreen.StripePay.titleId),
                            onBack = { finish() },
                        )
                    },
                    scaffoldState = scaffoldState,
                ) {

                    StripeSubActivityScreen(
                        subsViewModel = subsViewModel,
                        userViewModel = userViewModel,
                        onNextAction = {
                            stripe.handleNextActionForPayment(
                                this,
                                it
                            )
                        },
                        onPresentPaymentMethod = {
                            // Step 1 when user clicked payment method selection row.
                            paymentSession.presentPaymentMethodSelection()
                        },
                        showSnackBar = { msg ->
                            scope.launch {
                                scaffoldState.snackbarHostState.showSnackbar(msg)
                            }
                        },
                        onExit = { resultCode ->
                            setResult(resultCode)
                            finish()
                        }
                    )
                }
            }
        }

        subsViewModel.membershipUpdated.observe(this) {
            userViewModel.saveMembership(it)
        }

        subsViewModel.toastLiveData.observe(this) {
            when (it) {
                is ToastMessage.Resource -> toast(it.id)
                is ToastMessage.Text -> toast(it.text)
            }
        }
    }

    // A CustomerSession talks to your backend to retrieve an ephemeral key for your Customer with its EphemeralKeyProvider,
    // and uses that key to manage retrieving and updating the Customer’s payment methods on your behalf.
    // https://stripe.com/docs/mobile/android/basic#set-up-customer-session
    private fun setupCustomerSession() {
        Log.i(TAG, "Setup customer session")

        // Try to initialize customer session.
        try {
            CustomerSession.getInstance()
            Log.i(TAG, "CustomerSession already instantiated")
        } catch (e: Exception) {
            Log.i(TAG, e.message ?: "")

            // You must call [PaymentConfiguration.init] with your publishable key
            // before calling this method.
            CustomerSession.initCustomerSession(
                this,
                ephemeralKeyViewModel
            )
        }

        paymentSession = createPaymentSession(
            activity = this,
            listener = subsViewModel.onPaymentSessionData,
        )
    }


    /**
     * Handle select payment method or authentication.
     * Hook up your PaymentSession instance to a few key parts of your host Activity lifecycle.
     * The first is in onActivityResult().
     * This is all you need to do to get updates from the various activities launched by PaymentSession.
     * Any updates to the data will be reported to the PaymentSessionListener argument to PaymentSession#init().
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.i(TAG, "requestCode: $requestCode, resultCode: $resultCode")

        if (data != null) {
            paymentSession.handlePaymentData(requestCode, resultCode, data)
            return
        }
    }

    companion object {
        private const val TAG = "StripeSubActivity"

        private const val EXTRA_CHECKOUT_ITEM = "extra_checkout_item"

        @JvmStatic
        fun startForResult(activity: Activity, requestCode: Int, item: CartItemStripe) {
            activity.startActivityForResult(
                Intent(activity, StripeSubActivity::class.java).apply {
                    putExtra(EXTRA_CHECKOUT_ITEM, item)
                },
                requestCode,
            )
        }

        @JvmStatic
        fun intent(context: Context, item: CartItemStripe) = Intent(context, StripeSubActivity::class.java).apply {
            putExtra(EXTRA_CHECKOUT_ITEM, item)
        }
    }
}

@Composable
private fun StripeSubActivityScreen(
    subsViewModel: StripeSubViewModel,
    userViewModel: UserViewModel,
    onNextAction: (String) -> Unit,
    onPresentPaymentMethod: () -> Unit,
    showSnackBar: (String) -> Unit,
    onExit: (Int) -> Unit,
) {

    val context = LocalContext.current
    val loading by subsViewModel.inProgress.observeAsState(false)
    val customer by subsViewModel.customerLiveData.observeAsState()
    val paymentMethodState by subsViewModel.paymentMethodInUse.observeAsState()
    val cartItemState by subsViewModel.itemLiveData.observeAsState()
    val subsState by subsViewModel.subsCreated.observeAsState()
    val failureState by subsViewModel.failureLiveData.observeAsState()

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

    if (account.stripeId.isNullOrBlank()) {
        CreateCustomerDialog(
            email = account.email,
            onDismiss = {
                onExit(Activity.RESULT_CANCELED)
            },
            onConfirm = {
                subsViewModel.createCustomer(account)
            }
        )
    }

    LaunchedEffect(key1 = Unit) {
        subsViewModel.loadDefaultPaymentMethod(account)
    }

    failureState?.let {
        when (it) {
            is FailureStatus.Message -> {
                ErrorDialog(
                    text = it.message,
                    onDismiss = {
                        subsViewModel.clearFailureState()
                    }
                )
            }
            is FailureStatus.NextAction -> {
                AlertAuthentication(
                    onConfirm = {
                        onNextAction(it.secret)
                        subsViewModel.clearIdempotency()
                        subsViewModel.clearFailureState()
                    },
                    onDismiss = {
                        subsViewModel.clearFailureState()
                    }
                )
            }
        }
    }

    cartItemState?.let {
        StripePayScreen(
            cartItem = it,
            loading = loading,
            paymentMethod = paymentMethodState?.current,
            subs = subsState,
            onPaymentMethod = onPresentPaymentMethod,
            onSubscribe = {
                subsViewModel.subscribe(account)
            },
            onDone = {
                MemberActivity.start(context)
                onExit(Activity.RESULT_OK)
            }
        )
    }
}

private fun createPaymentSession(
    activity: ComponentActivity,
    listener: PaymentSession.PaymentSessionListener
): PaymentSession {
    return PaymentSession(
        activity = activity,
        PaymentSessionConfig.Builder()
            .setShippingInfoRequired(false)
            .setShippingMethodsRequired(false)
            .setShouldShowGooglePay(false)
            .build()
    ).apply {
        // Attached PaymentSessionListener
        init(listener)
    }
}
