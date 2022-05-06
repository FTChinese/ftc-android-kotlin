package com.ft.ftchinese.ui.wxinfo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.theme.OTheme
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.coroutines.launch

class WxInfoActivity : ComponentActivity() {
    private lateinit var wxApi: IWXAPI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        wxApi = WXAPIFactory.createWXAPI(this, BuildConfig.WX_SUBS_APPID)
        wxApi.registerApp(BuildConfig.WX_SUBS_APPID)

        setContent {
            OTheme {
                val scaffoldState = rememberScaffoldState()
                val scope = rememberCoroutineScope()

                Scaffold(
                    topBar = {
                        Toolbar(
                            heading = stringResource(id = R.string.title_wx_account),
                            onBack = {
                                // TODO: setResult(OK)
                                finish()
                            }
                        )
                    },
                    scaffoldState = scaffoldState
                ) { innerPadding ->
                    WxInfoActivityScreen(
                        wxApi = wxApi,
                        showSnackBar = {
                            scope.launch {
                                scaffoldState.snackbarHostState.showSnackbar(it)
                            }
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, WxInfoActivity::class.java))
        }
    }
}
