package com.ft.ftchinese.ui.member

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.ClickableRow

enum class OptionRow {
    GoToPaywall,
    CancelStripe,
    ReactivateStripe;
}

@Composable
fun SubsOptions(
    cancelStripe: Boolean,
    reactivateStripe: Boolean,
    onClickRow: (OptionRow) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        ClickableRow(
            onClick = {
                onClickRow(OptionRow.GoToPaywall)
            }
        ) {
            Text(
                text = "购买订阅或更改自动续订"
            )
        }

        Divider()

        if (reactivateStripe) {
            ClickableRow(
                onClick = {
                    onClickRow(OptionRow.ReactivateStripe)
                }
            ) {
                Text(
                    text = stringResource(id = R.string.stripe_reactivate_auto_renew)
                )
            }

            Divider()
        }

        if (cancelStripe) {
            ClickableRow(onClick = {
                onClickRow(OptionRow.CancelStripe)
            }) {
                Text(
                    text = stringResource(id = R.string.stripe_cancel)
                )
            }

            Divider()
        }
    }
}
