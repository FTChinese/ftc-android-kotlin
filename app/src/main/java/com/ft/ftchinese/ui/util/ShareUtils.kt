package com.ft.ftchinese.ui.util

import android.content.ContentValues
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.database.ReadArticle
import com.ft.ftchinese.model.request.WxMiniParams
import com.ft.ftchinese.ui.article.screenshot.ScreenshotMeta
import com.ft.ftchinese.ui.article.share.ShareApp
import com.tencent.mm.opensdk.modelbiz.WXLaunchMiniProgram
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX
import com.tencent.mm.opensdk.modelmsg.WXImageObject
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.io.InputStream

object ShareUtils {

    private const val keyWxMiniId = "wxminiprogramid"
    private const val keyWxMiniPath = "wxminiprogrampath"

    fun createWxApi(context: Context): IWXAPI {
        return WXAPIFactory.createWXAPI(context, BuildConfig.WX_SUBS_APPID, false)
    }

    /**
     * Create the content values used to insert a image row
     * using MediaStore.
     */
    fun screenshotDetails(teaser: ReadArticle): ContentValues {
        return ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, screenshotName())
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.TITLE, teaser.title)
        }
    }

    private fun screenshotName(): String {
        return "FTC_Screenshot_" + LocalDateTime
            .now()
            .format(
                DateTimeFormatter.ofPattern("yyyyMMdd_kkmmss")
            )
    }

    fun wxShareArticleReq(
        res: Resources,
        app: ShareApp,
        article: ReadArticle,
    ): SendMessageToWX.Req {
        val webpageObj = WXWebpageObject().apply {
            webpageUrl = article.canonicalUrl
        }

        val mediaMsg = WXMediaMessage(webpageObj).apply {
            title = article.title
            description = article.standfirst
            thumbData = ImageUtil.bitmapToByteArray(
                BitmapFactory.decodeResource(res, R.drawable.ic_splash),
                Bitmap.CompressFormat.PNG
            )
        }

        return SendMessageToWX.Req().apply {
            transaction = System.currentTimeMillis().toString()
            message = mediaMsg
            scene = wxScene(app)
        }
    }

    fun wxShareScreenshotReq(
        stream: InputStream,
        appId: ShareApp,
        screenshot: ScreenshotMeta
    ): SendMessageToWX.Req {
        val bmp = BitmapFactory.decodeStream(stream)

        val imgObj = WXImageObject().apply {
            imagePath = screenshot.imageUri.toString()
        }

        val msg = WXMediaMessage().apply {
            title = screenshot.title
            description = screenshot.description
            mediaObject = imgObj
            thumbData = ImageUtil.bitmapToByteArray(
                Bitmap.createScaledBitmap(
                    bmp,
                    150,
                    150,
                    true,
                ),
                Bitmap.CompressFormat.JPEG
            )
        }

        bmp.recycle()

        return SendMessageToWX.Req().apply {
            transaction = System.currentTimeMillis().toString()
            message = msg
            scene = wxScene(appId)
        }
    }

    private fun wxScene(appId: ShareApp): Int {
        return when (appId) {
            ShareApp.WxFriend -> SendMessageToWX.Req.WXSceneSession
            ShareApp.WxMoments -> SendMessageToWX.Req.WXSceneTimeline
            else -> 0
        }
    }

    fun containWxMiniProgram(url: Uri): Boolean {
        return !url.getQueryParameter(keyWxMiniId).isNullOrBlank()
    }

    fun wxMiniProgramParams(url: Uri): WxMiniParams? {
        return url.getQueryParameter(keyWxMiniId)?.let {
            WxMiniParams(
                id = it,
                path = url.getQueryParameter(keyWxMiniPath) ?: ""
            )
        }
    }

    fun wxMiniProgramReq(params: WxMiniParams): WXLaunchMiniProgram.Req {
        return WXLaunchMiniProgram.Req().apply {
            userName = params.id
            path = params.path
            miniprogramType = if (BuildConfig.DEBUG) {
                WXLaunchMiniProgram.Req.MINIPROGRAM_TYPE_TEST
            } else {
                WXLaunchMiniProgram.Req.MINIPTOGRAM_TYPE_RELEASE
            }
        }
    }
}
