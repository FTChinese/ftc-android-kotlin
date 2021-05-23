package com.ft.ftchinese.ui.share

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.MediaStore
import com.ft.ftchinese.database.ReadArticle
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.R
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX
import com.tencent.mm.opensdk.modelmsg.WXImageObject
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ShareUtils {
    /**
     * Create the content values used to insert a image row
     * using MediaStore.
     */
    fun screenshotDetails(teaser: ReadArticle): ContentValues {
        return ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, generateName())
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.TITLE, teaser.title)
        }
    }

    private fun generateName(): String {
        return "FTC_Screenshot_" + LocalDateTime
            .now()
            .format(
                DateTimeFormatter.ofPattern("yyyyMMdd_kkmmss")
            )
    }

    fun bmpToByteArray(bmp: Bitmap, format: Bitmap.CompressFormat): ByteArray {
        return ByteArrayOutputStream().use {  stream ->
            bmp.compress(format, 100, stream)
            bmp.recycle()
            stream.toByteArray()
        }
    }

    fun wxShareArticleReq(
        res: Resources,
        appId: SocialAppId,
        article: ReadArticle,
    ): SendMessageToWX.Req {
        val webpageObj = WXWebpageObject().apply {
            webpageUrl = article.canonicalUrl
        }

        val mediaMsg = WXMediaMessage(webpageObj).apply {
            title = article.title
            description = article.standfirst
            thumbData = bmpToByteArray(
                BitmapFactory.decodeResource(res, R.drawable.ic_splash),
                Bitmap.CompressFormat.PNG
            )
        }

        return SendMessageToWX.Req().apply {
            transaction = System.currentTimeMillis().toString()
            message = mediaMsg
            scene = wxScene(appId)
        }
    }

    fun wxShareScreenshotReq(
        stream: InputStream,
        appId: SocialAppId,
        screenshot: ArticleScreenshot
    ): SendMessageToWX.Req {
        val bmp = BitmapFactory.decodeStream(stream)

        val imgObj = WXImageObject().apply {
            imagePath = screenshot.imageUri.toString()
        }

        val msg = WXMediaMessage().apply {
            title = screenshot.content.title
            description = screenshot.content.standfirst
            mediaObject = imgObj
            thumbData = bmpToByteArray(
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

    private fun wxScene(appId: SocialAppId): Int {
        return when (appId) {
            SocialAppId.WECHAT_FRIEND -> SendMessageToWX.Req.WXSceneSession
            SocialAppId.WECHAT_MOMENTS -> SendMessageToWX.Req.WXSceneTimeline
            else -> 0
        }
    }
}
