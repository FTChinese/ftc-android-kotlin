package com.ft.ftchinese.model.order

import android.content.Context
import androidx.core.content.edit
import com.ft.ftchinese.util.json

private const val PREF_FILE_NAME = "stripe_pref"
private const val PREF_KEY = "idempotency_key"
private const val PREF_KEY_CREATED = "key_created_at"

class StripePref private constructor(context: Context) {
    private val sharedPreferences = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)

    fun getKey() {

    }

    fun savePlan(key: String?, plan: StripePlan) {
        if (key == null) {
            return
        }
        val data = json.toJsonString(plan)

        sharedPreferences.edit {
            putString(key, data)
        }
    }

    fun getPlan(key: String?): StripePlan? {
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

    companion object {
        private var instance: StripePref? = null

        @JvmStatic
        @Synchronized
        fun getInstance(ctx: Context): StripePref {
            if (instance == null) {
                instance = StripePref(ctx)
            }

            return instance!!
        }
    }
}
