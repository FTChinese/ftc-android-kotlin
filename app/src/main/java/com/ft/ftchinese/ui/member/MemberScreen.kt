package com.ft.ftchinese.ui.member

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.components.SubsRuleContent
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor
import org.threeten.bp.LocalDate

@Composable
fun MemberScreen(
    member: Membership,
    loading: Boolean,
    onSubsOption: (SubsOptionRow) -> Unit,
) {
    val context = LocalContext.current
    val status = SubsStatus.newInstance(
        ctx = context,
        m = member
    )

    ProgressLayout(
        loading = loading
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(Dimens.dp8)
        ) {
            Text(
                text = "下拉刷新",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = OColor.black60,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(Dimens.dp8))

            SubsStatusCard(status = status)

            Spacer(modifier = Modifier.height(Dimens.dp8))

            SubsOptions(
                cancelStripe = member.canCancelStripe,
                reactivateStripe = status.reactivateStripe,
                onClickRow = onSubsOption
            )

            Spacer(modifier = Modifier.height(Dimens.dp16))

            SubsRuleContent()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMemberScreen() {
    MemberScreen(
        member = Membership(
            tier = Tier.STANDARD,
            cycle = Cycle.YEAR,
            expireDate = LocalDate.now(),
            payMethod = PayMethod.ALIPAY,
            standardAddOn = 30,
            premiumAddOn = 20,
        ),
        loading = false,
        onSubsOption = {}
    )
}




