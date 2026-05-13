package com.ft.ftchinese.wxapi

import android.content.Context
import android.content.Intent
import android.os.Build
import com.ft.ftchinese.BuildConfig
import com.tencent.mm.opensdk.modelbase.BaseResp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object WxLoginDiagnostic {
    private const val PREFS_NAME = "wx_login_diagnostic"
    private const val KEY_ACTIVE = "active"
    private const val KEY_PENDING = "pending"
    private const val KEY_SHOWN = "shown"
    private const val KEY_REQUEST_ID = "request_id"
    private const val KEY_KIND = "kind"
    private const val KEY_STATE = "state"
    private const val KEY_SENT = "sent"
    private const val KEY_LAUNCHED_AT = "launched_at"
    private const val KEY_ENTRY_ACTIVITY = "entry_activity"
    private const val KEY_ENTRY_EVENT = "entry_event"
    private const val KEY_ENTRY_AT = "entry_at"
    private const val KEY_INTENT_ACTION = "intent_action"
    private const val KEY_INTENT_DATA = "intent_data"
    private const val KEY_COMMAND_TYPE = "command_type"
    private const val KEY_RESP_ACTIVITY = "resp_activity"
    private const val KEY_RESP_TYPE = "resp_type"
    private const val KEY_RESP_ERR_CODE = "resp_err_code"
    private const val KEY_RESP_ERR_STR = "resp_err_str"
    private const val MIN_PENDING_MS = 1200L

    fun recordLaunch(
        context: Context,
        kind: String,
        state: String,
        sent: Boolean
    ) {
        prefs(context).edit()
            .putBoolean(KEY_ACTIVE, true)
            .putBoolean(KEY_PENDING, true)
            .putBoolean(KEY_SHOWN, false)
            .putString(KEY_REQUEST_ID, UUID.randomUUID().toString().take(8))
            .putString(KEY_KIND, kind)
            .putString(KEY_STATE, state)
            .putBoolean(KEY_SENT, sent)
            .putLong(KEY_LAUNCHED_AT, System.currentTimeMillis())
            .remove(KEY_ENTRY_ACTIVITY)
            .remove(KEY_ENTRY_EVENT)
            .remove(KEY_ENTRY_AT)
            .remove(KEY_INTENT_ACTION)
            .remove(KEY_INTENT_DATA)
            .remove(KEY_COMMAND_TYPE)
            .remove(KEY_RESP_ACTIVITY)
            .remove(KEY_RESP_TYPE)
            .remove(KEY_RESP_ERR_CODE)
            .remove(KEY_RESP_ERR_STR)
            .apply()
    }

    fun recordEntry(
        context: Context,
        activityName: String,
        event: String,
        intent: Intent?
    ) {
        prefs(context).edit()
            .putString(KEY_ENTRY_ACTIVITY, activityName)
            .putString(KEY_ENTRY_EVENT, event)
            .putLong(KEY_ENTRY_AT, System.currentTimeMillis())
            .putString(KEY_INTENT_ACTION, intent?.action.orEmpty())
            .putString(KEY_INTENT_DATA, intent?.dataString.orEmpty())
            .putInt(KEY_COMMAND_TYPE, intent?.extras?.getInt("_wxapi_command_type", -1) ?: -1)
            .apply()
    }

    fun recordResp(
        context: Context,
        activityName: String,
        resp: BaseResp?
    ) {
        prefs(context).edit()
            .putBoolean(KEY_PENDING, false)
            .putString(KEY_RESP_ACTIVITY, activityName)
            .putInt(KEY_RESP_TYPE, resp?.type ?: -1)
            .putInt(KEY_RESP_ERR_CODE, resp?.errCode ?: Int.MIN_VALUE)
            .putString(KEY_RESP_ERR_STR, resp?.errStr.orEmpty())
            .apply()
    }

    fun hasActiveRequest(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ACTIVE, false)

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    fun consumePendingIssue(context: Context): String? {
        val prefs = prefs(context)
        if (!prefs.getBoolean(KEY_PENDING, false) || prefs.getBoolean(KEY_SHOWN, false)) {
            return null
        }

        val launchedAt = prefs.getLong(KEY_LAUNCHED_AT, 0L)
        if (launchedAt <= 0L || System.currentTimeMillis() - launchedAt < MIN_PENDING_MS) {
            return null
        }

        prefs.edit().putBoolean(KEY_SHOWN, true).apply()
        return buildPendingMessage(context)
    }

    private fun buildPendingMessage(context: Context): String {
        val prefs = prefs(context)
        val launchedAt = prefs.getLong(KEY_LAUNCHED_AT, 0L)
        val ageSec = ((System.currentTimeMillis() - launchedAt).coerceAtLeast(0L) / 1000.0)
        val entryActivity = prefs.getString(KEY_ENTRY_ACTIVITY, "").orEmpty()
        val entryEvent = prefs.getString(KEY_ENTRY_EVENT, "").orEmpty()
        val commandType = prefs.getInt(KEY_COMMAND_TYPE, -1)
        val entryLine = if (entryActivity.isBlank()) {
            "entry=none"
        } else {
            "entry=$entryActivity/$entryEvent command=$commandType"
        }

        return buildString {
            appendLine("微信登录已发起，但 App 没有收到微信授权回调。")
            appendLine()
            appendLine("request=${prefs.getString(KEY_REQUEST_ID, "")}")
            appendLine("kind=${prefs.getString(KEY_KIND, "")}")
            appendLine("sendReq=${prefs.getBoolean(KEY_SENT, false)}")
            appendLine("age=${String.format(Locale.US, "%.1f", ageSec)}s")
            appendLine(entryLine)
            appendLine("state=${prefs.getString(KEY_STATE, "")}")
            appendLine("appId=${BuildConfig.WX_SUBS_APPID}")
            appendLine("package=${context.packageName}")
            appendLine("version=${BuildConfig.VERSION_NAME} ${BuildConfig.FLAVOR}/${BuildConfig.BUILD_TYPE}")
            appendLine("device=${Build.MANUFACTURER}/${Build.BRAND}/${Build.MODEL} sdk=${Build.VERSION.SDK_INT}")
            appendLine("time=${formatTime(launchedAt)}")
            appendLine()
            appendLine("请确认是否使用微信分身、应用双开或工作空间微信。")
        }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun formatTime(timeMs: Long): String {
        if (timeMs <= 0L) {
            return ""
        }

        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(timeMs))
    }
}
