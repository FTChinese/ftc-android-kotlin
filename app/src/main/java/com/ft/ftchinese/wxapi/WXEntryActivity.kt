package com.ft.ftchinese.wxapi

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedAppActivity
import com.ft.ftchinese.model.*
import com.ft.ftchinese.util.ClientError
import com.ft.ftchinese.base.handleException
import com.ft.ftchinese.ui.account.LinkActivity
import com.ft.ftchinese.ui.login.LoginActivity
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
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast
import kotlin.Exception

@kotlinx.coroutines.ExperimentalCoroutinesApi
class WXEntryActivity : ScopedAppActivity(), IWXAPIEventHandler, AnkoLogger {
    private lateinit var api: IWXAPI
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wechat)

        setSupportActionBar(toolbar)

        hideUI()

        api = WXAPIFactory.createWXAPI(this, BuildConfig.WX_SUBS_APPID, false)

        try {
            api.handleIntent(intent, this)
        } catch (e: Exception) {
            info(e)
        }

        info("onCreate called")
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        setIntent(intent)
        api.handleIntent(intent, this)
    }

    override fun onReq(req: BaseReq?) {

    }

    private fun hideUI() {
        heading_tv.visibility = View.GONE
        done_button.visibility = View.GONE
    }

    private fun showLoginFailed() {
        heading_tv.visibility = View.VISIBLE
        heading_tv.text = getString(R.string.prompt_login_failed)
        done_button.visibility = View.VISIBLE

        done_button.setOnClickListener {

            LoginActivity.startForResult(this)

            finish()
        }
    }

    private fun showLoggingIn() {
        heading_tv.visibility = View.VISIBLE
        heading_tv.text = getString(R.string.progress_logging)
    }

    private fun showLoginSuccess(msg: String) {
        heading_tv.visibility = View.VISIBLE
        heading_tv.text = getString(R.string.prompt_logged_in)

        message_tv.text = msg

        done_button.visibility = View.VISIBLE
        done_button.setOnClickListener {
            finish()
        }
    }


    override fun onResp(resp: BaseResp?) {
        info("Wx login response type: ${resp?.type}, error code: ${resp?.errCode}")

        when (resp?.type) {
            // Wechat Login.
            ConstantsAPI.COMMAND_SENDAUTH -> {

                setTitle(R.string.title_wx_login)

                info("Wx auth")
                processLogin(resp)
            }
            // This is used to handle your app sending message to wx and then return back to your app.
            // Share will return to here.
            ConstantsAPI.COMMAND_SENDMESSAGE_TO_WX -> {
                heading_tv.text = getString(R.string.wxshare_done)
                info("Send message to wx")
                finish()
            }
            else -> {
                finish()
            }
        }

    }

    private fun processLogin(resp: BaseResp) {

        showLoggingIn()

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

                        showLoginFailed()

                        toast("State code not found. Suspicious login attempt")
                        return
                    }

                    if (state != resp.state) {

                        showLoginFailed()

                        toast(R.string.oauth_state_mismatch)
                        return
                    }


                    login(resp.code)
                }
            }

            BaseResp.ErrCode.ERR_USER_CANCEL -> {

                info(R.string.oauth_canceled)
            }
            BaseResp.ErrCode.ERR_AUTH_DENIED -> {
                info(R.string.oauth_denied)
            }
        }
    }


    private fun login(code: String) {
        showProgress(true)

        launch {
            try {
                // Get session data from API.
                val sess = withContext(Dispatchers.IO) {

                    WxOAuth.login(code)
                }

                if (sess == null) {
                    showProgress(false)

                    showLoginFailed()

                    return@launch
                }

                // Save session data.
                sessionManager.saveWxSession(sess)

                loadAccount(sess)

            } catch (e: ClientError) {
                info("API error: $e")

                showProgress(false)
                showLoginFailed()

                toast(e.message)
            } catch (e: Exception) {

                showProgress(false)
                showLoginFailed()

                handleException(e)
            }
        }
    }

    /**
     * Load account after fetched wechat session.
     */
    private suspend fun loadAccount(sess: WxSession) {
        val account = withContext(Dispatchers.IO) {
            sess.fetchAccount()
        }

        if (account == null) {
            showProgress(false)

            showLoginFailed()

            return
        }

        showProgress(false)

        info("Wx login account: $account")

        val wxIntent = sessionManager.loadWxIntent() ?: WxOAuthIntent.LOGIN

        info("Wechat OAuth intent: $wxIntent")

        /**
         * If wehcat oauth is used for binding accounts,
         * never save the fetched account!
         */
        if (wxIntent == WxOAuthIntent.BINDING) {
            info("Launch binding")
            LinkActivity.startForResult(this, account)

            finish()
            return
        }

        sessionManager.saveAccount(account)

        showLoginSuccess(getString(R.string.greeting_wx_login, account.wechat.nickname))
    }

    private fun showProgress(show: Boolean) {
        if (show) {
            progress_bar.visibility = View.VISIBLE
        } else {
            progress_bar.visibility = View.GONE
        }
    }
}

