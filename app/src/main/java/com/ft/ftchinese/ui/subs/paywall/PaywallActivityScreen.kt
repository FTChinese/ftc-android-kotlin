package com.ft.ftchinese.ui.subs.paywall

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ScaffoldState
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.paywall.IntentKind
import com.ft.ftchinese.model.paywall.CartItemFtc
import com.ft.ftchinese.model.paywall.CartItemStripe
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.subscriptioncatalog.SubscriptionCatalogOption
import com.ft.ftchinese.model.subscriptioncatalog.SubscriptionCatalogPlan
import com.ft.ftchinese.model.subscriptioncatalog.SubscriptionCatalogProduct
import com.ft.ftchinese.repository.ApiConfig
import com.ft.ftchinese.repository.StripeClient
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.auth.AuthActivity
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.subs.catalog.CatalogFtcCheckout
import com.ft.ftchinese.ui.subs.catalog.CatalogFtcCheckoutStore
import com.ft.ftchinese.ui.subs.catalog.CatalogStripeCheckout
import com.ft.ftchinese.ui.subs.catalog.CatalogStripeCheckoutStore
import com.ft.ftchinese.ui.subs.catalog.SubscriptionCatalogScreen
import com.ft.ftchinese.ui.subs.catalog.buildCatalogStripeCartItem
import com.ft.ftchinese.ui.subs.catalog.buildCatalogFtcCartItem
import com.ft.ftchinese.ui.subs.catalog.rememberSubscriptionCatalogState
import com.ft.ftchinese.ui.subs.SubscriptionEntryIntent
import com.ft.ftchinese.ui.subs.member.CancelStripeDialog
import com.ft.ftchinese.ui.subs.stripeAutoRenewUiState
import com.ft.ftchinese.ui.util.AccountAction
import com.ft.ftchinese.ui.util.IntentsUtil
import com.ft.ftchinese.ui.util.toast
import com.ft.ftchinese.ui.wxlink.launchWxLinkEmailActivity
import com.ft.ftchinese.viewmodel.UserViewModel
import kotlinx.coroutines.launch

