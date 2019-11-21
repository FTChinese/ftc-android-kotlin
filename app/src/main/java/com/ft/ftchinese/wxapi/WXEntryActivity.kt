package com.ft.ftchinese.wxapi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityWechatBinding
import com.ft.ftchinese.model.Result
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.model.reader.SessionManager
import com.ft.ftchinese.model.reader.WxOAuthIntent
import com.ft.ftchinese.ui.account.LinkPreviewActivity
import com.ft.ftchinese.ui.login.AccountResult
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

private const val EXTRA_UI_TEST = "extra_ui_test"

@kotlinx.coroutines.ExperimentalCoroutinesApi
class WXEntryActivity : ScopedAppActivity(), IWXAPIEventHandler, AnkoLogger {
    private lateinit var api: IWXAPI
    private lateinit var sessionManager: SessionManager
    private lateinit var loginViewModel: LoginViewModel
    private lateinit var binding: ActivityWechatBinding
//    private lateinit var accountViewModel: AccountViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_wechat)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_wechat)

        setSupportActionBar(toolbar)

        api = WXAPIFactory.createWXAPI(this, BuildConfig.WX_SUBS_APPID, false)

        sessionManager = SessionManager.getInstance(this)

        loginViewModel = ViewModelProvider(this)
                .get(LoginViewModel::class.java)
//        accountViewModel = ViewModelProvider(this)
//                .get(AccountViewModel::class.java)

        // Save wechat oauth session data.
//        loginViewModel.wxOAuthResult.observe(this, Observer {
//            onOAuthSession(it)
//        })

        loginViewModel.wxSessionResult.observe(this, Observer {
            when (it) {
                is Result.Success -> {
                    sessionManager.saveWxSession(it.data)
                    loginViewModel.loadWxAccount(it.data)
                }
                is Result.LocalizedError -> {
//                    showMessage(
//                            R.string.prompt_login_failed,
//                            it.msgId
//                    )
                    binding.inProgress = false
                    binding.result = UIWxOAuth(
                            heading = getString(R.string.prompt_login_failed),
                            body = getString(it.msgId),
                            done = true

                    )
//                    showProgress(false)
//                    enableButton(true)
                }
                is Result.Error -> {
//                    showMessage(
//                            R.string.prompt_login_failed,
//                            it.exception.message
//                    )
                    binding.inProgress = false
                    binding.result = UIWxOAuth(
                            heading = getString(R.string.prompt_login_failed),
                            body = it.exception.message,
                            done = true
                    )
//                    showProgress(false)
//                    enableButton(true)
                }
            }
        })

        // Handle wechat login or re-login after refresh token expired.
        loginViewModel.accountResult.observe(this, Observer {
            onAccountRetrieved(it)
        })

        doneButton.setOnClickListener {
            onClickDone()
        }

//        showMessage("")
//        enableButton(false)

        if (intent.getBooleanExtra(EXTRA_UI_TEST, false)) {

//            showProgress(true)
            binding.inProgress = true

//            showMessage(
//                    R.string.prompt_logged_in,
//                    getString(R.string.greeting_wx_login, "xxx"))

            binding.result = UIWxOAuth(
                    heading = getString(R.string.prompt_logged_in),
                    body = getString(R.string.greeting_wx_login, "xxx"),
                    done = true
            )

            binding.inProgress = false
//            showProgress(false)
//            enableButton(true)

            return
        }

        try {
            api.handleIntent(intent, this)
        } catch (e: Exception) {
            info(e)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        setIntent(intent)

        api.handleIntent(intent, this)
    }

    override fun onReq(req: BaseReq?) {}

    override fun onResp(resp: BaseResp?) {
        info("Wx login response type: ${resp?.type}, error code: ${resp?.errCode}")

        when (resp?.type) {
            // Wechat Login.
            ConstantsAPI.COMMAND_SENDAUTH -> {
                setTitle(R.string.title_wx_login)
                info("Start processing login...")
                processLogin(resp)
            }
            // This is used to handle your app sending message to wx and then return back to your app.
            // Share will return to here.
            ConstantsAPI.COMMAND_SENDMESSAGE_TO_WX -> {
                heading_tv.text = getString(R.string.share_done)
                finish()
            }
            else -> {
                finish()
            }
        }
    }

