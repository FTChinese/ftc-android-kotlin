package com.ft.ftchinese.ui.account.unlinkwx

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.enums.UnlinkAnchor
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.reader.Wechat
import com.ft.ftchinese.ui.components.*
import com.ft.ftchinese.ui.member.SubsStatus
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor
import org.threeten.bp.LocalDate

@Composable
fun UnlinkScreen(
    account: Account,
    loading: Boolean,
    onUnlink: (UnlinkAnchor) -> Unit
) {

    val context = LocalContext.current
    val isFtcSideOnly = account.membership.unlinkToEmailOnly

    val (selectOption, setSelectOption) = remember {
        mutableStateOf(if (isFtcSideOnly) {
            UnlinkAnchor.FTC
        } else {
            null
        })
    }

    WeightedColumn(
        bottom = {
            PrimaryButton(
                onClick = {
                    selectOption?.let(onUnlink)
                },
                enabled = !loading && (selectOption != null),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.title_unlink)
                )
            }
        }
    ) {
        Text(
            text = stringResource(id = R.string.unlink_guide),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h6
        )

        Spacer(modifier = Modifier.height(Dimens.dp16))

        UnlinkDetails(
            details = buildUnlinkDetails(
                context = context,
                account = account
            )
        )

        Spacer(modifier = Modifier.height(Dimens.dp16))

        UnlinkOptions(
            ftcSideOnly = isFtcSideOnly,
            selected = selectOption,
            onSelect = setSelectOption
        )
    }
}

@Composable
fun UnlinkDetails(
    details: List<Pair<String, String>>
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.dp16)
        ) {
            details.forEach {
                ListItemTwoCol(
                    lead = it.first,
                    tail = it.second
                )
            }
        }
    }
}

fun buildUnlinkDetails(
    context: Context,
    account: Account,
): List<Pair<String, String>> {
    val subsStatus = SubsStatus.newInstance(
        context,
        account.membership.normalize()
    )

    return listOf(
        Pair(
            context.getString(R.string.label_ftc_account),
            account.email,
        ),
        Pair(
            context.getString(R.string.label_wx_account),
            account.wechat.nickname ?: ""
        ),
        Pair(
            context.getString(R.string.label_current_subs),
            subsStatus.productName,
        )
    ) + subsStatus.details
}

@Composable
fun UnlinkOptions(
    ftcSideOnly: Boolean,
    selected: UnlinkAnchor?,
    onSelect: (UnlinkAnchor) -> Unit
) {
    val context = LocalContext.current
    val radioOptions = listOf(UnlinkAnchor.FTC, UnlinkAnchor.WECHAT)

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(id = R.string.unlink_anchor),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.subtitle1
        )

        Spacer(modifier = Modifier.height(Dimens.dp16))

        radioOptions.forEach { anchor ->
            val enabled = !(anchor == UnlinkAnchor.WECHAT && ftcSideOnly)
            ClickableRow(
                onClick = { onSelect(anchor) },
                endIcon = {
                    IconCheck(
                        checked = (selected == anchor),
                        tint = if (enabled) {
                            OColor.teal
                        } else {
                            OColor.black.copy(alpha = 0.4f)
                        }
                    )
                },
                enabled = enabled,
                modifier = Modifier.padding(Dimens.dp8)
            ) {
                Text(
                    text = getAnchorName(
                        context = context,
                        anchor = anchor,
                    ),
                    color = if (enabled) {
                        OColor.black
                    } else {
                        OColor.black.copy(alpha = 0.4f)
                    },
                    style = MaterialTheme.typography.body1
                )
            }

            Divider()
        }

        Spacer(modifier = Modifier.height(Dimens.dp16))

        Text(
            text = stringResource(id = R.string.unlink_footnote),
            modifier = Modifier.fillMaxWidth(),
            color = OColor.black80,
            style = MaterialTheme.typography.body2
        )
    }
}

private fun getAnchorName(
    context: Context,
    anchor: UnlinkAnchor,
): String {
    return when (anchor) {
        UnlinkAnchor.FTC -> context.getString(R.string.label_ftc_account)
        UnlinkAnchor.WECHAT -> context.getString(R.string.label_wechat)
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewUnlinkScreen() {
    UnlinkScreen(
        account = Account(
            id = "ftc-id",
            unionId = "wechat-id",
            email = "hell@example.org",
            wechat = Wechat(
                nickname = "Wechat name"
            ),
            membership = Membership(
                tier = Tier.STANDARD,
                cycle = Cycle.YEAR,
                expireDate = LocalDate.now().plusDays(7),
                payMethod = PayMethod.STRIPE,
                autoRenew = true
            )
        ),
        loading = false,
    ) {

    }
}
