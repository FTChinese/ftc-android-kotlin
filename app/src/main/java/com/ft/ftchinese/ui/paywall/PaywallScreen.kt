package com.ft.ftchinese.ui.paywall

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.paywall.*
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.stripesubs.StripePrice
import com.ft.ftchinese.ui.product.PriceList
import com.ft.ftchinese.ui.product.ProductCard
import com.ft.ftchinese.ui.theme.Dimens
import org.threeten.bp.LocalDate

@Composable
fun PaywallScreen(
    vm: PaywallViewModel,
    account: Account,
    scaffoldState: ScaffoldState,
    onFtcPay: (item: CartItemFtcV2) -> Unit,
    onStripePay: (item: CartItemStripeV2) -> Unit,
) {

    vm.loadPaywall(account.isTest)
    vm.loadStripePrices()

    vm.msgId?.let {
        Toast(
            scaffoldState = scaffoldState,
            message = stringResource(id = it),
        )
    }

    vm.errMsg?.let {
        Toast(
            scaffoldState = scaffoldState,
            message = it,
        )
    }

    PaywallBody(
        paywall = vm.paywallState,
        stripePrices = vm.stripeState,
        membership = account.membership,
        onFtcPay = onFtcPay,
        onStripePay = onStripePay
    )
}

@Composable
fun PaywallBody(
    paywall: Paywall,
    stripePrices: Map<String, StripePrice>,
    membership: Membership,
    onFtcPay: (item: CartItemFtcV2) -> Unit,
    onStripePay: (item: CartItemStripeV2) -> Unit,
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(Dimens.dp8)
    ) {
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
    }
}

@Preview
@Composable
fun PreviewPaywallContent() {
    PaywallBody(
        paywall = defaultPaywall,
        stripePrices = mapOf(),
        membership = Membership(
            tier = Tier.STANDARD,
            cycle = Cycle.YEAR,
            expireDate = LocalDate.now().plusMonths(6),
            payMethod = PayMethod.ALIPAY,
        ),
        onFtcPay = {},
        onStripePay = {},
    )
}
