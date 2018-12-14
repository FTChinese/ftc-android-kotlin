package com.ft.ftchinese.wxapi

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.models.WxManager
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.gson
import com.google.gson.annotations.SerializedName
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.android.synthetic.main.simple_toolbar.*
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast
import kotlin.Exception

val wxSnsUri = Uri.parse("https://api.weixin.qq.com/sns")

class WXEntryActivity : AppCompatActivity(), IWXAPIEventHandler, AnkoLogger {
    private var api: IWXAPI? = null
    private val tokenUri = Uri.parse("https://api.weixin.qq.com/sns/oauth2/access_token")

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
            ConstantsAPI.COMMAND_SENDAUTH -> {
                info("Wx auth")
                processLogin(resp)
            }
            // This is used to handle your app sending message to wx and then return back to your app.
            ConstantsAPI.COMMAND_SENDMESSAGE_TO_WX -> {
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

                    val wxManager = WxManager.getInstance(this)
                    val state = wxManager.loadState()

                    if (state == null) {
                        toast("State code not found. Suspicious login attempt")
                        return
                    }

                    if (state != resp.state) {
                        toast("State code not matchi")
                        return
                    }

                    toast("Prepare to get access token")
//                    val codeResp = WxCodeResp(resp.code, resp.state)

//                    login(codeResp)
                }
            }

            BaseResp.ErrCode.ERR_USER_CANCEL -> {
                info("User canceled")
            }
            BaseResp.ErrCode.ERR_AUTH_DENIED -> {
                info("User denied")
            }
        }
    }

    private fun login(resp: WxCodeResp) {
        val url = wxSnsUri.buildUpon()
                .appendPath("oauth2")
                .appendPath("access_token")
                .appendQueryParameter("appid", BuildConfig.WX_SUBS_APPID)
                .appendQueryParameter("secret", "")
                .appendQueryParameter("code", resp.code)
                .appendQueryParameter("grant_type", "authorization_code")
                .build()
                .toString()

        GlobalScope.launch(Dispatchers.Main) {
            try {
                // 第二步：通过code获取access_token
                val wxAccess = async {
                    resp.getTokenAsync(url)
                }.await()

                info("Get wx access: $wxAccess")

                // 获取用户个人信息

                val userInfo = async {
                    wxAccess?.getUserInfo()
                }.await()
                
                info("Userinfo $userInfo")

            } catch (e: WxAccessException) {

                toast("Wechat login failed. Error code: ${e.errCode}, error message: ${e.errMsg}")

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }
}

data class WxCodeResp(
        val code: String,
        val state: String
) {
    /**
     * @return WxAccessResp if request success, or null if failed.
     * @throws WxAccessException
     */
    fun getTokenAsync(url: String): WxAccessResp? {
        val respStr = Fetch().get(url).string()

        return try {
            gson.fromJson<WxAccessResp>(respStr, WxAccessResp::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            return try {
                val ex =gson.fromJson<WxAccessException>(respStr, WxAccessException::class.java)

                throw ex
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}

data class WxAccessResp(
        @SerializedName("access_token") val accessToken: String,
        @SerializedName("expires_in") val expiresIn: Int,
        @SerializedName("refresh_token") val refreshToken: String,
        @SerializedName("openid") val openId: String,
        val scope: String,
        @SerializedName("unionid") val unionId: String

) {
//    fun refresh(): Deferred<WxAccessResp?> = async {
//
//    }

    fun getUserInfo(): WxUserInfo? {
        val url = wxSnsUri.buildUpon()
                .appendPath("userinfo")
                .appendQueryParameter("access_token", accessToken)
                .appendQueryParameter("openid", openId)
                .build()
                .toString()
        val respStr = Fetch().get(url).string()

        return try {
            gson.fromJson<WxUserInfo>(respStr, WxUserInfo::class.java)
        } catch (e: Exception) {
            return try {
                val ex = gson.fromJson<WxAccessException>(respStr, WxAccessException::class.java)
                throw ex
            } catch (e: Exception) {
                e.printStackTrace()

                null
            }
        }
    }
}

data class WxUserInfo(
        @SerializedName("openid") val openId: String,
        @SerializedName("nickname") val nickName: String,
        @SerializedName("sex") val gender: Int,
        val province: String,
        val city: String,
        val country: String,
        @SerializedName("headimgurl") val avatar: String,
        val privilege: Array<String>,
        @SerializedName("unionid") val unionId: String
)

data class WxAccessException(
    @SerializedName("errcode") val errCode: Int,
    @SerializedName("errmsg") val errMsg: Int
) : Exception()