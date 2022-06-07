package com.ft.ftchinese.ui.subs.member

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.ClickableRow
import com.ft.ftchinese.ui.components.IconCancel
import com.ft.ftchinese.ui.components.IconRedo
import com.ft.ftchinese.ui.components.RightArrow
import com.ft.ftchinese.ui.theme.Dimens

enum class SubsOptionRow {
    GoToPaywall,
    CancelStripe,
    ReactivateStripe;
}

@Composable
fun SubsOptions(
    cancelStripe: Boolean,
    reactivateStripe: Boolean,
    onClickRow: (SubsOptionRow) -> Unit
) {

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {

            ClickableRow(
                onClick = {
                    onClickRow(SubsOptionRow.GoToPaywall)
                },
                endIcon = {
                    RightArrow()
                },
                modifier = Modifier.padding(Dimens.dp8)
            ) {
                Text(text = "购买订阅或更改自动续订")
            }


            if (reactivateStripe) {
                Divider(startIndent = Dimens.dp8)

                ClickableRow(
                    onClick = {
                        onClickRow(SubsOptionRow.ReactivateStripe)
                    },
                    modifier = Modifier.padding(Dimens.dp8),
                    endIcon = {
                        IconRedo()
                    }
                ) {
                    Text(text = stringResource(id = R.string.stripe_reactivate_auto_renew))
                }
            }

            if (cancelStripe) {
                Divider(startIndent = Dimens.dp8)
                ClickableRow(
                    onClick = {
                        onClickRow(SubsOptionRow.CancelStripe)
                    },
                    modifier = Modifier.padding(Dimens.dp8),
                    endIcon = {
                        IconCancel()
                    }
                ) {
                    Text(text = stringResource(id = R.string.stripe_cancel))
                }

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
