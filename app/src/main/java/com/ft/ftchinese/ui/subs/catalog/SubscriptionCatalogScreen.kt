package com.ft.ftchinese.ui.subs.catalog

import android.util.Log
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
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.paywall.IntentKind
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.subscriptioncatalog.SubscriptionCatalog
import com.ft.ftchinese.model.subscriptioncatalog.SubscriptionCatalogOption
import com.ft.ftchinese.model.subscriptioncatalog.SubscriptionCatalogPlan
import com.ft.ftchinese.model.subscriptioncatalog.SubscriptionCatalogProduct
import com.ft.ftchinese.model.subscriptioncatalog.SubscriptionCatalogSummary
import com.ft.ftchinese.ui.components.CustomerService
import com.ft.ftchinese.ui.components.PrimaryBlockButton
import com.ft.ftchinese.ui.components.SubsRuleContent
import com.ft.ftchinese.ui.subs.StripeAutoRenewUiState
import com.ft.ftchinese.ui.subs.stripeAutoRenewUiState
import com.ft.ftchinese.ui.theme.OColor
import com.ft.ftchinese.ui.theme.OColors

private const val PURCHASE_FLOW_TAG = "FTCPurchaseFlow"

@Composable
fun SubscriptionCatalogScreen(
    catalog: SubscriptionCatalog,
    membership: Membership?,
    isLoggedIn: Boolean,
    autoOpenPaymentDialogTier: Tier? = null,
    onLoginRequest: () -> Unit,
    onFtcCheckout: (
        product: SubscriptionCatalogProduct,
        plan: SubscriptionCatalogPlan,
        option: SubscriptionCatalogOption,
        payMethod: PayMethod,
    ) -> Unit,
    onStripeCheckout: (priceId: String, trialId: String?, couponId: String?) -> Unit,
    onStripeAutoRenewChange: (Boolean) -> Unit,
) {
    var pendingSelection by remember { mutableStateOf<PendingSelection?>(null) }
    val preferredLanguage = catalog.preferredLanguage
    val checkoutMembership = membership ?: Membership()
    val displayPreference = catalog.summary.displayPreference(
        membership = checkoutMembership,
        preferredLanguage = preferredLanguage,
    )
    var autoOpenAttempted by remember(autoOpenPaymentDialogTier) {
        mutableStateOf(false)
    }

    LaunchedEffect(
        autoOpenPaymentDialogTier,
        catalog,
        checkoutMembership,
        preferredLanguage,
        displayPreference,
    ) {
        val tier = autoOpenPaymentDialogTier ?: return@LaunchedEffect
        if (autoOpenAttempted) {
            return@LaunchedEffect
        }
        autoOpenAttempted = true

        pendingSelection = findAutoPaymentSelection(
            catalog = catalog,
            membership = checkoutMembership,
            preferredLanguage = preferredLanguage,
            displayPreference = displayPreference,
            tier = tier,
        )
    }

    if (pendingSelection != null) {
        PaymentMethodDialog(
            product = pendingSelection!!.product,
            plan = pendingSelection!!.plan,
            membership = checkoutMembership,
            preferredLanguage = preferredLanguage,
            displayPreference = displayPreference,
            onDismiss = { pendingSelection = null },
            onChoose = { choice ->
                if (!choice.enabled) {
                    return@PaymentMethodDialog
                }
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

        val autoRenewUiState = checkoutMembership.stripeAutoRenewUiState(preferredLanguage)
        if (autoRenewUiState.visible) {
            StripeAutoRenewCard(
                state = autoRenewUiState,
                onCheckedChange = onStripeAutoRenewChange,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        catalog.products.forEach { product ->
            MembershipTierCard(
                product = product,
                membership = checkoutMembership,
                preferredLanguage = preferredLanguage,
                displayPreference = displayPreference,
                onPlanAction = { plan ->
                    val choices = paymentChoicesForPlan(
                        product = product,
                        plan = plan,
                        membership = checkoutMembership,
                        preferredLanguage = preferredLanguage,
                        displayPreference = displayPreference,
                    )
                    val directStripeChoice = choices.firstOrNull {
                        it.enabled && it.isDirectStripeUpdate
                    }
                    when {
                        choices.none { it.enabled } -> Unit
                        directStripeChoice != null -> onStripeCheckout(
                            directStripeChoice.option.checkout.stripePriceId,
                            directStripeChoice.option.checkout.stripeTrialPriceId.ifBlank { null },
                            directStripeChoice.option.checkout.stripeCouponId.ifBlank { null }
                        )
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
private fun StripeAutoRenewCard(
    state: StripeAutoRenewUiState,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = MaterialTheme.colors.primarySurface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.status,
                        style = MaterialTheme.typography.body2,
                        color = OColor.teal,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Switch(
                    checked = state.checked,
                    onCheckedChange = onCheckedChange,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = state.detail,
                style = MaterialTheme.typography.body2,
                color = OColors.black50Default,
            )
        }
    }
}

@Composable
private fun MembershipTierCard(
    product: SubscriptionCatalogProduct,
    membership: Membership,
    preferredLanguage: String,
    displayPreference: CatalogDisplayPreference,
    onPlanAction: (SubscriptionCatalogPlan) -> Unit,
) {
    val currentTier = membership.isActiveMember() && product.tier == membership.tier

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
                fontSize = 28.sp,
                modifier = Modifier.fillMaxWidth()
            )
            if (currentTier) {
                Spacer(modifier = Modifier.height(10.dp))
                CurrentTierBadge(membership, preferredLanguage)
            }

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
                    membership = membership,
                    preferredLanguage = preferredLanguage,
                    displayPreference = displayPreference,
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
    membership: Membership,
    preferredLanguage: String,
    displayPreference: CatalogDisplayPreference,
    onAction: () -> Unit,
) {
    val primaryOption = displayOptionForPlan(plan, displayPreference)
    val planState = planCheckoutState(product, plan, membership, preferredLanguage, displayPreference)

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

            planState.message?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.body2,
                    color = if (planState.enabled) OColor.claret else OColors.black50Default,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            PrimaryBlockButton(
                onClick = onAction,
                text = planState.actionText,
                enabled = planState.enabled
            )
        }
    }
}

@Composable
private fun PaymentMethodDialog(
    product: SubscriptionCatalogProduct,
    plan: SubscriptionCatalogPlan,
    membership: Membership,
    preferredLanguage: String,
    displayPreference: CatalogDisplayPreference,
    onDismiss: () -> Unit,
    onChoose: (PaymentChoice) -> Unit,
) {
    val choices = paymentChoicesForPlan(product, plan, membership, preferredLanguage, displayPreference)
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
    val enabledAlpha = if (choice.enabled) 1f else 0.48f

    Card(
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.primarySurface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(enabledAlpha)
            .clickable(enabled = choice.enabled, onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = paymentChoiceLabel(choice, preferredLanguage),
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (choice.current) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentLabel(preferredLanguage),
                        style = MaterialTheme.typography.caption,
                        color = OColor.teal,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = paymentChoicePrice(choice, preferredLanguage),
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (choice.option.originalPrice.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = plainText(choice.option.originalPrice),
                    style = MaterialTheme.typography.caption,
                    color = OColors.black50Default,
                    textDecoration = TextDecoration.LineThrough,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = paymentChoiceHint(choice, preferredLanguage),
                style = MaterialTheme.typography.body2,
                color = OColors.black50Default
            )
            if (choice.reason.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = choice.reason,
                    style = MaterialTheme.typography.caption,
                    color = OColor.claret
                )
            }
        }
    }
}

@Composable
private fun CurrentTierBadge(
    membership: Membership,
    preferredLanguage: String,
) {
    val zh = preferredLanguage.startsWith("zh", ignoreCase = true)
    val expire = membership.localizeCurrentTierEntitlementExpireDate().ifBlank { "" }
    val label = when {
        zh && expire.isNotBlank() -> "✓ 当前 · $expire"
        zh -> "✓ 当前"
        expire.isNotBlank() -> "✓ Current · $expire"
        else -> "✓ Current"
    }

    Box(
        modifier = Modifier
            .background(OColor.teal.copy(alpha = 0.12f), RoundedCornerShape(100))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = OColor.teal,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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

internal fun SubscriptionCatalogSummary.displayPreference(
    membership: Membership,
    preferredLanguage: String,
): CatalogDisplayPreference {
    val active = membership.isActiveMember()
    val payMethod = membership.normalizedPayMethod

    if (active && payMethod == PayMethod.STRIPE) {
        return CatalogDisplayPreference(
            kind = CatalogDisplayKind.STRIPE,
            stripeCurrency = stripeCurrency.lowercase().ifBlank { null },
        )
    }

    if (active && payMethod.isOneTimePay()) {
        return CatalogDisplayPreference(CatalogDisplayKind.FTC)
    }

    return CatalogDisplayPreference.fromLanguage(preferredLanguage)
}

internal fun displayOptionForPlan(
    plan: SubscriptionCatalogPlan,
    displayPreference: CatalogDisplayPreference,
): SubscriptionCatalogOption? {
    val options = plan.checkoutOptions()
    if (options.isEmpty()) {
        return null
    }

    return when (displayPreference.kind) {
        CatalogDisplayKind.FTC -> options.firstOrNull { it.kind == "ftc" } ?: options.first()
        CatalogDisplayKind.STRIPE -> {
            val stripeOptions = options.filter { it.kind == "stripe" }
            if (stripeOptions.isEmpty()) {
                options.first()
            } else {
                displayPreference.stripeCurrency
                    ?.let { currency ->
                        stripeOptions.firstOrNull {
                            it.checkout.stripeCurrency.equals(currency, ignoreCase = true)
                        }
                    }
                    ?: stripeOptions.first()
            }
        }
    }
}

internal fun paymentChoicesForPlan(
    product: SubscriptionCatalogProduct,
    plan: SubscriptionCatalogPlan,
    membership: Membership,
    preferredLanguage: String,
    displayPreference: CatalogDisplayPreference = CatalogDisplayPreference.fromLanguage(preferredLanguage),
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
    }.map { choice ->
        choice.withCheckoutGuard(product, plan, membership, preferredLanguage)
    }.sortedBy { choice ->
        choice.displayOrder(displayPreference)
    }
}

internal fun planCheckoutState(
    product: SubscriptionCatalogProduct,
    plan: SubscriptionCatalogPlan,
    membership: Membership,
    preferredLanguage: String,
    displayPreference: CatalogDisplayPreference = CatalogDisplayPreference.fromLanguage(preferredLanguage),
): PlanCheckoutState {
    val choices = paymentChoicesForPlan(product, plan, membership, preferredLanguage, displayPreference)
    val enabled = choices.any { it.enabled }
    val currentTier = membership.isActiveMember() && membership.tier == product.tier
    val currentPlan = currentTier && membership.cycle == plan.toCycle()
    val activeApple = membership.isActiveMember() && membership.normalizedPayMethod == PayMethod.APPLE
    val pendingStripeChange = membership.pendingStripeChange
    val stripeChoice = choices.firstOrNull { it.payMethod == PayMethod.STRIPE && it.enabled }
    val stripeIntentKind = stripeChoice?.intentKind
    val activeStripeChange = enabled &&
        membership.isActiveMember() &&
        membership.normalizedPayMethod == PayMethod.STRIPE &&
        product.tier != null &&
        membership.tier != product.tier
    val zh = preferredLanguage.startsWith("zh", ignoreCase = true)
    val oneTimeCurrent = currentPlan && membership.normalizedPayMethod.isOneTimePay()
    val productTierLabel = product.tier?.label().orEmpty()
    val pendingTierLabel = pendingStripeChange?.targetTier?.label().orEmpty()

    val message = when {
        membership.vip -> if (zh) "VIP 无需订阅" else "VIP does not need a subscription"
        pendingStripeChange?.isDowngrade == true && currentTier ->
            if (zh) {
                "当前仍为$productTierLabel；下次续订已安排转为$pendingTierLabel，可选择保留当前方案"
            } else {
                "You still have $productTierLabel. The next renewal is scheduled to switch to $pendingTierLabel"
            }
        pendingStripeChange?.targets(product.tier) == true ->
            if (zh) "已安排下次续订起转为$productTierLabel" else "Scheduled for the next renewal"
        activeApple ->
            if (zh) "当前为苹果自动续订，请在苹果设备上管理订阅" else "Current subscription is managed by Apple"
        currentTier && membership.normalizedPayMethod == PayMethod.STRIPE ->
            if (zh) "当前为信用卡/借记卡自动续订，不能重复购买同级别会员" else "Current card subscription cannot be purchased again"
        oneTimeCurrent && !enabled ->
            if (zh) "到期时间已超过最长续订期限，暂不能继续叠加购买" else "Renewal limit reached"
        oneTimeCurrent ->
            if (zh) "当前方案，可继续叠加购买" else "Current plan. You may renew again"
        else -> null
    }

    val actionText = when {
        stripeIntentKind == IntentKind.CancelScheduledChange ->
            if (zh) "保留高端续订" else "Keep Premium Renewal"
        pendingStripeChange?.targets(product.tier) == true ->
            if (zh) "已安排转为$productTierLabel" else "Scheduled"
        activeApple && currentTier -> currentPlanLabel(preferredLanguage)
        activeApple -> unavailableLabel(preferredLanguage)
        activeStripeChange -> stripeChangePlanLabel(product, preferredLanguage)
        enabled && oneTimeCurrent -> renewPlanLabel(plan, preferredLanguage)
        enabled -> planPurchaseLabel(product, plan, preferredLanguage)
        currentTier -> currentPlanLabel(preferredLanguage)
        else -> unavailableLabel(preferredLanguage)
    }

    return PlanCheckoutState(
        currentTier = currentTier,
        currentPlan = currentPlan,
        enabled = enabled,
        actionText = actionText,
        message = message,
    )
}

private fun PaymentChoice.withCheckoutGuard(
    product: SubscriptionCatalogProduct,
    plan: SubscriptionCatalogPlan,
    membership: Membership,
    preferredLanguage: String,
): PaymentChoice {
    val zh = preferredLanguage.startsWith("zh", ignoreCase = true)
    val targetTier = product.tier
    val active = membership.isActiveMember()
    val currentTier = active && targetTier != null && membership.tier == targetTier
    val currentPlan = currentTier && membership.cycle == plan.toCycle()
    val isOneTime = payMethod.isOneTimePay()
    val currentPaymentMethod = when (payMethod) {
        PayMethod.STRIPE -> membership.normalizedPayMethod == PayMethod.STRIPE
        PayMethod.WXPAY,
        PayMethod.ALIPAY -> membership.normalizedPayMethod == payMethod
        else -> false
    }
    val stripeIntent = if (kind == "stripe") {
        buildCatalogStripeCartItem(
            membership = membership,
            product = product,
            plan = plan,
            option = option,
        )?.intent
    } else {
        null
    }

    val reason = when {
        option.disabled && stripeIntent?.kind != IntentKind.CancelScheduledChange -> option.ctaText.ifBlank {
            if (zh) "暂不可购买" else "Unavailable"
        }
        membership.vip -> if (zh) "VIP 无需订阅" else "VIP does not need a subscription"
        stripeIntent?.kind == IntentKind.Forbidden -> stripeIntent.message
        active && membership.normalizedPayMethod == PayMethod.APPLE ->
            if (zh) "当前为苹果自动续订，请在苹果设备上管理或取消后再购买" else "Managed by Apple. Please use an Apple device to manage this subscription"
        active && membership.normalizedPayMethod == PayMethod.STRIPE && isOneTime ->
            if (zh) "当前为信用卡/借记卡自动续订，请继续使用信用卡/借记卡管理升级或降级" else "Please manage this card subscription by credit/debit card"
        currentTier && membership.normalizedPayMethod.isOneTimePay() && isOneTime && membership.beyondMaxRenewalPeriod() ->
            if (zh) "到期时间已超过最长续订期限，暂不能继续叠加购买" else "Renewal limit reached"
        else -> ""
    }

    return copy(
        enabled = reason.isBlank(),
        reason = reason,
        current = currentPlan && currentPaymentMethod,
        intentKind = stripeIntent?.kind,
    )
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
    val currency = paymentChoiceCurrency(choice)
    return when (choice.payMethod) {
        PayMethod.WXPAY ->
            if (zh) "人民币一次性购买，使用微信完成支付" else "One-off purchase in CNY via WeChat Pay"
        PayMethod.ALIPAY ->
            if (zh) "人民币一次性购买，使用支付宝完成支付" else "One-off purchase in CNY via Alipay"
        PayMethod.STRIPE ->
            when (choice.intentKind) {
                IntentKind.Downgrade ->
                    if (zh) "保留当前权益到周期结束，下次续订起按新方案扣费" else "Keeps current access until the next renewal"
                IntentKind.CancelScheduledChange ->
                    if (zh) "取消已安排的降级，下次续订继续按当前方案扣费" else "Cancels the scheduled downgrade"
                else ->
                    if (zh) "信用卡/借记卡自动续订，将以 $currency 扣费" else "Auto-renews in $currency by credit/debit card"
            }
        else -> ""
    }
}

private fun paymentChoicePrice(
    choice: PaymentChoice,
    preferredLanguage: String,
): String {
    val price = plainText(choice.option.displayPrice)
    if (price.isBlank()) {
        return ""
    }

    return if (preferredLanguage.startsWith("zh", ignoreCase = true)) {
        "应付 $price"
    } else {
        "Pay $price"
    }
}

private fun paymentChoiceCurrency(choice: PaymentChoice): String {
    return when (choice.payMethod) {
        PayMethod.STRIPE -> choice.option.checkout.stripeCurrency.ifBlank { "CNY" }.uppercase()
        PayMethod.WXPAY,
        PayMethod.ALIPAY -> "CNY"
        else -> ""
    }
}

private fun prefersCnyPayment(preferredLanguage: String): Boolean {
    val normalized = preferredLanguage.lowercase()
    return normalized == "zh" || normalized == "zh-cn"
}

private fun stripeCurrencyForLanguage(preferredLanguage: String): String? {
    return when (preferredLanguage.lowercase()) {
        "zh-tw" -> "twd"
        "zh-hk" -> "hkd"
        "en-gb",
        "en-uk" -> "gbp"
        "en-au" -> "aud"
        "en-us",
        "en" -> "usd"
        else -> null
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

private fun currentLabel(preferredLanguage: String): String {
    return if (preferredLanguage.startsWith("zh", ignoreCase = true)) {
        "当前"
    } else {
        "Current"
    }
}

private fun currentPlanLabel(preferredLanguage: String): String {
    return if (preferredLanguage.startsWith("zh", ignoreCase = true)) {
        "当前方案"
    } else {
        "Current Plan"
    }
}

private fun unavailableLabel(preferredLanguage: String): String {
    return if (preferredLanguage.startsWith("zh", ignoreCase = true)) {
        "暂不可购买"
    } else {
        "Unavailable"
    }
}

private fun renewPlanLabel(
    plan: SubscriptionCatalogPlan,
    preferredLanguage: String,
): String {
    val zh = preferredLanguage.startsWith("zh", ignoreCase = true)
    val isMonthly = plan.cycle.equals("month", ignoreCase = true) || plainText(plan.title).contains("月")

    return when {
        zh && isMonthly -> "续订月度订阅"
        zh -> "续订年度订阅"
        isMonthly -> "Renew Monthly"
        else -> "Renew Annual"
    }
}

private fun stripeChangePlanLabel(
    product: SubscriptionCatalogProduct,
    preferredLanguage: String,
): String {
    val zh = preferredLanguage.startsWith("zh", ignoreCase = true)
    val isPremium = product.tier?.name.equals("premium", ignoreCase = true)

    return when {
        zh && isPremium -> "升级高端会员"
        zh -> "转为标准会员"
        isPremium -> "Upgrade to Premium"
        else -> "Switch to Standard"
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

    val parsed = try {
        HtmlCompat
            .fromHtml(normalized, HtmlCompat.FROM_HTML_MODE_LEGACY)
            .toString()
    } catch (_: NullPointerException) {
        normalized
    }

    return parsed
        .replace('\u00A0', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun SubscriptionCatalogPlan.toCycle(): Cycle? {
    return Cycle.fromString(cycle.lowercase())
}

private fun Membership.isActiveMember(): Boolean {
    return vip || (tier != null && !expired && !isInvalidStripe)
}

private fun PayMethod?.isOneTimePay(): Boolean {
    return this == PayMethod.ALIPAY || this == PayMethod.WXPAY
}

private fun com.ft.ftchinese.model.enums.Tier.label(): String {
    return when (this.name.lowercase()) {
        "standard" -> "标准会员"
        "premium" -> "高端会员"
        else -> name
    }
}

private fun findAutoPaymentSelection(
    catalog: SubscriptionCatalog,
    membership: Membership,
    preferredLanguage: String,
    displayPreference: CatalogDisplayPreference,
    tier: Tier,
): PendingSelection? {
    val products = catalog.products.filter { product ->
        product.tier == tier
    }

    for (product in products) {
        val plans = product.plans.sortedBy(::autoPaymentPlanOrder)
        for (plan in plans) {
            val choices = paymentChoicesForPlan(
                product = product,
                plan = plan,
                membership = membership,
                preferredLanguage = preferredLanguage,
                displayPreference = displayPreference,
            ).filter { it.enabled }

            if (choices.isNotEmpty()) {
                Log.i(
                    PURCHASE_FLOW_TAG,
                    "auto_payment_dialog open tier=${product.tier?.symbol.orEmpty()} " +
                        "cycle=${plan.cycle} choices=${choices.joinToString(",") { choice ->
                            "${choice.kind}:${choice.payMethod}"
                        }}"
                )
                return PendingSelection(product, plan)
            }
        }
    }

    Log.i(
        PURCHASE_FLOW_TAG,
        "auto_payment_dialog no_match tier=${tier.symbol} productCount=${catalog.products.size}"
    )
    return null
}

private fun autoPaymentPlanOrder(plan: SubscriptionCatalogPlan): Int {
    val normalized = plan.cycle.lowercase()
    return when {
        Cycle.fromString(normalized) == Cycle.YEAR || normalized.contains("year") -> 0
        Cycle.fromString(normalized) == Cycle.MONTH || normalized.contains("month") -> 1
        else -> 2
    }
}

private data class PendingSelection(
    val product: SubscriptionCatalogProduct,
    val plan: SubscriptionCatalogPlan,
)

internal enum class CatalogDisplayKind {
    FTC,
    STRIPE,
}

internal data class CatalogDisplayPreference(
    val kind: CatalogDisplayKind,
    val stripeCurrency: String? = null,
) {
    companion object {
        fun fromLanguage(preferredLanguage: String): CatalogDisplayPreference {
            return if (prefersCnyPayment(preferredLanguage)) {
                CatalogDisplayPreference(CatalogDisplayKind.FTC)
            } else {
                CatalogDisplayPreference(
                    kind = CatalogDisplayKind.STRIPE,
                    stripeCurrency = stripeCurrencyForLanguage(preferredLanguage),
                )
            }
        }
    }
}

internal data class PlanCheckoutState(
    val currentTier: Boolean,
    val currentPlan: Boolean,
    val enabled: Boolean,
    val actionText: String,
    val message: String?,
)

internal data class PaymentChoice(
    val option: SubscriptionCatalogOption,
    val kind: String,
    val payMethod: PayMethod?,
    val enabled: Boolean = true,
    val reason: String = "",
    val current: Boolean = false,
    val intentKind: IntentKind? = null,
) {
    val isDirectStripeUpdate: Boolean
        get() = kind == "stripe" && (
            intentKind == IntentKind.Downgrade ||
                intentKind == IntentKind.CancelScheduledChange
            )

    fun displayOrder(displayPreference: CatalogDisplayPreference): Int {
        val baseOrder = when (displayPreference.kind) {
            CatalogDisplayKind.FTC -> when (payMethod) {
                PayMethod.WXPAY -> 0
                PayMethod.ALIPAY -> 1
                PayMethod.STRIPE -> 2
                else -> 9
            }
            CatalogDisplayKind.STRIPE -> when (payMethod) {
                PayMethod.STRIPE -> 0
                PayMethod.WXPAY -> 1
                PayMethod.ALIPAY -> 2
                else -> 9
            }
        }
        val currencyOrder = if (
            displayPreference.kind == CatalogDisplayKind.STRIPE &&
            payMethod == PayMethod.STRIPE &&
            !displayPreference.stripeCurrency.isNullOrBlank()
        ) {
            if (option.checkout.stripeCurrency.equals(displayPreference.stripeCurrency, ignoreCase = true)) {
                0
            } else {
                1
            }
        } else {
            0
        }

        return baseOrder * 10 + currencyOrder
    }
}
