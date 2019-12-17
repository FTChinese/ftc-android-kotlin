package com.ft.ftchinese.wxapi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityWechatBinding
import com.ft.ftchinese.model.Result
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.model.reader.SessionManager
import com.ft.ftchinese.model.reader.WxOAuthIntent
import com.ft.ftchinese.ui.account.LinkPreviewActivity
import com.ft.ftchinese.ui.login.LoginActivity
import com.ft.ftchinese.viewmodel.LoginViewModel
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.openapi.WXAPIFactory
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

        // First get session data from api,
        // and then use session data to retrieve account.
        loginViewModel.wxSessionResult.observe(this, Observer {
            when (it) {
                is Result.Success -> {

                    sessionManager.saveWxSession(it.data)

                    // Start to retrieve account data
                    loginViewModel.loadWxAccount(it.data)
                }
                is Result.LocalizedError -> {

                    binding.inProgress = false
                    binding.result = UIWx(
                            heading = getString(R.string.prompt_login_failed),
                            body = getString(it.msgId),
                            enableButton = true

                    )
                }
                is Result.Error -> {

                    binding.inProgress = false
                    binding.result = UIWx(
                            heading = getString(R.string.prompt_login_failed),
                            body = it.exception.message ?: "",
                            enableButton = true
                    )
                }
            }
        })

        // Handle wechat login or re-login after refresh token expired.
        loginViewModel.accountResult.observe(this, Observer {
            onAccountRetrieved(it)
        })

        binding.doneButton.setOnClickListener {
            onClickDone()
        }


        if (intent.getBooleanExtra(EXTRA_UI_TEST, false)) {

            binding.result = UIWx(
                    heading = getString(R.string.prompt_logged_in),
                    body = getString(R.string.greeting_wx_login, "xxx"),
                    enableButton = true
            )

            binding.inProgress = false

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
                binding.result = UIWx(
                        heading = getString(R.string.share_done),
                        body = "",
                        enableButton = true
                )
                finish()
            }
            else -> {
                finish()
            }
        }
    }

    private fun validateResponse(resp: SendAuth.Resp): Boolean {

        val state = sessionManager.loadWxState()

        if (state == null) {
            binding.result = UIWx(
                    heading = getString(R.string.prompt_login_failed),
                    body = getString(R.string.oauth_state_missing),
                    enableButton = true
            )
            binding.inProgress = false

            return false
        }

        if (state != resp.state) {
            binding.result = UIWx(
                    heading = getString(R.string.prompt_login_failed),
                    body = getString(R.string.oauth_state_mismatch),
                    enableButton = true
            )
            binding.inProgress = false
            return false
        }

        return true
    }

    private fun handleFailureResponse(resp: BaseResp): UIWx {
        return when (resp.errCode) {
            BaseResp.ErrCode.ERR_USER_CANCEL -> {

                UIWx(
                        heading = getString(R.string.prompt_login_failed),
                        body = getString(R.string.oauth_canceled),
                        enableButton = true
                )
            }
            BaseResp.ErrCode.ERR_AUTH_DENIED -> {

                UIWx(
                        heading = getString(R.string.prompt_login_failed),
                        body = getString(R.string.oauth_denied),
                        enableButton = true
                )
            }
            BaseResp.ErrCode.ERR_SENT_FAILED,
            BaseResp.ErrCode.ERR_UNSUPPORT,
            BaseResp.ErrCode.ERR_BAN -> {

                UIWx(
                        heading = getString(R.string.prompt_login_failed),
                        body = resp.errStr,
                        enableButton = true
                )
            }
            else -> UIWx(
                    heading = getString(R.string.prompt_login_failed),
                    body = "Unknown errors occurred",
                    enableButton = true
            )
        }
    }

    private fun processLogin(resp: BaseResp) {
        binding.result = UIWx(
                heading = getString(R.string.progress_logging)
        )

        binding.inProgress = true

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

                    if (!validateResponse(resp)) {
                        return
                    }

                    info("Use oauth code to exchange for ftc login session...")
                    loginViewModel.wxLogin(
                            code = resp.code)
                }
            }

            else -> {
                binding.result = handleFailureResponse(resp)
                binding.inProgress = false
            }

        }
    }

    private fun onAccountRetrieved(accountResult: Result<Account>) {

        binding.inProgress = false

        when (accountResult) {
            is Result.LocalizedError -> {
                binding.result = UIWx(
                        heading = getString(R.string.prompt_login_failed),
                        body = getString(accountResult.msgId),
                        enableButton = true
                )
            }
            is Result.Error -> {
                binding.result = UIWx(
                        heading = getString(R.string.prompt_login_failed),
                        body = accountResult.exception.message ?: "",
                        enableButton = true
                )
            }
            is Result.Success -> {
                when (sessionManager.loadWxIntent()) {
                    // For login
                    WxOAuthIntent.LOGIN -> {
                        binding.result = UIWx(
                                heading = getString(R.string.prompt_logged_in),
                                body = getString(
                                        R.string.greeting_wx_login,
                                        accountResult.data.wechat.nickname
                                ),
                                enableButton = true
                        )
                        sessionManager.saveAccount(accountResult.data)
                    }

                    // For account linking
                    WxOAuthIntent.LINK -> {
                        LinkPreviewActivity.startForResult(this, accountResult.data)
                        finish()
                    }
                }
            }
        }
//        if (accountResult == null) {
//            binding.result = UIWx(
//                    heading = getString(R.string.prompt_login_failed),
//                    body = getString(R.string.loading_failed),
//                    enableButton = true
//            )
//            return
//        }

//        if (accountResult.error != null) {
//            binding.result = UIWx(
//                    heading = getString(R.string.prompt_login_failed),
//                    body = getString(accountResult.error),
//                    enableButton = true
//            )
//
//            return
//        }

//        if (accountResult.exception != null) {
//
//            binding.result = UIWx(
//                    heading = getString(R.string.prompt_login_failed),
//                    body = accountResult.exception.message ?: "",
//                    enableButton = true
//            )
//            return
//        }

//        if (accountResult.success == null) {
//
//            binding.result = UIWx(
//                    heading = getString(R.string.prompt_login_failed),
//                    body = getString(R.string.loading_failed),
//                    enableButton = true
//            )
//            return
//        }

//        when (sessionManager.loadWxIntent()) {
//            // For login
//            WxOAuthIntent.LOGIN -> {
//                binding.result = UIWx(
//                        heading = getString(R.string.prompt_logged_in),
//                        body = getString(
//                                R.string.greeting_wx_login,
//                                accountResult.success.wechat.nickname
//                        ),
//                        enableButton = true
//                )
//                sessionManager.saveAccount(accountResult.success)
//            }
//
//            // For account linking
//            WxOAuthIntent.LINK -> {
//                LinkPreviewActivity.startForResult(this, accountResult.success)
//                finish()
//            }
//        }
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

