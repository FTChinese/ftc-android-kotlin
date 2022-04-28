package com.ft.ftchinese.ui.member

import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.RightArrow
import com.ft.ftchinese.ui.theme.Dimens

enum class SubsOptionRow {
    GoToPaywall,
    CancelStripe,
    ReactivateStripe;
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun OptionRow(
    text: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Dimens.dp16,
                    vertical = Dimens.dp8
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.body1
            )

            RightArrow()
        }
    }
}

@Composable
fun SubsOptions(
    cancelStripe: Boolean,
    reactivateStripe: Boolean,
    onClickRow: (SubsOptionRow) -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        OptionRow(
            text = "购买订阅或更改自动续订"
        ) {
            onClickRow(SubsOptionRow.GoToPaywall)
        }

        Spacer(modifier = Modifier.height(1.dp))

        if (reactivateStripe) {
            OptionRow(
                text = stringResource(id = R.string.stripe_reactivate_auto_renew)
            ) {
                onClickRow(SubsOptionRow.ReactivateStripe)
            }

        }

        if (cancelStripe) {
            OptionRow(
                text = stringResource(id = R.string.stripe_cancel)
            ) {
                onClickRow(SubsOptionRow.CancelStripe)
            }

        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSubsOptions() {
    SubsOptions(
        cancelStripe = true,
        reactivateStripe = false,
        onClickRow = {}
    )
}
