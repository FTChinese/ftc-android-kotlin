package com.ft.ftchinese.model.order

import android.content.Context
import androidx.core.content.edit
import com.ft.ftchinese.util.json

private const val PREF_FILE_NAME = "stripe_plans"

class StripePlanCache private constructor(context: Context) {
    private val sharedPreferences = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)

    fun save(plan: StripePlan, key: String?) {
        if (key == null) {
            return
        }

        val data = json.toJsonString(plan)

        sharedPreferences.edit {
            putString(key, data)
        }
    }

    fun load(key: String?): StripePlan? {
        if (key == null) {
            return null
        }

        val data = sharedPreferences.getString(key, null) ?: return null

        return try {
            json.parse<StripePlan>(data)
        } catch (e: Exception) {
            null
        }
    }

    fun clear() {
        sharedPreferences.edit {
            clear()
        }
    }

    companion object {
        private var instance: StripePlanCache? = null

        @JvmStatic
        @Synchronized
        fun getInstance(ctx: Context): StripePlanCache {
            if (instance == null) {
                instance = StripePlanCache(ctx.applicationContext)
            }

            return instance!!
        }
    }
}
