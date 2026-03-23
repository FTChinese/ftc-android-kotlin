package com.ft.ftchinese.ui.subs.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.subscriptioncatalog.SubscriptionCatalog
import com.ft.ftchinese.model.subscriptioncatalog.SubscriptionCatalogOption
import com.ft.ftchinese.model.subscriptioncatalog.SubscriptionCatalogPlan
import com.ft.ftchinese.model.subscriptioncatalog.SubscriptionCatalogProduct
import com.ft.ftchinese.ui.components.CustomerService
import com.ft.ftchinese.ui.components.PrimaryBlockButton
import com.ft.ftchinese.ui.components.SubsRuleContent
import com.ft.ftchinese.ui.theme.OColor
import com.ft.ftchinese.ui.theme.OColors

@Composable
fun SubscriptionCatalogScreen(
    catalog: SubscriptionCatalog,
    isLoggedIn: Boolean,
    onLoginRequest: () -> Unit,
    onFtcCheckout: (
        product: SubscriptionCatalogProduct,
        plan: SubscriptionCatalogPlan,
        option: SubscriptionCatalogOption,
        payMethod: PayMethod,
    ) -> Unit,
    onStripeCheckout: (priceId: String, trialId: String?, couponId: String?) -> Unit,
) {
    var pendingSelection by remember { mutableStateOf<PendingSelection?>(null) }
    val preferredLanguage = catalog.preferredLanguage

    if (pendingSelection != null) {
        PaymentMethodDialog(
            product = pendingSelection!!.product,
            plan = pendingSelection!!.plan,
            preferredLanguage = preferredLanguage,
            onDismiss = { pendingSelection = null },
            onChoose = { choice ->
                val current = pendingSelection ?: return@PaymentMethodDialog
                pendingSelection = null
                when (choice.kind) {
                    "stripe" -> onStripeCheckout(
                        choice.option.checkout.stripePriceId,
                        choice.option.checkout.stripeTrialPriceId.ifBlank { null },
                        choice.option.checkout.stripeCouponId.ifBlank { null }
                    )

                    "ftc" -> onFtcCheckout(
                        current.product,
                        current.plan,
                        choice.option,
                        choice.payMethod ?: return@PaymentMethodDialog
                    )
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        if (!isLoggedIn) {
            PrimaryBlockButton(
                onClick = onLoginRequest,
                text = "登录后订阅"
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        val actionsTitle = plainText(catalog.actionsTitle)
        if (actionsTitle.isNotBlank()) {
            Text(
                text = actionsTitle,
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        catalog.products.forEach { product ->
            MembershipTierCard(
                product = product,
                preferredLanguage = preferredLanguage,
                onPlanAction = { plan ->
                    val choices = paymentChoicesForPlan(plan, preferredLanguage)
                    when {
                        choices.isEmpty() -> Unit
                        else -> pendingSelection = PendingSelection(product, plan)
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        SubsRuleContent()
        Spacer(modifier = Modifier.height(16.dp))
        CustomerService()
    }
}

@Composable
private fun MembershipTierCard(
    product: SubscriptionCatalogProduct,
    preferredLanguage: String,
    onPlanAction: (SubscriptionCatalogPlan) -> Unit,
) {
    Card(
        elevation = 4.dp,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = plainText(product.name),
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            )

            val benefits = product.benefits.map(::plainText).filter { it.isNotBlank() }
            if (benefits.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                benefits.forEach { benefit ->
                    BenefitRow(text = benefit)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            val note = plainText(product.note)
            if (note.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = note,
                    style = MaterialTheme.typography.caption,
                    color = OColors.black50Default
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            product.plans.forEachIndexed { index, plan ->
                if (index > 0) {
                    Divider(
                        color = OColor.black10,
                        modifier = Modifier.padding(vertical = 14.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                }
                PlanCard(
                    product = product,
                    plan = plan,
                    preferredLanguage = preferredLanguage,
                    onAction = { onPlanAction(plan) }
                )
            }
        }
    }
}

@Composable
private fun PlanCard(
    product: SubscriptionCatalogProduct,
    plan: SubscriptionCatalogPlan,
    preferredLanguage: String,
    onAction: () -> Unit,
) {
    val options = plan.checkoutOptions()
    val primaryOption = options.preferredDisplayOption(preferredLanguage)
    val hasCheckout = options.isNotEmpty()
    val actionText = when {
        primaryOption?.isActive == true -> primaryOption.ctaText.ifBlank { "当前方案" }
        else -> planPurchaseLabel(product, plan, preferredLanguage)
    }

    Card(
        elevation = 0.dp,
        backgroundColor = OColor.white,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = plainText(primaryOption?.displayPrice.orEmpty()),
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp
            )

            if (!primaryOption?.originalPrice.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = plainText(primaryOption?.originalPrice.orEmpty()),
                    style = MaterialTheme.typography.body2,
                    color = OColors.black50Default,
                    textDecoration = TextDecoration.LineThrough,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            PrimaryBlockButton(
                onClick = onAction,
                text = actionText,
                enabled = hasCheckout && primaryOption?.disabled != true && primaryOption?.isActive != true
            )
        }
    }
}

@Composable
private fun PaymentMethodDialog(
    product: SubscriptionCatalogProduct,
    plan: SubscriptionCatalogPlan,
    preferredLanguage: String,
    onDismiss: () -> Unit,
    onChoose: (PaymentChoice) -> Unit,
) {
    val choices = paymentChoicesForPlan(plan, preferredLanguage)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = paymentChoiceTitle(preferredLanguage),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = plainText(plan.title),
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = plainText(product.name),
                    style = MaterialTheme.typography.body2,
                    color = OColors.black50Default
                )
                Spacer(modifier = Modifier.height(12.dp))
                choices.forEachIndexed { index, choice ->
                    PaymentMethodOptionCard(
                        choice = choice,
                        preferredLanguage = preferredLanguage,
                        onClick = { onChoose(choice) }
                    )
                    if (index < choices.lastIndex) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        },
        buttons = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(cancelLabel(preferredLanguage))
                }
            }
        }
    )
}

@Composable
private fun PaymentMethodOptionCard(
    choice: PaymentChoice,
    preferredLanguage: String,
    onClick: () -> Unit,
) {
    Card(
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.primarySurface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = paymentChoiceLabel(choice, preferredLanguage),
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = plainText(choice.option.displayPrice),
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = paymentChoiceHint(choice, preferredLanguage),
                style = MaterialTheme.typography.body2,
                color = OColors.black50Default
            )
        }
    }
}

@Composable
private fun BenefitRow(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .width(7.dp)
                .height(7.dp)
                .background(OColor.teal, RoundedCornerShape(100))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = plainText(text),
            style = MaterialTheme.typography.body1
        )
    }
}

private fun SubscriptionCatalogPlan.checkoutOptions(): List<SubscriptionCatalogOption> {
    return options.filter {
        when (it.kind) {
            "stripe" -> it.checkout.stripePriceId.isNotBlank()
            "ftc" -> it.checkout.ftcPriceId.isNotBlank()
            else -> false
        }
    }
}

private fun paymentChoicesForPlan(
    plan: SubscriptionCatalogPlan,
    preferredLanguage: String,
): List<PaymentChoice> {
    return plan.checkoutOptions().flatMap { option ->
        when (option.kind) {
            "ftc" -> listOf(
                PaymentChoice(option, "ftc", PayMethod.WXPAY),
                PaymentChoice(option, "ftc", PayMethod.ALIPAY)
            )
            "stripe" -> listOf(PaymentChoice(option, "stripe", PayMethod.STRIPE))
            else -> emptyList()
        }
    }.sortedBy { choice ->
        when (choice.payMethod) {
            PayMethod.WXPAY -> 0
            PayMethod.ALIPAY -> 1
            PayMethod.STRIPE -> if (preferredLanguage.startsWith("zh", ignoreCase = true)) 2 else 0
            else -> 9
        }
    }
}

private fun List<SubscriptionCatalogOption>.preferredDisplayOption(preferredLanguage: String): SubscriptionCatalogOption? {
    if (isEmpty()) {
        return null
    }

    val preferLocalCurrency = preferredLanguage.startsWith("zh", ignoreCase = true)
    return when {
        preferLocalCurrency -> firstOrNull { it.kind == "ftc" } ?: first()
        else -> firstOrNull { it.kind == "stripe" } ?: first()
    }
}

private fun paymentOptionLabel(
    option: SubscriptionCatalogOption,
    preferredLanguage: String,
): String {
    val zh = preferredLanguage.startsWith("zh", ignoreCase = true)
    return when (option.kind) {
        "ftc" -> if (zh) "支付宝 / 微信支付" else "Alipay / WeChat Pay"
        "stripe" -> if (zh) "信用卡 / 借记卡" else "Credit / Debit Card"
        else -> plainText(option.paymentLabel)
    }
}

private fun paymentChoiceLabel(
    choice: PaymentChoice,
    preferredLanguage: String,
): String {
    val zh = preferredLanguage.startsWith("zh", ignoreCase = true)
    return when (choice.payMethod) {
        PayMethod.WXPAY -> if (zh) "微信支付" else "WeChat Pay"
        PayMethod.ALIPAY -> if (zh) "支付宝" else "Alipay"
        PayMethod.STRIPE -> if (zh) "信用卡 / 借记卡" else "Credit / Debit Card"
        else -> plainText(choice.option.paymentLabel)
    }
}

private fun paymentChoiceHint(
    choice: PaymentChoice,
    preferredLanguage: String,
): String {
    val zh = preferredLanguage.startsWith("zh", ignoreCase = true)
    return when (choice.payMethod) {
        PayMethod.WXPAY ->
            if (zh) "一次性购买，使用微信完成支付" else "One-off purchase via WeChat Pay"
        PayMethod.ALIPAY ->
            if (zh) "一次性购买，使用支付宝完成支付" else "One-off purchase via Alipay"
        PayMethod.STRIPE ->
            if (zh) "自动续订，订阅到期前 24 小时自动扣费" else "Auto-renews 24 hours before expiry"
        else -> ""
    }
}

private fun paymentChoiceLabel(preferredLanguage: String): String {
    return if (preferredLanguage.startsWith("zh", ignoreCase = true)) {
        "选择支付方式"
    } else {
        "Choose Payment Method"
    }
}

private fun paymentChoiceTitle(preferredLanguage: String): String {
    return if (preferredLanguage.startsWith("zh", ignoreCase = true)) {
        "请选择支付方式"
    } else {
        "Choose a Payment Method"
    }
}

private fun cancelLabel(preferredLanguage: String): String {
    return if (preferredLanguage.startsWith("zh", ignoreCase = true)) {
        "取消"
    } else {
        "Cancel"
    }
}

private fun planPurchaseLabel(
    product: SubscriptionCatalogProduct,
    plan: SubscriptionCatalogPlan,
    preferredLanguage: String,
): String {
    val zh = preferredLanguage.startsWith("zh", ignoreCase = true)
    val isPremium = product.tier?.name.equals("premium", ignoreCase = true)
    val isMonthly = plan.cycle.equals("month", ignoreCase = true) || plainText(plan.title).contains("月")

    return when {
        zh && isPremium -> "购买高端订阅"
        zh && isMonthly -> "购买月度订阅"
        zh -> "购买年度订阅"
        isPremium -> "Buy Premium"
        isMonthly -> "Buy Monthly"
        else -> "Buy Annual"
    }
}

private fun plainText(value: String?): String {
    val normalized = value?.trim().orEmpty()
    if (normalized.isBlank()) {
        return ""
    }

    return HtmlCompat
        .fromHtml(normalized, HtmlCompat.FROM_HTML_MODE_LEGACY)
        .toString()
        .replace('\u00A0', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun com.ft.ftchinese.model.enums.Tier.label(): String {
    return when (this.name.lowercase()) {
        "standard" -> "标准会员"
        "premium" -> "高端会员"
        else -> name
    }
}

private data class PendingSelection(
    val product: SubscriptionCatalogProduct,
    val plan: SubscriptionCatalogPlan,
)

private data class PaymentChoice(
    val option: SubscriptionCatalogOption,
    val kind: String,
    val payMethod: PayMethod?,
)
