package com.ft.ftchinese.ui.wxlink.merge

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.LoginMethod
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.reader.Wechat
import com.ft.ftchinese.ui.components.ListItemTwoCol
import com.ft.ftchinese.ui.components.PrimaryBlockButton
import com.ft.ftchinese.ui.components.WeightedColumn
import com.ft.ftchinese.ui.subs.mysubs.SubsStatus
import com.ft.ftchinese.ui.theme.Dimens
import org.threeten.bp.LocalDate

@Composable
fun LinkScreen(
    loading: Boolean,
    params: WxEmailMerger,
    onLink: (Account) -> Unit,
) {
    val context = LocalContext.current
    val linkMock = params.link(context)

    WeightedColumn(
        bottom = {
            PrimaryBlockButton(
                onClick = {
                    linkMock.linked?.let(onLink)
                },
                enabled = !loading && (linkMock.linked != null),
                text = stringResource(id = R.string.btn_start)
            )
        }
    ) {
        Text(
            text = stringResource(id = R.string.link_heading),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h6
        )

        Spacer(modifier = Modifier.height(Dimens.dp16))

        LinkDetails(
            title = stringResource(id = R.string.label_ftc_account),
            subTitle = params.ftc.email,
            subsDetails = buildSubsDetails(
                context,
                params.ftc.membership
            )
        )

        Spacer(modifier = Modifier.height(Dimens.dp8))

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_link_teal_24dp),
                contentDescription = ""
            )
        }

        Spacer(modifier = Modifier.height(Dimens.dp8))

        LinkDetails(
            title = stringResource(id = R.string.label_wx_account),
            subTitle = params.wx.wechat.nickname ?: "",
            subsDetails = buildSubsDetails(
                context = context,
                member = params.wx.membership
            )
        )

        Spacer(modifier = Modifier.height(Dimens.dp16))

        linkMock.denied?.let {
            Text(
                text = it,
                modifier = Modifier.fillMaxWidth()
            )
        }

    }
}

@Composable
fun LinkDetails(
    title: String,
    subTitle: String,
    subsDetails: List<Pair<String, String>>,
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.dp16)
        ) {
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.subtitle1
            )

            Text(
                text = subTitle,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.subtitle2
            )

            Spacer(modifier = Modifier.height(Dimens.dp16))

            subsDetails.forEach {
                ListItemTwoCol(
                    lead = it.first,
                    tail = it.second
                )
            }
        }
    }
}

fun buildSubsDetails(
    context: Context,
    member: Membership
): List<Pair<String, String>> {

    val subsStatus = SubsStatus.newInstance(
        ctx = context,
        m = member.normalize(),
    )

    return listOf(
        Pair(
            context.getString(R.string.label_current_subs),
            subsStatus.productName
        )
    ) + subsStatus.details
}

@Preview(showBackground = true)
@Composable
fun PreviewLinkScreen() {
    LinkScreen(
        loading = false,
        params = WxEmailMerger(
            ftc = Account(
                id = "ftc-id",
                email = "preview@example.org",
                wechat = Wechat(),
                membership = Membership(
                    tier = Tier.STANDARD,
                    expireDate = LocalDate.now().plusMonths(1),
                    payMethod = PayMethod.ALIPAY
                )
            ),
            wx = Account(
                id = "",
                unionId = "wechat-id",
                email = "",
                wechat = Wechat(
                    nickname = "Wechat user"
                ),
                membership = Membership(
                    tier = Tier.PREMIUM,
                    expireDate = LocalDate.now().plusDays(1),
                    payMethod = PayMethod.WXPAY
                )
            ),
            loginMethod = LoginMethod.EMAIL
        ),
        onLink = {}
    )
}
