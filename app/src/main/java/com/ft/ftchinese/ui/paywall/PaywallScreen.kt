package com.ft.ftchinese.ui.paywall

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.model.paywall.*
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.stripesubs.StripePrice
import com.ft.ftchinese.ui.components.CustomerService
import com.ft.ftchinese.ui.components.Toast
import com.ft.ftchinese.ui.formatter.FormatHelper
import com.ft.ftchinese.ui.product.PriceList
import com.ft.ftchinese.ui.product.ProductCard
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.viewmodel.AuthViewModel
import dev.jeziellago.compose.markdowntext.MarkdownText

@Composable
fun PaywallScreen(
    paywallViewModel: PaywallViewModel,
    authViewModel: AuthViewModel,
    scaffoldState: ScaffoldState,
    onFtcPay: (item: CartItemFtcV2) -> Unit,
    onStripePay: (item: CartItemStripeV2) -> Unit,
    onClickLogin: () -> Unit,
) {

    LaunchedEffect(key1 = Unit) {
        paywallViewModel.loadPaywall(authViewModel.account?.isTest ?: false)
        paywallViewModel.loadStripePrices()
    }

    paywallViewModel.msgId?.let {
        Toast(
            scaffoldState = scaffoldState,
            message = stringResource(id = it),
        )
    }

    paywallViewModel.errMsg?.let {
        Toast(
            scaffoldState = scaffoldState,
            message = it,
        )
    }

    PaywallBody(
        paywall = paywallViewModel.paywallState,
        stripePrices = paywallViewModel.stripeState,
        account = authViewModel.account,
        onFtcPay = onFtcPay,
        onStripePay = onStripePay,
        loginContent = {
            PaywallLogin(onClick = onClickLogin)
        },
        emailContent = {
            CustomerService()
        }
    )
}

@Composable
fun PaywallBody(
    paywall: Paywall,
    stripePrices: Map<String, StripePrice>,
    account: Account?,
    onFtcPay: (item: CartItemFtcV2) -> Unit,
    onStripePay: (item: CartItemStripeV2) -> Unit,
    loginContent: @Composable () -> Unit,
    emailContent: @Composable () -> Unit,
) {
    val membership = account?.membership?.normalize() ?: Membership()

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(Dimens.dp8)
    ) {

        if (account == null) {
            loginContent()
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

        emailContent()
    }
}

@Composable
fun PaywallLogin(
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

@Preview
@Composable
fun PreviewPaywallContent() {
    PaywallBody(
        paywall = defaultPaywall,
        stripePrices = mapOf(),
        account = null,
        onFtcPay = {},
        onStripePay = {},
        loginContent = {
            PaywallLogin {

            }
        },
        emailContent = {
            CustomerService()
        }
    )
}