private const val PURCHASE_FLOW_TAG = "FTCPurchaseFlow"

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PaywallActivityScreen(
    userViewModel: UserViewModel = viewModel(),
    scaffoldState: ScaffoldState,
    premiumOnTop: Boolean,
    subscriptionEntry: SubscriptionEntryIntent? = null,
    onFtcPay: (item: CartItemFtc) -> Unit,
    onStripePay: (item: CartItemStripe) -> Unit,
    onFtcPayById: (priceId: String, payMethod: PayMethod?) -> Unit,
    onStripePayByIds: (priceId: String, trialId: String?, couponId: String?) -> Unit,
) {

    val context = LocalContext.current
    val tracker = remember {
        StatsTracker.getInstance(context)
    }

    val accountState = userViewModel.accountLiveData.observeAsState()
    val account = accountState.value
    val isLoggedIn by userViewModel.loggedInLiveData.observeAsState(false)
    val isTest by userViewModel.testUserLiveData.observeAsState(false)

    val apiConfig = remember(key1 = isTest) {
        ApiConfig.ofSubs(isTest)
    }

    val (openDialog, setOpenDialog) = remember {
        mutableStateOf(false)
    }
    val (showAutoRenewOffDialog, setShowAutoRenewOffDialog) = remember {
        mutableStateOf(false)
    }

    val catalogState = rememberSubscriptionCatalogState(
        scaffoldState = scaffoldState
    )
    val scope = rememberCoroutineScope()
    var directStripeUpdating by remember { mutableStateOf(false) }

    // Launcher if user is not logged in.
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                userViewModel.reloadAccount()
                result.data?.let(IntentsUtil::getAccountAction)?.let {
                    if (it == AccountAction.SignedIn) {
                        context.toast(R.string.login_success)
                    }
                }
            }
            Activity.RESULT_CANCELED -> {

            }
        }
    }

    // Load the new Node-backed catalog first. Do not eagerly load the
    // legacy paywall because it depends on older endpoints that may still
    // require MySQL-only infrastructure.
    LaunchedEffect(apiConfig.baseUrl, account?.id, subscriptionEntry) {
        catalogState.loadCatalog(
            api = apiConfig,
            userId = account?.id,
            entry = subscriptionEntry,
        )

        tracker.displayPaywall()
    }

    if (openDialog ) {
        LinkEmailDialog(
            onConfirm = {
                launchWxLinkEmailActivity(launcher, context)
                setOpenDialog(false)
            },
            onDismiss = {
                setOpenDialog(false)
            }
        )
    }

    val catalogData = remember(catalogState.catalog, premiumOnTop) {
        catalogState.catalog
            ?.reOrderProducts(premiumOnTop)
    }

    val checkoutMembership = remember(account?.membership, catalogData?.summary) {
        account?.membership?.let { membership ->
            catalogData?.summary?.checkoutMembership(membership) ?: membership
        }
    }

    fun startFtcCatalogCheckout(
        product: SubscriptionCatalogProduct,
        plan: SubscriptionCatalogPlan,
        option: SubscriptionCatalogOption,
        payMethod: PayMethod?,
        origin: String,
    ) {
        if (!userViewModel.isLoggedIn) {
            Log.i(PURCHASE_FLOW_TAG, "catalog_checkout_$origin blocked=not_logged_in")
            AuthActivity.launch(launcher, context)
            return
        }

        val membership = checkoutMembership ?: run {
            Log.i(PURCHASE_FLOW_TAG, "catalog_checkout_$origin blocked=missing_membership")
            return
        }

        val cartItem = buildCatalogFtcCartItem(
            membership = membership,
            product = product,
            plan = plan,
            option = option
        ) ?: run {
            Log.i(
                PURCHASE_FLOW_TAG,
                "catalog_checkout_$origin blocked=cart_build_failed " +
                    "tier=${product.tier?.symbol.orEmpty()} cycle=${plan.cycle} " +
                    "kind=${option.kind} ftcPriceId=${option.checkout.ftcPriceId}"
            )
            context.toast("Error building checkout item")
            return
        }

        CatalogFtcCheckoutStore.save(
            CatalogFtcCheckout(
                priceId = option.checkout.ftcPriceId,
                cartItem = cartItem,
                payMethod = payMethod
            )
        )

        Log.i(
            PURCHASE_FLOW_TAG,
            "catalog_checkout_$origin open_ftc_pay " +
                "tier=${product.tier?.symbol.orEmpty()} cycle=${plan.cycle} " +
                "payMethod=$payMethod ftcPriceId=${option.checkout.ftcPriceId}"
        )
        onFtcPayById(option.checkout.ftcPriceId, payMethod)
    }

    fun startStripeCatalogCheckout(
        priceId: String,
        trialId: String?,
        couponId: String?,
        origin: String,
    ) {
        if (!userViewModel.isLoggedIn) {
            Log.i(PURCHASE_FLOW_TAG, "catalog_checkout_$origin blocked=not_logged_in")
            AuthActivity.launch(launcher, context)
            return
        }

        if (userViewModel.isWxOnly) {
            Log.i(PURCHASE_FLOW_TAG, "catalog_checkout_$origin blocked=wx_only_account")
            setOpenDialog(true)
            return
        }

        val currentAccount = account ?: run {
            Log.i(PURCHASE_FLOW_TAG, "catalog_checkout_$origin blocked=missing_account")
            return
        }
        val membership = checkoutMembership ?: run {
            Log.i(PURCHASE_FLOW_TAG, "catalog_checkout_$origin blocked=missing_membership")
            return
        }
        val data = catalogData ?: run {
            Log.i(PURCHASE_FLOW_TAG, "catalog_checkout_$origin blocked=missing_catalog")
            return
        }

        val product = data.products
            .firstOrNull { product ->
                product.plans.any { plan ->
                    plan.options.any { option ->
                        option.checkout.stripePriceId == priceId
                    }
                }
            }
        val plan = product?.plans?.firstOrNull { plan ->
            plan.options.any { option ->
                option.checkout.stripePriceId == priceId
            }
        }
        val option = plan?.options?.firstOrNull { option ->
            option.checkout.stripePriceId == priceId
        }

        if (product != null && plan != null && option != null) {
            val cartItem = buildCatalogStripeCartItem(
                membership = membership,
                product = product,
                plan = plan,
                option = option
            )

            if (cartItem != null) {
                if (cartItem.isDirectSubscriptionUpdate) {
                    Log.i(
                        PURCHASE_FLOW_TAG,
                        "catalog_checkout_$origin stripe_direct_update " +
                            "priceId=$priceId intent=${cartItem.intent.kind}"
                    )
                    directStripeUpdating = true
                    scope.launch {
                        try {
                            val result = StripeClient.asyncUpdateSubs(
                                account = currentAccount,
                                params = cartItem.subsParams(payMethod = null)
                            )

                            when (result) {
                                is FetchResult.Success -> {
                                    userViewModel.saveStripeSubs(result.data)
                                    catalogState.refreshCatalog(
                                        api = apiConfig,
                                        userId = currentAccount.id,
                                        entry = subscriptionEntry,
                                    )
                                    scaffoldState.snackbarHostState.showSnackbar(
                                        directStripeSuccessMessage(cartItem)
                                    )
                                }
                                is FetchResult.LocalizedError -> {
                                    scaffoldState.snackbarHostState.showSnackbar(
                                        context.getString(result.msgId)
                                    )
                                }
                                is FetchResult.TextError -> {
                                    scaffoldState.snackbarHostState.showSnackbar(result.text)
                                }
                            }
                        } catch (e: Exception) {
                            scaffoldState.snackbarHostState.showSnackbar(
                                e.localizedMessage ?: "订阅设置更新失败"
                            )
                        } finally {
                            directStripeUpdating = false
                        }
                    }
                    return
                }

                CatalogStripeCheckoutStore.save(
                    CatalogStripeCheckout(
                        priceId = priceId,
                        cartItem = cartItem
                    )
                )
            }
        }

        Log.i(
            PURCHASE_FLOW_TAG,
            "catalog_checkout_$origin open_stripe_pay priceId=$priceId " +
                "trialId=${trialId.orEmpty()} couponId=${couponId.orEmpty()}"
        )
        onStripePayByIds(priceId, trialId, couponId)
    }

    fun updateStripeAutoRenew(enabled: Boolean) {
        val currentAccount = account ?: return
        directStripeUpdating = true
        scope.launch {
            try {
                val result = if (enabled) {
                    StripeClient.asyncReactiveSub(currentAccount)
                } else {
                    StripeClient.asyncCancelSub(currentAccount)
                }

                when (result) {
                    is FetchResult.Success -> {
                        userViewModel.saveStripeSubs(result.data)
                        catalogState.refreshCatalog(
                            api = apiConfig,
                            userId = currentAccount.id,
                            entry = subscriptionEntry,
                        )
                        scaffoldState.snackbarHostState.showSnackbar(
                            if (enabled) {
                                "已开启自动续订"
                            } else {
                                "已关闭自动续订"
                            }
                        )
                    }
                    is FetchResult.LocalizedError -> {
                        scaffoldState.snackbarHostState.showSnackbar(
                            context.getString(result.msgId)
                        )
                    }
                    is FetchResult.TextError -> {
                        scaffoldState.snackbarHostState.showSnackbar(result.text)
                    }
                }
            } catch (e: Exception) {
                scaffoldState.snackbarHostState.showSnackbar(
                    e.localizedMessage ?: "自动续订设置更新失败"
                )
            } finally {
                directStripeUpdating = false
            }
        }
    }

    if (showAutoRenewOffDialog) {
        CancelStripeDialog(
            bodyText = checkoutMembership
                ?.stripeAutoRenewUiState(catalogData?.preferredLanguage ?: "zh")
                ?.offConfirmation,
            onConfirm = {
                setShowAutoRenewOffDialog(false)
                updateStripeAutoRenew(false)
            },
            onDismiss = {
                setShowAutoRenewOffDialog(false)
            }
        )
    }

    val refreshing = catalogState.refreshing
    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = {
            catalogState.refreshCatalog(
                api = apiConfig,
                userId = account?.id,
                entry = subscriptionEntry,
            )
        },
    )
    ProgressLayout(
        loading = directStripeUpdating,
        modifier = Modifier.fillMaxSize()
    ) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        if (catalogData != null) {
            SubscriptionCatalogScreen(
                catalog = catalogData,
                membership = checkoutMembership,
                isLoggedIn = isLoggedIn,
                autoOpenPaymentDialogTier = subscriptionEntry?.tier,
                onLoginRequest = {
                    AuthActivity.launch(launcher, context)
                },
                onFtcCheckout = { product, plan, option, payMethod ->
                    startFtcCatalogCheckout(
                        product = product,
                        plan = plan,
                        option = option,
                        payMethod = payMethod,
                        origin = "manual",
                    )
                },
                onStripeCheckout = { priceId, trialId, couponId ->
                    startStripeCatalogCheckout(
                        priceId = priceId,
                        trialId = trialId,
                        couponId = couponId,
                        origin = "manual",
                    )
                },
                onStripeAutoRenewChange = { enabled ->
                    if (enabled) {
                        updateStripeAutoRenew(true)
                    } else {
                        setShowAutoRenewOffDialog(true)
                    }
                }
            )
        }
        PullRefreshIndicator(
            refreshing = refreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
    }
}

private fun directStripeSuccessMessage(item: CartItemStripe): String {
    return when (item.intent.kind) {
        IntentKind.Downgrade ->
            "已安排下次续订起转为标准会员"
        IntentKind.CancelScheduledChange ->
            "已取消降级安排，将保留当前方案自动续订"
        else -> "订阅设置已更新"
    }
}
