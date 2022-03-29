package com.ft.ftchinese.ui.paywall

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.R
import com.ft.ftchinese.model.paywall.*
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.stripesubs.StripePrice
import com.ft.ftchinese.ui.components.CustomerService
import com.ft.ftchinese.ui.formatter.FormatHelper
import com.ft.ftchinese.ui.login.AuthActivity
import com.ft.ftchinese.ui.product.PriceList
import com.ft.ftchinese.ui.product.ProductCard
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.wxlink.LinkFtcActivity
import com.ft.ftchinese.viewmodel.AuthViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import dev.jeziellago.compose.markdowntext.MarkdownText

private fun launchLoginActivity(
    launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    context: Context,
) {
    launcher.launch(
        AuthActivity.intent(context)
    )
}

private fun launchLinkFtcActivity(
    launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    context: Context,
) {
    launcher.launch(LinkFtcActivity.intent(context))
}

@Composable
fun PaywallScreen(
    paywallViewModel: PaywallViewModel,
    authViewModel: AuthViewModel = viewModel(),
    onFtcPay: (item: CartItemFtcV2) -> Unit,
    onStripePay: (item: CartItemStripeV2) -> Unit,
    onError: (String) -> Unit,
) {

    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->

        when (result.resultCode) {
            Activity.RESULT_OK -> {
                authViewModel.load()
            }
            Activity.RESULT_CANCELED -> {

            }
        }
    }

    val (openDialog, setOpenDialog) = remember {
        mutableStateOf(false)
    }

    LaunchedEffect(key1 = Unit) {
        paywallViewModel.loadPaywall(
            authViewModel.account?.isTest ?: false
        )
        paywallViewModel.loadStripePrices()
    }

    LaunchedEffect(key1 = paywallViewModel.msgId) {
        paywallViewModel.msgId?.let {
            onError(context.getString(it))
        }
    }

    LaunchedEffect(key1 = paywallViewModel.errMsg) {
        paywallViewModel.errMsg?.let {
            onError(it)
        }
    }

    if (openDialog ) {
        LinkEmailDialog(
            onConfirm = {
                launchLinkFtcActivity(launcher, context)
                setOpenDialog(false)
            },
            onDismiss = {
                setOpenDialog(false)
            }
        )
    }

    SwipeRefresh(
        state = rememberSwipeRefreshState(
            isRefreshing = paywallViewModel.isRefreshing,
        ),
        onRefresh = { paywallViewModel.refresh() },
    ) {
        PaywallBody(
            paywall = paywallViewModel.paywallState,
            stripePrices = paywallViewModel.stripeState,
            account = authViewModel.account,
            onFtcPay = {
                if (!authViewModel.isLoggedIn) {
                    launchLoginActivity(launcher, context)
                    return@PaywallBody
                }
                onFtcPay(it)
           },
            onStripePay = {
                if (!authViewModel.isLoggedIn) {
                    launchLoginActivity(launcher, context)
                    return@PaywallBody
                }
                if (!authViewModel.isWxOnly) {
                    setOpenDialog(true)
                    return@PaywallBody
                }
                onStripePay(it)
            },
            loginButton = {
                PaywallLogin {
                    launchLoginActivity(launcher, context)
                }
            },
            customerService = {
                CustomerService(onError = onError)
            }
        )
    }
}

@Composable
fun PaywallBody(
    paywall: Paywall,
    stripePrices: Map<String, StripePrice>,
    account: Account?,
    onFtcPay: (item: CartItemFtcV2) -> Unit,
    onStripePay: (item: CartItemStripeV2) -> Unit,
    loginButton: @Composable () -> Unit,
    customerService: @Composable () -> Unit,
) {
    val membership = account?.membership?.normalize() ?: Membership()

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(Dimens.dp8)
    ) {

        if (account == null) {
            loginButton()
        }
        
        SubsStatusBox(membership = membership)

        if (paywall.isPromoValid()) {
            PromoBox(banner = paywall.promo)
        }
        
        paywall.products.forEach { product ->
            val ftcItems = product.listShoppingItems(membership)

            ProductCard(
                heading = product.heading,
                description = product.descWithDailyCost(),
                smallPrint = product.smallPrint,
                priceContent = {
                    PriceList(
                        ftcCartItems = ftcItems,
                        stripeCartItems = StripePriceIDsOfProduct
                            .newInstance(ftcItems)
                            .listShoppingItems(stripePrices, membership),
                        onFtcPay = onFtcPay,
                        onStripePay = onStripePay
                    )
                }
            )
            Spacer(modifier = Modifier.height(Dimens.dp16))
        }

        SubsRuleContent()

        Spacer(modifier = Modifier.height(Dimens.dp16))

        customerService()
    }
}

@Composable
private fun PaywallLogin(
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = stringResource(id = R.string.paywall_login_prompt))
    }
}

@Composable
fun SubsStatusBox(
    membership: Membership
) {
    if (membership.tier == null) {
        return
    }

    if (!membership.autoRenewOffExpired) {
        return
    }

    Text(
        text = stringResource(
            R.string.member_expired_on,
            FormatHelper.getTier(LocalContext.current, membership.tier),
            membership.localizeExpireDate()
        ),
        style = MaterialTheme.typography.body1
    )
}

@Composable
fun SubsRuleContent() {
    MarkdownText(markdown = paywallGuide)
}

@Preview(showBackground = true)
@Composable
fun PreviewPaywallContent() {
    PaywallBody(
        paywall = defaultPaywall,
        stripePrices = mapOf(),
        account = null,
        onFtcPay = {},
        onStripePay = {},
        loginButton = {
            PaywallLogin {

            }
        },
        customerService = {
            CustomerService(onError = {})
        }
    )
}
