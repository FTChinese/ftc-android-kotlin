package com.ft.ftchinese.wxapi

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.models.*
import com.ft.ftchinese.util.ClientError
import com.ft.ftchinese.util.handleException
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.android.synthetic.main.activity_wx_entry.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast
import kotlin.Exception

class WXEntryActivity : AppCompatActivity(), IWXAPIEventHandler, AnkoLogger {
    private var api: IWXAPI? = null
    private var wxManager: WxManager? = null
    private var sessionManager: SessionManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wx_entry)

        setSupportActionBar(toolbar)

        api = WXAPIFactory.createWXAPI(this, BuildConfig.WX_SUBS_APPID, false)

        try {
            api?.handleIntent(intent, this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        info("onCreate called")
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        setIntent(intent)
        api?.handleIntent(intent, this)
    }

    override fun onReq(req: BaseReq?) {

    }

    override fun onResp(resp: BaseResp?) {
        info("Wx login response type: ${resp?.type}, error code: ${resp?.errCode}")

        when (resp?.type) {
            // Wechat Login.
            ConstantsAPI.COMMAND_SENDAUTH -> {
                heading_tv.text = getString(R.string.progress_logging)
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
        when (resp.errCode) {
            BaseResp.ErrCode.ERR_OK -> {
                info("User authorized")

                // 第一步：请求CODE
                // 用户点击授权后，微信客户端会被拉起，
                // 跳转至授权界面，用户在该界面点击允许或取消，
                // SDK通过SendAuth的Resp返回数据给调用方
                if (resp is SendAuth.Resp) {
                    info("code: ${resp.code}, state: ${resp.state}, lang: ${resp.lang}, country: ${resp.country}")

                    wxManager = WxManager.getInstance(this)
                    sessionManager = SessionManager.getInstance(this)

                    val state = wxManager?.loadState()

                    info("State: $state")
                    if (state == null) {
                        toast("State code not found. Suspicious login attempt")
                        return
                    }

                    if (state != resp.state) {
                        toast("State code not match")
                        return
                    }


                    login(resp.code)
                }
            }

            BaseResp.ErrCode.ERR_USER_CANCEL -> {
                info("OAuth Login Canceled")
            }
            BaseResp.ErrCode.ERR_AUTH_DENIED -> {
                info("OAuth Login Denied")
            }
        }
    }

    private fun login(code: String) {
        showProgress(true)

        GlobalScope.launch(Dispatchers.Main) {
            try {
                val sess = withContext(Dispatchers.IO) {
                    WxLogin(code).send()
                }
                if (sess == null) {
                    showProgress(false)

                    toast("Login failed")

                    return@launch
                }
                wxManager?.saveSession(sess)

                val acnt = withContext(Dispatchers.IO) {
                    sess.getAccount()
                }

                if (acnt == null) {
                    showProgress(false)
                    toast("Failed to fetch account data")
                    return@launch
                }

                showProgress(false)

                sessionManager?.saveAccount(acnt)

                info("Killing activity")
                finish()
            } catch (e: ClientError) {
                info("API error: $e")

                showProgress(false)

                handleClientError(e)
            } catch (e: Exception) {
                handleException(e)
            }
        }
//
    }

    private fun handleClientError(err: ClientError) {
        toast(err.message)
    }

    private fun showProgress(v: Boolean) {
        if (v) {
            progress_bar.visibility = View.VISIBLE
        } else {
            progress_bar.visibility = View.GONE
        }
    }
}

