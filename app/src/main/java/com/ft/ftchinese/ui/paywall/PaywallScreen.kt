package com.ft.ftchinese.ui.paywall

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.model.paywall.CartItemFtc
import com.ft.ftchinese.model.paywall.CartItemStripe
import com.ft.ftchinese.model.paywall.Paywall
import com.ft.ftchinese.model.paywall.defaultPaywall
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.ui.components.CustomerService
import com.ft.ftchinese.ui.formatter.FormatHelper
import com.ft.ftchinese.ui.product.ProductCard
import com.ft.ftchinese.ui.product.SubsRuleContent
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun PaywallScreen(
    paywall: Paywall,
    account: Account?,
    onFtcPay: (item: CartItemFtc) -> Unit,
    onStripePay: (item: CartItemStripe) -> Unit,
    onLoginRequest: () -> Unit,
) {
    val membership = account?.membership?.normalize() ?: Membership()

    val productItems = paywall.buildUiProducts(membership)

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(Dimens.dp8)
    ) {

        if (account == null) {
            PaywallLogin(onClick = onLoginRequest)
        }
        
        SubsStatusBox(membership = membership)

        if (paywall.isPromoValid()) {
            PromoBox(banner = paywall.promo)
        }
        
        productItems.forEach { item ->

            ProductCard(
                item = item,
                onFtcPay = onFtcPay,
                onStripePay = onStripePay
            )
            Spacer(modifier = Modifier.height(Dimens.dp16))
        }

        SubsRuleContent()

        Spacer(modifier = Modifier.height(Dimens.dp16))

        CustomerService()
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

@Preview(showBackground = true)
@Composable
fun PreviewPaywallContent() {
    PaywallScreen(
        paywall = defaultPaywall,
        account = null,
        onFtcPay = {},
        onStripePay = {},
    ) {}
}
