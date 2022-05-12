package com.ft.ftchinese.ui.wxlink

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.ft.ftchinese.R
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.ui.components.ListItemTwoCol
import com.ft.ftchinese.ui.components.PrimaryButton
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.components.WeightedColumn
import com.ft.ftchinese.ui.member.SubsStatus
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun LinkScreen(
    loading: Boolean,
    linkable: Boolean,
    onLink: () -> Unit,
) {
    ProgressLayout(
        loading = loading
    ) {
        WeightedColumn(
            bottom = {
                PrimaryButton(
                    onClick = onLink,
                    enabled = !loading && linkable
                ) {
                    Text(
                        text = stringResource(id = R.string.btn_start)
                    )
                }
            }
        ) {
            Text(
                text = stringResource(id = R.string.link_heading),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.h6
            )

            Row {
                Image(
                    painter = painterResource(id = R.drawable.ic_link_teal_24dp),
                    contentDescription = ""
                )
            }
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
        m = member,
    )

    return listOf(
        Pair(
            context.getString(R.string.label_current_subs),
            subsStatus.productName
        )
    ) + subsStatus.details
}
