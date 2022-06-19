package com.ft.ftchinese.wxapi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.auth.AuthActivity
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.theme.OTheme
import com.ft.ftchinese.ui.wxlink.merge.MergeActivityScreen
import com.ft.ftchinese.wxapi.oauth.WxOAuthActivityScreen
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.openapi.WXAPIFactory

class WXEntryActivity : AppCompatActivity(), IWXAPIEventHandler {
    private var api: IWXAPI? = null
    private lateinit var sessionManager: SessionManager
    private val wxRespLiveData = MutableLiveData<BaseResp?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        api = WXAPIFactory.createWXAPI(this, BuildConfig.WX_SUBS_APPID)
        sessionManager = SessionManager.getInstance(this)

        setContent {
            OAuthApp(
                wxRespLiveData = wxRespLiveData,
                onExit = this::onClickDone
            )
        }

        try {
            api?.handleIntent(intent, this)
        } catch (e: Exception) {
            e.message?.let { Log.i(TAG, it) }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        setIntent(intent)

        api?.handleIntent(intent, this)
    }

    override fun onReq(req: BaseReq?) {}

    override fun onResp(resp: BaseResp?) {
        Log.i(TAG, "Wx login response type: ${resp?.type}, error code: ${resp?.errCode}")

        when (resp?.type) {
            // Wechat Login.
            ConstantsAPI.COMMAND_SENDAUTH -> {
                Log.i(TAG, "Start processing login...")
                wxRespLiveData.value = resp
            }
            // This is used to handle your app sending message to wx and then return back to your app.
            // Share will return to here.
            ConstantsAPI.COMMAND_SENDMESSAGE_TO_WX -> {
                finish()
            }
            else -> {
                finish()
            }
        }
    }

    private fun onClickDone() {
        val account = sessionManager.loadAccount()

        if (account != null) {
            finish()
            return
        }

        startActivity(AuthActivity.newIntent(this))
        finish()
    }

    companion object {
        private const val TAG = "WxEntryActivity"

        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(
                context,
                WXEntryActivity::class.java
            ))
        }
    }
}

@Composable
fun OAuthApp(
    wxRespLiveData: LiveData<BaseResp?>,
    onExit: () -> Unit
) {
    val scaffold = rememberScaffoldState()

    OTheme {
        val navController = rememberNavController()
        val backstackEntry = navController.currentBackStackEntryAsState()

        val currentScreen = OAuthAppScreen.fromRoute(
            backstackEntry.value?.destination?.route
        )
        
        Scaffold(
            topBar = {
                Toolbar(
                    heading = stringResource(id = currentScreen.titleId),
                    onBack = {
                        val ok = navController.popBackStack()
                        if (!ok) {
                            onExit()
                        }
                    }
                )
            },
            scaffoldState = scaffold
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = OAuthAppScreen.OAuth.name,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(
                    route = OAuthAppScreen.OAuth.name
                ) {
                    WxOAuthActivityScreen(
                        wxRespLiveData = wxRespLiveData,
                        onFinish = onExit
                    ) {
                        navigateToLink(
                            navController
                        )
                    }
                }

                composable(
                    route = OAuthAppScreen.EmailLink.name
                ) {
                    MergeActivityScreen(
                        scaffoldState = scaffold,
                        onSuccess = onExit
                    )
                }
            }
        }
    }
}

private fun navigateToLink(
    navController: NavController
) {
    navController.navigate(OAuthAppScreen.EmailLink.name)
}
