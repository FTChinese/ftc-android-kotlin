package com.ft.ftchinese.wxapi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.core.view.WindowCompat
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
import com.ft.ftchinese.wxapi.shared.WxRespProgress
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
    private val wxDiagnosticLiveData = MutableLiveData<String?>()
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WxLoginDiagnostic.recordEntry(this, TAG, "onCreate", intent)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = true

        api = WXAPIFactory.createWXAPI(this, BuildConfig.WX_SUBS_APPID)
        sessionManager = SessionManager.getInstance(this)

        setContent {
            OAuthApp(
                wxRespLiveData = wxRespLiveData,
                wxDiagnosticLiveData = wxDiagnosticLiveData,
                onExit = this::onClickDone
            )
        }

        try {
            api?.handleIntent(intent, this)
            scheduleDiagnosticCheck()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle wechat login intent in onCreate", e)
            wxDiagnosticLiveData.value = e.message ?: "Failed to handle wechat login intent"
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        WxLoginDiagnostic.recordEntry(this, TAG, "onNewIntent", intent)

        setIntent(intent)

        try {
            api?.handleIntent(intent, this)
            scheduleDiagnosticCheck()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle wechat login intent in onNewIntent", e)
            wxDiagnosticLiveData.value = e.message ?: "Failed to handle wechat login intent"
        }
    }

    override fun onReq(req: BaseReq?) {}

    override fun onResp(resp: BaseResp?) {
        WxLoginDiagnostic.recordResp(this, TAG, resp)
        if (resp == null) {
            Log.e(TAG, "Received null response from wechat sdk")
            finish()
            return
        }

        Log.i(
            TAG,
            "Wx login response type: ${resp.type}, error code: ${resp.errCode}, error message: ${resp.errStr}"
        )

        when (resp.type) {
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
            ConstantsAPI.COMMAND_PAY_BY_WX -> {
                Log.w(TAG, "Received wx pay callback in auth entry; forwarding to pay entry")
                WXPayEntryActivity.startFromWxCallback(this, intent)
                finish()
            }
            else -> {
                finish()
            }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
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

    private fun scheduleDiagnosticCheck() {
        handler.postDelayed({
            if (wxRespLiveData.value == null) {
                wxDiagnosticLiveData.value = WxLoginDiagnostic.consumePendingIssue(this)
            }
        }, DIAGNOSTIC_DELAY_MS)
    }

    companion object {
        private const val TAG = "WxEntryActivity"
        private const val DIAGNOSTIC_DELAY_MS = 1500L

        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(
                context,
                WXEntryActivity::class.java
            ))
        }

        @JvmStatic
        fun startFromWxCallback(context: Context, wxIntent: Intent?) {
            context.startActivity(Intent(
                context,
                WXEntryActivity::class.java
            ).apply {
                wxIntent?.extras?.let { putExtras(it) }
                action = wxIntent?.action
                data = wxIntent?.data
            })
        }
    }
}

@Composable
fun OAuthApp(
    wxRespLiveData: LiveData<BaseResp?>,
    wxDiagnosticLiveData: LiveData<String?>,
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
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
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
                    val diagnostic = wxDiagnosticLiveData.observeAsState()

                    if (diagnostic.value != null) {
                        WxRespProgress(
                            title = "微信登录诊断",
                            subTitle = diagnostic.value.orEmpty(),
                            onClickButton = onExit
                        )
                    } else {
                        WxOAuthActivityScreen(
                            wxRespLiveData = wxRespLiveData,
                            onFinish = onExit,
                            onLink = {
                                navigateToLink(
                                    navController
                                )
                            }
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
