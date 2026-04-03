package com.ft.ftchinese.model.request

import kotlinx.serialization.Serializable

@Serializable
data class NativePushRegistration(
    val platform: String,
    val provider: String,
    val pushId: String,
    val installationId: String,
    val brand: String,
    val model: String,
    val osVersion: String,
    val appVersion: String,
    val locale: String,
    val notificationPermission: String,
    val gmsAvailable: Boolean,
)
