package com.ft.ftchinese.service

import android.content.Context
import android.content.Intent
import android.util.Log
import com.ft.ftchinese.ui.main.SplashActivity
import com.vivo.push.model.UPSNotificationMessage
import com.vivo.push.model.UnvarnishedMessage

private const val TAG = "VivoPushRouter"
private const val LOG_PREFIX = "[FTCPush]"
private const val EXTRA_MESSAGE_TYPE = "content_type"
private const val EXTRA_CONTENT_ID = "content_id"

object VivoPushRouter {
    fun handleNotificationClick(context: Context?, message: UPSNotificationMessage?) {
        if (context == null || message == null) {
            Log.i(TAG, "$LOG_PREFIX vivo_notification_click_skip reason=missing_context_or_message")
            return
        }

        val params = message.params ?: emptyMap()
        val contentType = params[EXTRA_MESSAGE_TYPE]?.trim().orEmpty()
        val contentId = params[EXTRA_CONTENT_ID]?.trim().orEmpty()

        if (contentType.isBlank() || contentId.isBlank()) {
            Log.i(
                TAG,
                "$LOG_PREFIX vivo_notification_click_skip reason=missing_payload contentType=$contentType contentId=$contentId"
            )
            return
        }

        val intent = Intent(context, SplashActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_MESSAGE_TYPE, contentType)
            putExtra(EXTRA_CONTENT_ID, contentId)
        }

        Log.i(
            TAG,
            "$LOG_PREFIX vivo_notification_click_route contentType=$contentType contentId=$contentId"
        )
        context.startActivity(intent)
    }

    fun logForegroundMessage(message: UPSNotificationMessage?) {
        val params = message?.params ?: emptyMap()
        Log.i(
            TAG,
            "$LOG_PREFIX vivo_foreground_message title=${message?.title.orEmpty()} content=${message?.content.orEmpty()} params=$params"
        )
    }

    fun logTransmissionMessage(message: UnvarnishedMessage?) {
        val params = message?.params ?: emptyMap()
        Log.i(
            TAG,
            "$LOG_PREFIX vivo_transmission_message targetType=${message?.targetType ?: -1} params=$params"
        )
    }
}
