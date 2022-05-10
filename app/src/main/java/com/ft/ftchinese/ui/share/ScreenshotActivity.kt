package com.ft.ftchinese.ui.share

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.CloseBar
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OTheme

class ScreenshotActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OTheme {

            }
        }
    }
}

@Composable
private fun Screen(
    onShareTo: (SocialApp) -> Unit,
    onExit: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        CloseBar(
            onClose = onExit
        )

        Column(
            modifier = Modifier.weight(1.0f)
        ) {

        }

        Divider()

        Spacer(modifier = Modifier.height(Dimens.dp16))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            arrayOf(
                SocialApp(
                    name = "好友",
                    icon = R.drawable.wechat,
                    id = SocialAppId.WECHAT_FRIEND
                ),
                SocialApp(
                    name = "朋友圈",
                    icon = R.drawable.moments,
                    id = SocialAppId.WECHAT_MOMENTS
                ),
            ).forEach {
                ShareIcon(
                    image = painterResource(id = it.icon),
                    text = it.name as String
                ) {
                    onShareTo(it)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(Dimens.dp16))
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewScreenshot() {
    Screen(onShareTo = {}) {
        
    }
}
