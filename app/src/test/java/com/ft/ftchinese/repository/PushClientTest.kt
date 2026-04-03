package com.ft.ftchinese.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PushClientTest {

    @Test
    fun doesNotRegisterWhenUserAndTokenAreUnchanged() {
        val needsRegistration = PushClient.needsFcmRegistration(
            currentUserId = "user-1",
            currentToken = "token-1",
            lastUserId = "user-1",
            lastToken = "token-1",
        )

        assertFalse(needsRegistration)
    }

    @Test
    fun registersWhenUserChangesEvenIfTokenStaysTheSame() {
        val needsRegistration = PushClient.needsFcmRegistration(
            currentUserId = "user-2",
            currentToken = "token-1",
            lastUserId = "user-1",
            lastToken = "token-1",
        )

        assertTrue(needsRegistration)
    }

    @Test
    fun buildsFcmRegistrationPayloadForTheNewEndpoint() {
        val request = PushClient.buildFcmRegistration(
            pushId = "fcm-token",
            installationId = "device-id",
            appVersion = "6.8.46",
            locale = "zh-CN",
            brand = "vivo",
            model = "X200",
            osVersion = "15",
            notificationsEnabled = true,
            gmsAvailable = false,
        )

        assertEquals("android", request.platform)
        assertEquals("fcm", request.provider)
        assertEquals("fcm-token", request.pushId)
        assertEquals("device-id", request.installationId)
        assertEquals("granted", request.notificationPermission)
        assertFalse(request.gmsAvailable)
    }
}
