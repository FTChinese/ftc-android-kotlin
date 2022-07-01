package com.ft.ftchinese.ui.article.content

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ft.ftchinese.R
import com.ft.ftchinese.model.content.Language
import com.ft.ftchinese.ui.components.IconArrowBackIOS
import com.ft.ftchinese.ui.theme.OColor

@Composable
fun ArticleToolBar(
    isBilingual: Boolean,
    currentLang: Language,
    onSelectLang: (Language) -> Unit,
    audioFound: Boolean,
    onClickAudio: () -> Unit,
    onBack: () -> Unit,
) {
    TopAppBar {
        IconButton(
            onClick = onBack
        ) {
            IconArrowBackIOS()
        }

        Box(
            modifier = Modifier.weight(1f),
        ) {
            if (isBilingual) {
                LanguageSwitch(
                    modifier = Modifier.align(Alignment.Center),
                    currentLang = currentLang,
                    onSelect = onSelectLang
                )
            }
        }

        Box(
            modifier = Modifier.widthIn(24.dp)
        ) {
            if (audioFound) {
                IconButton(
                    onClick = onClickAudio
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_volume_up_black_24dp),
                        contentDescription = "Audio"
                    )
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewArticleBar() {
    ArticleToolBar(
        isBilingual = true,
        currentLang = Language.CHINESE,
        onSelectLang = {},
        audioFound = true,
        onClickAudio = { /*TODO*/ }
    ) {

    }
}
