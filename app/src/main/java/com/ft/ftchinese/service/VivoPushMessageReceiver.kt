package com.ft.ftchinese.service

import android.content.Context
import android.util.Log
import com.ft.ftchinese.repository.PushClient
import com.vivo.push.model.UPSNotificationMessage
import com.vivo.push.model.UnvarnishedMessage
import com.vivo.push.sdk.OpenClientPushMessageReceiver

private const val TAG = "VivoPushReceiver"
private const val LOG_PREFIX = "[FTCPush]"

class VivoPushMessageReceiver : OpenClientPushMessageReceiver() {
    override fun onReceiveRegId(context: Context?, regId: String?) {
        val pushId = regId?.trim().orEmpty()
        Log.i(TAG, "$LOG_PREFIX vivo_receiver_reg_id token=${PushClient.summarizeSecret(pushId)}")
        PushClient.handleNewVivoPushId(pushId)
    }

    override fun onNotificationMessageClicked(context: Context?, message: UPSNotificationMessage?) {
        Log.i(
            TAG,
            "$LOG_PREFIX vivo_notification_clicked title=${message?.title.orEmpty()} content=${message?.content.orEmpty()} params=${message?.params ?: emptyMap<String, String>()}"
        )
        VivoPushRouter.handleNotificationClick(context, message)
    }

    override fun onForegroundMessageArrived(message: UPSNotificationMessage?) {
        VivoPushRouter.logForegroundMessage(message)
    }

    override fun onTransmissionMessage(context: Context?, message: UnvarnishedMessage?) {
        VivoPushRouter.logTransmissionMessage(message)
    }
}
