package com.ft.ftchinese.ui.main.terms

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.PrimaryButton
import com.ft.ftchinese.ui.components.SecondaryButton
import com.ft.ftchinese.ui.theme.Dimens

@Composable
fun TermsScreen(
    onAgree: () -> Unit,
    onDecline: () -> Unit,
    content: @Composable() (BoxScope.() -> Unit)
) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            content()
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.dp16),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            SecondaryButton(
                onClick = onDecline,
                text = stringResource(id = R.string.btn_decline)
            )
            PrimaryButton(
                onClick = {
                    onAgree()
                },
                text = stringResource(id = R.string.btn_agree)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewTermsScreen() {
    TermsScreen(
        onAgree = {  },
        onDecline = {}
    ) {
        
    }
}
