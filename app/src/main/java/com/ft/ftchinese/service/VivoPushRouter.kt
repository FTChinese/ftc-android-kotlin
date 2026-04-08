package com.ft.ftchinese.service

import android.content.Context
import android.util.Log
import com.vivo.push.model.UPSNotificationMessage
import com.vivo.push.model.UnvarnishedMessage

private const val TAG = "VivoPushRouter"
private const val LOG_PREFIX = "[FTCPush]"

object VivoPushRouter {
    fun handleNotificationClick(context: Context?, message: UPSNotificationMessage?) {
        if (context == null || message == null) {
            Log.i(TAG, "$LOG_PREFIX vivo_notification_click_skip reason=missing_context_or_message")
            return
        }

        val params = message.params ?: emptyMap()
        val route = PushNotificationRouter.routeFromData(
            data = params,
            fallbackTitle = message.title,
        ) ?: run {
            Log.i(
                TAG,
                "$LOG_PREFIX vivo_notification_click_skip reason=unsupported_payload params=$params"
            )
            return
        }

        PushNotificationRouter.start(context, route, "vivo_click")
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
