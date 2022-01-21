package com.ft.ftchinese.model.conversion

data class ConversionTrackingRequest(
    val devToken: String, // The unique, static developer token issued to the API consumer.
    val linkId: String, // The link identifier binding the developer token of the API consumer to a specific app.
    val appEventType: AppEventType = AppEventType.FirstOpen,
    val rawDeviceId: String, // A valid UUID string representing the raw device ID.
    val idType: String = "advertisingid",
    val limitAdTracking: Boolean, // Whether user chosen to limit add tracking.
    val appVersion: String, // The current version of the app
    val osVersion: String,
    val sdkVersion: String, // The version of the SDK that measured the event
    val timestamp: Long, // The UNIX timestamp the conversion event occurred
) {

    fun toMap(): Map<String, String> {
        return mapOf(
            "dev_token" to devToken,
            "link_id" to linkId,
            "app_event_type" to appEventType.toString(),
            "rdid" to rawDeviceId,
            "id_type" to idType,
            "lat" to if (limitAdTracking) "1" else "0",
            "app_version" to appVersion, // 1.2.4
            "os_version" to osVersion, // The current version of the app’s host OS
            "sdk_version" to sdkVersion,
            "timestamp" to "$timestamp"
        )
    }
}
