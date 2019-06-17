package com.ft.ftchinese.wxapi

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedAppActivity
import com.ft.ftchinese.model.*
import com.ft.ftchinese.ui.account.AccountViewModel
import com.ft.ftchinese.ui.account.LinkPreviewActivity
import com.ft.ftchinese.ui.login.LoginActivity
import com.ft.ftchinese.ui.login.LoginViewModel
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.android.synthetic.main.activity_wechat.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import kotlin.Exception

@kotlinx.coroutines.ExperimentalCoroutinesApi
class WXEntryActivity : ScopedAppActivity(), IWXAPIEventHandler, AnkoLogger {
    private lateinit var api: IWXAPI
    private lateinit var sessionManager: SessionManager
    private lateinit var loginViewModel: LoginViewModel
    private lateinit var accountViewModel: AccountViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wechat)

        setSupportActionBar(toolbar)

        hideUI()

        api = WXAPIFactory.createWXAPI(this, BuildConfig.WX_SUBS_APPID, false)
        sessionManager = SessionManager.getInstance(this)

        try {
            api.handleIntent(intent, this)
        } catch (e: Exception) {
            info(e)
        }

        loginViewModel = ViewModelProviders.of(this)
                .get(LoginViewModel::class.java)
        accountViewModel = ViewModelProviders.of(this)
                .get(AccountViewModel::class.java)

        // Handle re-authorize when refreshing wechat info page.
        loginViewModel.wxOAuthResult.observe(this, Observer {
            val oauthResult = it ?: return@Observer

            if (oauthResult.error != null) {
                showFailure(getString(oauthResult.error))
                return@Observer
            }

            if (oauthResult.exception != null) {
                showFailure(oauthResult.exception.message)
                return@Observer
            }

            if (oauthResult.success == null) {
                showFailure(getString(R.string.prompt_load_failure))
                return@Observer
            }


            if (sessionManager.loadWxIntent() == WxOAuthIntent.REFRESH) {
                val account = sessionManager.loadAccount()
                if (account == null) {
                    showSuccess("请稍后在账号与安全中下拉刷新")
                    return@Observer
                }
                accountViewModel.refresh(account)
            }
        })

        // Handle wechat login or re-login after refresh token expired.
        accountViewModel.accountResult.observe(this, Observer {
            val accountResult = it ?: return@Observer

            if (accountResult.error != null) {
                showFailure(getString(accountResult.error), true)
                return@Observer
            }

            if (accountResult.exception != null) {
                showFailure(accountResult.exception.message, true)
                return@Observer
            }

            if (accountResult.success == null) {
                showFailure(getString(R.string.prompt_load_failure), true)
                return@Observer
            }

            when (sessionManager.loadWxIntent()) {
                WxOAuthIntent.LOGIN -> {
                    showSuccess(getString(R.string.greeting_wx_login, accountResult.success.wechat.nickname))

                    sessionManager.saveAccount(accountResult.success)
                }
                WxOAuthIntent.LINK -> {
                    LinkPreviewActivity.startForResult(this, accountResult.success)
                    finish()
                }
            }
        })

        info("onCreate called")
    }

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        setIntent(intent)
        api.handleIntent(intent, this)
    }

    override fun onReq(req: BaseReq?) {

    }

    override fun onResp(resp: BaseResp?) {
        info("Wx login response type: ${resp?.type}, error code: ${resp?.errCode}")

        when (resp?.type) {
            // Wechat Login.
            ConstantsAPI.COMMAND_SENDAUTH -> {
                setTitle(R.string.title_wx_login)
                processLogin(resp)
            }
            // This is used to handle your app sending message to wx and then return back to your app.
            // Share will return to here.
            ConstantsAPI.COMMAND_SENDMESSAGE_TO_WX -> {
                heading_tv.text = getString(R.string.wxshare_done)
                finish()
            }
            else -> {
                finish()
            }
        }

    }

    private fun processLogin(resp: BaseResp) {

        showLoggingIn()

        info("Resp code: ${resp.errCode}, resp msg: ${resp.errStr}")

        when (resp.errCode) {
            BaseResp.ErrCode.ERR_OK -> {
                info("User authorized")

                // 第一步：请求CODE
                // 用户点击授权后，微信客户端会被拉起，
                // 跳转至授权界面，用户在该界面点击允许或取消，
                // SDK通过SendAuth的Resp返回数据给调用方
                if (resp is SendAuth.Resp) {
                    info("code: ${resp.code}, state: ${resp.state}, lang: ${resp.lang}, country: ${resp.country}")
                    // Cannot initialize in onCreate method due to WXEntryActivity weird design.
                    sessionManager = SessionManager.getInstance(this)

                    val state = sessionManager.loadWxState()

                    info("State: $state")
                    if (state == null) {
                        showFailure(
                                "授权码缺失",
                                sessionManager.loadWxIntent() != WxOAuthIntent.REFRESH)
                        return
                    }

                    if (state != resp.state) {
                        showFailure(
                                getString(R.string.oauth_state_mismatch),
                                sessionManager.loadWxIntent()!= WxOAuthIntent.REFRESH)
                        return
                    }

                    val oauthIntent = sessionManager.loadWxIntent()
                    showProgress(true)
                    loginViewModel.wxLogin(
                            code = resp.code,
                            isManualRefresh = oauthIntent == WxOAuthIntent.REFRESH)
                }
            }

            BaseResp.ErrCode.ERR_USER_CANCEL -> {
                showFailure(
                        getString(R.string.oauth_canceled),
                        sessionManager.loadWxIntent() != WxOAuthIntent.REFRESH)
            }

            BaseResp.ErrCode.ERR_SENT_FAILED -> {
                showFailure(
                        null,
                        sessionManager.loadWxIntent() != WxOAuthIntent.REFRESH)
            }
            BaseResp.ErrCode.ERR_AUTH_DENIED -> {
                showFailure(
                        getString(R.string.oauth_denied),
                        sessionManager.loadWxIntent() != WxOAuthIntent.REFRESH)
            }
            BaseResp.ErrCode.ERR_UNSUPPORT -> {
                showFailure(
                        null,
                        sessionManager.loadWxIntent() != WxOAuthIntent.REFRESH)
            }
            BaseResp.ErrCode.ERR_BAN -> {
                showFailure(
                        null,
                        sessionManager.loadWxIntent() != WxOAuthIntent.REFRESH)
            }
        }
    }

    private fun hideUI() {
        heading_tv.visibility = View.GONE
        done_button.visibility = View.GONE
    }

    private fun showLoggingIn() {
        showResult(UIWxOAuth(
                heading = R.string.progress_logging
        ))
    }

    private fun showSuccess(msg: String) {
        showResult(UIWxOAuth(
                heading = R.string.prompt_logged_in,
                body = msg,
                done = true
        ))
    }

    private fun showFailure(msg: String?, reLogin: Boolean = false) {
        showResult(UIWxOAuth(
                heading = R.string.prompt_login_failed,
                body = msg,
                done = true,
                restartLogin = reLogin
        ))
    }

    private fun showResult(ui: UIWxOAuth) {
        heading_tv.visibility = View.VISIBLE
        done_button.visibility = if (ui.done) View.VISIBLE else View.GONE
        showProgress(false)

        heading_tv.text = getString(ui.heading)
        message_tv.text = ui.body

        done_button.setOnClickListener {
            if (ui.restartLogin) {
                LoginActivity.startForResult(this)
            }
            finish()
        }
    }

    private fun showProgress(show: Boolean) {
        if (show) {
            progress_bar.visibility = View.VISIBLE
        } else {
            progress_bar.visibility = View.GONE
        }
    }
}

