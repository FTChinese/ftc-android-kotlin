package com.ft.ftchinese.model.order

import android.content.Context
import androidx.core.content.edit
import com.ft.ftchinese.util.json
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.util.*

private const val PREF_FILE_NAME = "stripe_pref"
private const val PREF_KEY = "key"
private const val PREF_CREATED = "created"
private const val PREF_USAGE = "usage"

data class Idempotency (
        val key: String,
        val created: ZonedDateTime
) {

    /**
     * checks whether th key is stale.
     * Idempotency key is only valid for 24 hours since creation.
     * @see See https://stripe.com/docs/api/idempotent_requests
     */
    fun stale(): Boolean {
        return created.plusHours(24).isBefore(ZonedDateTime.now())
    }
}

class StripePref private constructor(context: Context) {
    private val sharedPreferences = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)

    fun getKey(tier: Tier): Idempotency {
        val key = sharedPreferences.getString(PREF_KEY, null)
        val created = sharedPreferences.getString(PREF_CREATED, null)
        val usage = sharedPreferences.getString(PREF_USAGE, null)

        if (key == null || created == null || usage == null) {
            return generateIdempotency(tier)
        }

        val usageTier = Tier.fromString(usage)
        if (usageTier != tier) {
            return generateIdempotency(tier)
        }

        val i = Idempotency(
                key = key,
                created = ZonedDateTime.parse(created, DateTimeFormatter.ISO_DATE_TIME)
        )

        if (i.stale()) {
            return generateIdempotency(tier)
        }

        return i
    }

    private fun generateIdempotency(tier: Tier): Idempotency {
        val uuid = UUID.randomUUID().toString()
        val now = ZonedDateTime.now()
        sharedPreferences.edit {
            putString(PREF_KEY, uuid)
            putString(PREF_CREATED, now.format(DateTimeFormatter.ISO_DATE_TIME))
            putString(PREF_USAGE, tier.toString())
        }

        return Idempotency(
                key = uuid,
                created = now
        )
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
