package com.ft.ftchinese.ui.wxinfo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.theme.OTheme
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.coroutines.launch

class WxInfoActivity : ComponentActivity() {
    private lateinit var wxApi: IWXAPI
    private lateinit var wxInfoViewModel: WxInfoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        wxApi = WXAPIFactory.createWXAPI(this, BuildConfig.WX_SUBS_APPID)
        wxApi.registerApp(BuildConfig.WX_SUBS_APPID)
        wxInfoViewModel = ViewModelProvider(this)[WxInfoViewModel::class.java]

        setContent {
            OTheme {
                ScreenLayout(
                    wxApi = wxApi,
                    wxInfoViewModel = wxInfoViewModel,
                    onBack = {
                        setResult(Activity.RESULT_OK)
                        finish()
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        wxInfoViewModel.reloadAccount()
    }

    companion object {

        @JvmStatic
        fun newIntent(context: Context) = Intent(context, WxInfoActivity::class.java)
    }
}

@Composable
private fun ScreenLayout(
    wxApi: IWXAPI,
    wxInfoViewModel: WxInfoViewModel,
    onBack: () -> Unit,
) {
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Toolbar(
                heading = stringResource(id = R.string.title_wx_account),
                onBack = onBack
            )
        },
        scaffoldState = scaffoldState
    ) { innerPadding ->
        WxInfoActivityScreen(
            wxApi = wxApi,
            wxInfoViewModel = wxInfoViewModel,
            modifier = Modifier.padding(innerPadding),
            showSnackBar = {
                scope.launch {
                    scaffoldState.snackbarHostState.showSnackbar(it)
                }
            },
            onUpdated = {

            }
        )
    }
}
