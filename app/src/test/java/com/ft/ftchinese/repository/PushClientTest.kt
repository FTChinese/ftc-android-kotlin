package com.ft.ftchinese.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PushClientTest {

    @Test
    fun doesNotRegisterWhenUserProviderAndPushIdAreUnchanged() {
        val needsRegistration = PushClient.needsRegistration(
            currentUserId = "user-1",
            currentProvider = "fcm",
            currentPushId = "token-1",
            lastUserId = "user-1",
            lastProvider = "fcm",
            lastPushId = "token-1",
        )

        assertFalse(needsRegistration)
    }

    @Test
    fun registersWhenProviderChangesEvenIfPushIdStaysTheSame() {
        val needsRegistration = PushClient.needsRegistration(
            currentUserId = "user-1",
            currentProvider = "vivo",
            currentPushId = "token-1",
            lastUserId = "user-1",
            lastProvider = "fcm",
            lastPushId = "token-1",
        )

        assertTrue(needsRegistration)
    }

    @Test
    fun registersWhenUserChangesEvenIfPushIdStaysTheSame() {
        val needsRegistration = PushClient.needsRegistration(
            currentUserId = "user-2",
            currentProvider = "fcm",
            currentPushId = "token-1",
            lastUserId = "user-1",
            lastProvider = "fcm",
            lastPushId = "token-1",
        )

        assertTrue(needsRegistration)
    }

    @Test
    fun prefersFcmWhenGooglePlayServicesAreAvailableEvenOnVivoDevices() {
        val preferred = PushClient.resolvePreferredProvider(
            gmsAvailable = true,
            brand = "vivo",
            manufacturer = "vivo",
            vivoConfigured = true,
        )

        assertEquals("fcm", preferred)
    }

    @Test
    fun prefersVivoWhenGooglePlayServicesAreUnavailableOnConfiguredVivoDevices() {
        val preferred = PushClient.resolvePreferredProvider(
            gmsAvailable = false,
            brand = "vivo",
            manufacturer = "vivo",
            vivoConfigured = true,
        )

        assertEquals("vivo", preferred)
    }

    @Test
    fun doesNotPreferVivoProviderWhenConfigIsMissing() {
        val preferred = PushClient.shouldUseVivoProvider(
            brand = "vivo",
            manufacturer = "vivo",
            vivoConfigured = false,
        )

        assertFalse(preferred)
    }

    @Test
    fun buildsVivoRegistrationPayloadForTheNewEndpoint() {
        val request = PushClient.buildRegistration(
            provider = "vivo",
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
        assertEquals("vivo", request.provider)
        assertEquals("fcm-token", request.pushId)
        assertEquals("device-id", request.installationId)
        assertEquals("granted", request.notificationPermission)
        assertFalse(request.gmsAvailable)
    }
}
