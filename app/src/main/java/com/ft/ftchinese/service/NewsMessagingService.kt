package com.ft.ftchinese.service

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

@kotlinx.coroutines.ExperimentalCoroutinesApi
class NewsMessagingService : FirebaseMessagingService(), AnkoLogger {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        info("From: ${remoteMessage.from}")

        remoteMessage.data.isNotEmpty().let {
            info("Message data payload: " + remoteMessage.data)
            handleNow()
        }

        remoteMessage.notification?.let {
            info("Title: ${it.title}, Body: ${it.body}")
        }
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        info("onNewToken: $token")

        sendRegistrationToServer(token)
    }

//    private fun scheduleJob() {
//
//    }

    private fun handleNow() {

    }

    private fun sendRegistrationToServer(token: String?) {
        info("Sending new token to server: $token")
    }
}
