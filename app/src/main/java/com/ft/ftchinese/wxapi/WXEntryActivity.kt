package com.ft.ftchinese.wxapi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.LoginMethod
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.auth.AuthActivity
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.theme.OTheme
import com.ft.ftchinese.ui.wxlink.LinkPreviewFragment
import com.ft.ftchinese.ui.wxlink.WxEmailLinkAccounts
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.openapi.WXAPIFactory

class WXEntryActivity : ScopedAppActivity(), IWXAPIEventHandler {
    private var api: IWXAPI? = null
    private lateinit var sessionManager: SessionManager
    private val wxRespLiveData = MutableLiveData<BaseResp?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        api = WXAPIFactory.createWXAPI(this, BuildConfig.WX_SUBS_APPID)
        sessionManager = SessionManager.getInstance(this)

        setContent {
            OTheme {

                WxEntryActivityScreen(
                    sessionStore = sessionManager,
                    wxRespLiveData = wxRespLiveData,
                    onFinish = this::onClickDone,
                    onLink = this::onLink
                )
            }
        }

        try {
            api?.handleIntent(intent, this)
        } catch (e: Exception) {
            e.message?.let { Log.i(TAG, it) }
        }
    }

    private fun onLink(wxAccount: Account) {
        sessionManager.loadAccount()?.let { current ->
            LinkPreviewFragment(
                WxEmailLinkAccounts(
                    ftc = current,
                    wx = wxAccount,
                    loginMethod = current.loginMethod ?: LoginMethod.EMAIL
                )
            ).show(
                supportFragmentManager,
                "PreviewEmailLinkWechat",
            )
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

        AuthActivity.startForResult(this)
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
fun WxEntryActivityScreen(
    sessionStore: SessionManager,
    wxRespLiveData: LiveData<BaseResp?>,
    onFinish: () -> Unit,
    onLink: (Account) -> Unit
) {

    val scaffoldState = rememberScaffoldState()
    val oauthState = rememberWxOAuthState(
        sessionStore = sessionStore
    )

    val wxRespState = wxRespLiveData.observeAsState()

    LaunchedEffect(key1 = wxRespState) {
        wxRespState.value?.let {
            oauthState.handleWxResp(it)
        }
    }

    when (val status = oauthState.authStatus.value) {
        is AuthStatus.LinkLoaded -> {
            onLink(status.account)
        }
        else -> {}
    }

    Scaffold(
        topBar = {
            Toolbar(
                heading = stringResource(id = R.string.title_wx_login),
                onBack = onFinish
            )
        },
        scaffoldState = scaffoldState
    ) { innerPadding ->
        ProgressLayout(
            loading = oauthState.authStatus.value is AuthStatus.Loading,
            modifier = Modifier.padding(innerPadding),
        ) {
            WxOAuthAScreen(
                status = oauthState.authStatus.value,
                onFinish = onFinish,
                onLink = onLink,
                onRetry = oauthState::retry
            )
        }
    }
}
