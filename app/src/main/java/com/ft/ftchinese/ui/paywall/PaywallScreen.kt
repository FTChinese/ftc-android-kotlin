package com.ft.ftchinese.ui.paywall

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.paywall.Paywall
import com.ft.ftchinese.model.paywall.defaultPaywall
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.ui.product.ProductCard
import com.ft.ftchinese.ui.theme.Space
import org.threeten.bp.LocalDate

@Composable
fun PaywallScreen(paywall: Paywall, membership: Membership) {
    Column {
        if (paywall.isPromoValid()) {
            PromoBox(banner = paywall.promo)
        }

        paywall.products.forEach { product ->
            ProductCard(
                product = product,
                membership = membership,
            )
        }

        Spacer(modifier = Modifier.height(Space.dp16))

        SubsRuleContent()
    }
}

@Preview
@Composable
fun PreviewPaywallScreen() {
    PaywallScreen(
        paywall = defaultPaywall,
        membership = Membership(
            tier = Tier.STANDARD,
            cycle = Cycle.YEAR,
            expireDate = LocalDate.now().plusMonths(6),
            payMethod = PayMethod.ALIPAY,
        )
    )
}
