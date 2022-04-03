package com.ft.ftchinese.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.model.legal.legalPages
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OTheme
import com.ft.ftchinese.ui.webpage.WebpageActivity

class AboutListActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OTheme {
                Scaffold(
                    topBar = {
                        Toolbar(
                            heading = stringResource(id = R.string.title_about_us),
                            onBack = { finish() }
                        )
                    },
                    scaffoldState = rememberScaffoldState()
                ) {
                    AboutListScreen()
                }
            }
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, AboutListActivity::class.java))
        }
    }
}

@Composable
fun AboutListScreen() {
    val context = LocalContext.current

    Column {
        legalPages.forEach { pageMeta ->
            Row(
                modifier = Modifier
                    .clickable {
                        WebpageActivity.start(context, pageMeta)
                    }
                    .fillMaxWidth()
                    .padding(Dimens.dp8),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = pageMeta.title)
                Icon(
                    painter = painterResource(id = R.drawable.ic_keyboard_arrow_right_gray_24dp),
                    contentDescription = "Open"
                )
            }

            Divider(startIndent = Dimens.dp8)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAboutListScreen() {
    AboutListScreen()
}
