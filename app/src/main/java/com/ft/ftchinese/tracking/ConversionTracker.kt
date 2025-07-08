package com.ft.ftchinese.tracking

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.model.conversion.AdEvent
import com.ft.ftchinese.model.conversion.ConversionTrackingRequest
import com.ft.ftchinese.model.conversion.ConversionTrackingUA
import com.ft.ftchinese.repository.ConversionClient
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import org.threeten.bp.ZonedDateTime
import java.io.IOException
import java.util.*

class ConversionTracker(
    val context: Context,
    val timeout: Int,
) {

    private var shouldRetry = false

    // 可定制化的重试回退时间, 重试次数大于配置的重试时间个数时，默认为3秒
    private val backoffTimes = intArrayOf(1, 3, 10, 20, 60, 120, 300)
    // 累计回退次数
    private var backoffCount = 0
    // 回退时间
    private var backOffTime = 0L

    private val curTimestampInSeconds: Long
        get() = ZonedDateTime.now().toEpochSecond() - backOffTime

    /**
     * 计算回退时间
     *
     * @return
     */
    private fun calBackOffTime(): Int {
        var backTime = 3 // 默认3s

        // 重试次数在配置的重试时间个数以内
        if (backoffCount <= backoffTimes.size - 1) {
            backTime = backoffTimes[backoffCount]
        }
        backoffCount++
        return backTime
    }

    // See https://developer.android.com/training/articles/ad-id
    // https://developers.google.com/android/reference/com/google/android/gms/ads/identifier/AdvertisingIdClient
    // Starting from late 2021, on Android 12 devices, when isLimitAdTrackingEnabled() is true, the returned value of this API will be 00000000-0000-0000-0000-000000000000 regardless of the app’s target SDK level.
    // See https://developers.google.com/android/reference/com/google/android/gms/ads/identifier/AdvertisingIdClient.Info
    private fun fetchAdInfo(): AdvertisingIdClient.Info? {
        return try {
            // This method cannot be called in the main thread as it may block leading to ANRs.
            // An IllegalStateException will be thrown if this is called on the main thread.
            AdvertisingIdClient.getAdvertisingIdInfo(context)
        } catch (e: GooglePlayServicesNotAvailableException) {
            e.message?.let { Log.i(TAG, it) }
            null
        } catch (e: GooglePlayServicesRepairableException) {
            e.message?.let { Log.i(TAG, it) }
            null
        } catch (e: IllegalStateException) {
            e.message?.let { Log.i(TAG, it) }
            null
        } catch (e: IOException) {
            e.message?.let { Log.i(TAG, it) }
            null
        }
    }



    private fun getAppVersion(): String {
        return context
            .packageManager
            .getPackageInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            ).versionName ?: ""
    }



    private fun requestUA(): ConversionTrackingUA {
        return ConversionTrackingUA(
            name = BuildConfig.APPLICATION_ID,
            version = "18.0.1",
            osAndVersion = "Android " + Build.VERSION.RELEASE,
            locale = Locale.getDefault().toString(),
            device = Build.MODEL,
            build = "Build/" + Build.ID
        )
    }

    private fun requestParams(rawDeviceId: String, limitAdTracking: Boolean): ConversionTrackingRequest {
        val appVersion = getAppVersion()

        return ConversionTrackingRequest(
            devToken = BuildConfig.CONVERSION_DEV_TOKEN,
            linkId = BuildConfig.CONVERSION_LINK_ID,
            rawDeviceId = rawDeviceId,
            limitAdTracking = limitAdTracking,
            appVersion = appVersion,
            osVersion = Build.VERSION.RELEASE,
            sdkVersion = appVersion,
            timestamp = curTimestampInSeconds,
        )
    }

    /**
     * @return - the second parameter indicates whether retry is required
     */
    private fun acquireCampaignInfo(): AdEvent? {

        val adInfo = fetchAdInfo()
        Log.i(TAG, "Ad info $adInfo")

        if (adInfo == null || adInfo.id.isNullOrBlank()) {
            Log.i(TAG, "Invalid ad info")
            return null
        }

        try {
            val resp = ConversionClient.getConversion(
                ua = requestUA(),
                params = requestParams(
                    rawDeviceId = adInfo.id!!,
                    limitAdTracking = adInfo.isLimitAdTrackingEnabled
                ),
                timeout = timeout
            )

            Log.i(TAG, "Conversion tracking data loaded ${resp.body}")

            if (resp.body == null) {
                shouldRetry = true
                return null
            }

            if (resp.body.hasErrors() && resp.body.isTimestampInvalid()) {
                Log.i(TAG, "Set retry to true")
                backoffCount = calBackOffTime()
                shouldRetry = true
                return null
            }

            return resp.body.findLatestEvent()
        } catch (e: IOException) {
            e.message?.let { Log.i(TAG, it) }
            shouldRetry = true
            return null
        } catch (e: Exception) {
            e.message?.let { Log.i(TAG, it) }
            return null
        }
    }

    fun loadAdEvent(retries: Int): AdEvent? {
        Log.i(TAG, "Start loading google ads campaign")

        var adEvent = acquireCampaignInfo()

        for (i in 0 until  retries) {
            if (!shouldRetry) {
                break
            }

            Log.i(TAG, "Retry for $backoffCount time")
            adEvent = acquireCampaignInfo()
        }

        return adEvent
    }

    companion object {
        private const val TAG = "ConversionTracker"
    }
}
