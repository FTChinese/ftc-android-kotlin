package com.ft.ftchinese.ui.subs.paywall

import android.app.Activity
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
import com.ft.ftchinese.repository.ApiConfig
import com.ft.ftchinese.repository.StripeClient
import com.ft.ftchinese.tracking.AddCartParams
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
import com.ft.ftchinese.ui.subs.member.CancelStripeDialog
import com.ft.ftchinese.ui.subs.stripeAutoRenewUiState
import com.ft.ftchinese.ui.util.AccountAction
import com.ft.ftchinese.ui.util.IntentsUtil
import com.ft.ftchinese.ui.util.toast
import com.ft.ftchinese.ui.wxlink.launchWxLinkEmailActivity
import com.ft.ftchinese.viewmodel.UserViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PaywallActivityScreen(
    userViewModel: UserViewModel = viewModel(),
    scaffoldState: ScaffoldState,
    premiumOnTop: Boolean,
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
    LaunchedEffect(apiConfig.baseUrl, account?.id) {
        catalogState.loadCatalog(
            api = apiConfig,
            userId = account?.id
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
                            userId = currentAccount.id
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
                userId = account?.id
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
                onLoginRequest = {
                    AuthActivity.launch(launcher, context)
                },
                onFtcCheckout = { product, plan, option, payMethod ->
                    if (!userViewModel.isLoggedIn) {
                        AuthActivity.launch(launcher, context)
                    } else {
                        val membership = checkoutMembership ?: return@SubscriptionCatalogScreen
                        val cartItem = buildCatalogFtcCartItem(
                            membership = membership,
                            product = product,
                            plan = plan,
                            option = option
                        ) ?: run {
                            context.toast("Error building checkout item")
                            return@SubscriptionCatalogScreen
                        }

                        CatalogFtcCheckoutStore.save(
                            CatalogFtcCheckout(
                                priceId = option.checkout.ftcPriceId,
                                cartItem = cartItem,
                                payMethod = payMethod
                            )
                        )

                        onFtcPayById(option.checkout.ftcPriceId, payMethod)
                    }
                },
                onStripeCheckout = { priceId, trialId, couponId ->
                    if (!userViewModel.isLoggedIn) {
                        AuthActivity.launch(launcher, context)
                    } else if (userViewModel.isWxOnly) {
                        setOpenDialog(true)
                    } else {
                        val currentAccount = account ?: return@SubscriptionCatalogScreen
                        val membership = checkoutMembership ?: return@SubscriptionCatalogScreen
                        val product = catalogData.products
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
                                                        userId = currentAccount.id
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
                                    return@SubscriptionCatalogScreen
                                }

                                CatalogStripeCheckoutStore.save(
                                    CatalogStripeCheckout(
                                        priceId = priceId,
                                        cartItem = cartItem
                                    )
                                )
                            }
                        }

                        onStripePayByIds(priceId, trialId, couponId)
                    }
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