    private fun processLogin(resp: BaseResp) {

        showMessage(R.string.progress_logging)

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

                    val state = sessionManager.loadWxState()

                    if (state == null) {
                        showMessage(R.string.oauth_state_missing)
                        return
                    }

                    if (state != resp.state) {
                        showMessage(R.string.oauth_state_mismatch)
                        return
                    }

                    showProgress(true)

                    info("Use oauth code to exchange for ftc login session...")
                    loginViewModel.wxLogin(
                            code = resp.code)
                }
            }

            BaseResp.ErrCode.ERR_USER_CANCEL -> {
                showMessage(R.string.oauth_canceled, resp.errStr)
                showProgress(false)
                enableButton(true)
            }
            BaseResp.ErrCode.ERR_AUTH_DENIED -> {
                showMessage(R.string.oauth_denied, resp.errStr)
                showProgress(false)
                enableButton(true)
            }
            BaseResp.ErrCode.ERR_SENT_FAILED,
            BaseResp.ErrCode.ERR_UNSUPPORT,
            BaseResp.ErrCode.ERR_BAN -> {
                showMessage(R.string.prompt_login_failed, resp.errStr)
                showProgress(false)
                enableButton(true)
            }
        }
    }

    // This is just used for saving data.
    // If errors occurred here, onAccountRetrieved won't be
    // called.
//    private fun onOAuthSession(oauthResult: WxOAuthResult?) {
//        info("Received wx login session: $oauthResult")
//
//        if (oauthResult == null) {
//            return
//        }
//
//        if (oauthResult.error != null) {
//            showMessage(R.string.prompt_login_failed, oauthResult.error)
//            showProgress(false)
//            enableButton(true)
//            return
//        }
//
//        if (oauthResult.exception != null) {
//            showMessage(
//                    R.string.prompt_login_failed,
//                    oauthResult.exception.message
//            )
//            showProgress(false)
//            enableButton(true)
//            return
//        }
//
//        if (oauthResult.success == null) {
//            showMessage(R.string.prompt_login_failed, R.string.loading_failed)
//            showProgress(false)
//            enableButton(true)
//            return
//        }
//
//        sessionManager.saveWxSession(oauthResult.success)
//    }

    private fun onAccountRetrieved(accountResult: AccountResult?) {
//        showProgress(false)
//        enableButton(true)
        binding.inProgress = false
        val result = UIWxOAuth(
                heading = getString(R.string.prompt_login_failed),
                done = true
        )

        if (accountResult == null) {
            return
        }

        if (accountResult.error != null) {
//            showMessage(
//                    R.string.prompt_login_failed,
//                    accountResult.error
//            )
            result.body = getString(accountResult.error)
            binding.result = result

            return
        }

        if (accountResult.exception != null) {
//            showMessage(
//                    R.string.prompt_login_failed,
//                    accountResult.exception.message
//            )
            binding.result = UIWxOAuth(
                    heading = getString(R.string.prompt_login_failed),
                    body = accountResult.exception.message,
                    done = true
            )
            return
        }

        if (accountResult.success == null) {
//            showMessage(
//                    R.string.prompt_login_failed,
//                    R.string.loading_failed
//            )
            binding.result = UIWxOAuth(
                    heading = getString(R.string.prompt_login_failed),
                    body = getString(R.string.loading_failed),
                    done = true
            )
            return
        }

        when (sessionManager.loadWxIntent()) {
            WxOAuthIntent.LOGIN -> {
//                showMessage(
//                        R.string.prompt_logged_in,
//                        getString(
//                                R.string.greeting_wx_login,
//                                accountResult.success.wechat.nickname
//                        )
//                )
                binding.result = UIWxOAuth(
                        heading = getString(R.string.prompt_logged_in),
                        body = getString(
                                R.string.greeting_wx_login,
                                accountResult.success.wechat.nickname
                        )
                )
                sessionManager.saveAccount(accountResult.success)
            }
            WxOAuthIntent.LINK -> {
                LinkPreviewActivity.startForResult(this, accountResult.success)
                finish()
            }
        }
    }

    private fun showMessage(title: String, body: String? = null) {
        heading_tv.text = title
        message_tv.text = body ?: ""
    }

    private fun showMessage(title: Int, body: String? = null) {
        heading_tv.text = getString(title)
        message_tv.text = body ?: ""
    }

    private fun showMessage(title: Int?, body: Int? = null) {
        heading_tv.text = if (title != null) getString(title) else ""
        message_tv.text = if (body != null) getString(body) else ""
    }

    private fun onClickDone() {
        val account = sessionManager.loadAccount()

        if (account != null) {
            finish()
            return
        }

        LoginActivity.startForResult(this)
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        onClickDone()
    }

    private fun enableButton(enable: Boolean) {
        doneButton.isEnabled = enable
    }

    private fun showProgress(show: Boolean) {
        progress_bar.visibility = if (show) View.VISIBLE else View.GONE
    }

    companion object {
        @JvmStatic
        fun start(context: Context) {
            val intent = Intent(
                    context,
                    WXEntryActivity::class.java
            ).apply {
                putExtra(EXTRA_UI_TEST, true)
            }

            context.startActivity(intent)
        }
    }
}

