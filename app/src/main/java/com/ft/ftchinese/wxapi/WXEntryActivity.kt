package com.ft.ftchinese.wxapi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityWechatBinding
import com.ft.ftchinese.model.reader.WxOAuth
import com.ft.ftchinese.model.reader.WxOAuthIntent
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.data.FetchResult
import com.ft.ftchinese.ui.login.AuthActivity
import com.ft.ftchinese.ui.wxlink.LinkPreviewFragment
import com.ft.ftchinese.ui.wxlink.LinkViewModel
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

private const val EXTRA_UI_TEST = "extra_ui_test"

@kotlinx.coroutines.ExperimentalCoroutinesApi
class WXEntryActivity : ScopedAppActivity(), IWXAPIEventHandler, AnkoLogger {
    private lateinit var api: IWXAPI
    private lateinit var sessionManager: SessionManager
    private lateinit var binding: ActivityWechatBinding

    private lateinit var oauthViewModel: WxOAuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_wechat,
        )

        setSupportActionBar(binding.toolbar.toolbar)

        api = WXAPIFactory.createWXAPI(this, BuildConfig.WX_SUBS_APPID, false)

        sessionManager = SessionManager.getInstance(this)

        oauthViewModel = ViewModelProvider(this)
            .get(WxOAuthViewModel::class.java)

        setupViewModel()

        if (intent.getBooleanExtra(EXTRA_UI_TEST, false)) {
            oauthViewModel.progressLiveData.value = false
            binding.title = getString(R.string.prompt_logged_in)
            binding.details = getString(R.string.greeting_wx_login, "xxx")
            return
        }

        try {
            api.handleIntent(intent, this)
        } catch (e: Exception) {
            info(e)
        }
    }

    private fun setupViewModel() {
        oauthViewModel.progressLiveData.observe(this) {
            binding.inProgress = it
        }

        // First get session data from api,
        // and then use session data to retrieve account.
        oauthViewModel.wxSessionResult.observe(this) { result ->
            when (result) {
                is FetchResult.Success -> {
                    sessionManager.saveWxSession(result.data)
                }
                is FetchResult.LocalizedError -> {
                    binding.title = getString(R.string.prompt_login_failed)
                    binding.details = getString(result.msgId)
                }
                is FetchResult.Error -> {
                    binding.title = getString(R.string.prompt_login_failed)
                    binding.details = result.exception.message ?: ""
                }
            }
        }

        // Handle wechat
        // * login;
        // * re-login after refresh token expired;
        // * login for link.
        oauthViewModel.accountResult.observe(this) { result ->
            when (result) {
                is FetchResult.LocalizedError -> {
                    binding.title  = getString(R.string.prompt_login_failed)
                    binding.details = getString(result.msgId)
                }
                is FetchResult.Error -> {
                    binding.title = getString(R.string.prompt_login_failed)
                    binding.details = result.exception.message ?: ""
                }
                is FetchResult.Success -> {
                    when (WxOAuth.getLastIntent()) {
                        // For login
                        WxOAuthIntent.LOGIN -> {
                            binding.title = getString(R.string.prompt_logged_in)
                            binding.details = getString(
                                R.string.greeting_wx_login,
                                result.data.wechat.nickname
                            )
                            // For login, persist account data.
                            sessionManager.saveAccount(result.data)
                        }

                        // For account linking, show preview ui.
                        WxOAuthIntent.LINK -> {
                            LinkPreviewFragment()
                                .show(supportFragmentManager, "EmailLinkWxPreview")
                        }
                    }
                }
            }
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
                binding.title = getString(R.string.share_done)
                binding.details = ""
                finish()
            }
            else -> {
                finish()
            }
        }
    }

    private fun processLogin(resp: BaseResp) {
        // When start processing login, make sure the progress
        // indicator is on.
        oauthViewModel.progressLiveData.value = true

        binding.title = getString(R.string.progress_logging)
        binding.details = getString(R.string.wait_while_wx_login)

        info("Resp code: ${resp.errCode}, resp msg: ${resp.errStr}")

        when (resp.errCode) {
            BaseResp.ErrCode.ERR_OK -> {
                info("User authorized")

                if (resp is SendAuth.Resp) {
                    // 第一步：请求CODE
                    // 用户点击授权后，微信客户端会被拉起，
                    // 跳转至授权界面，用户在该界面点击允许或取消，
                    // SDK通过SendAuth的Resp返回数据给调用方
                    info("code: ${resp.code}, state: ${resp.state}, lang: ${resp.lang}, country: ${resp.country}")

                    if (!WxOAuth.codeMatched(resp.state)) {
                        binding.title = getString(R.string.prompt_login_failed)
                        binding.details = getString(R.string.oauth_state_mismatch)
                        // Stop progress.
                        oauthViewModel.progressLiveData.value = false
                        return
                    }

                    // The login process is handled here.
                    info("Use oauth code to exchange for ftc login session...")
                    oauthViewModel.getSession(code = resp.code)
                    return
                }

                binding.title = getString(R.string.prompt_login_failed)
                binding.details = resp.errStr
                // Stop progress indicator.
                oauthViewModel.progressLiveData.value = false
            }
            BaseResp.ErrCode.ERR_USER_CANCEL -> {

                binding.title = getString(R.string.prompt_login_failed)
                binding.details = getString(R.string.oauth_canceled)
                // Stop progress indicator.
                oauthViewModel.progressLiveData.value = false
            }
            BaseResp.ErrCode.ERR_AUTH_DENIED -> {
                binding.title = getString(R.string.prompt_login_failed)
                binding.details = getString(R.string.oauth_denied)
                // Stop progress indicator.
                oauthViewModel.progressLiveData.value = false
            }
            BaseResp.ErrCode.ERR_SENT_FAILED,
            BaseResp.ErrCode.ERR_UNSUPPORT,
            BaseResp.ErrCode.ERR_BAN -> {
                binding.title = getString(R.string.prompt_login_failed)
                binding.details = resp.errStr
                // Stop progress indicator.
                oauthViewModel.progressLiveData.value = false
            }
            else -> {
                binding.title = getString(R.string.prompt_login_failed)
                binding.details = "Unknown errors occurred"
                // Stop progress indicator.
                oauthViewModel.progressLiveData.value = false
            }
        }
    }

    fun onClickDoneButton(view: View) {
        onClickDone()
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

