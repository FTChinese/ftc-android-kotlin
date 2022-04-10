package com.ft.ftchinese.store

import android.content.Context
import androidx.core.content.edit
import com.ft.ftchinese.model.fetch.marshaller
import com.ft.ftchinese.model.ftcsubs.FtcPayIntent
import com.ft.ftchinese.model.ftcsubs.Order
import com.ft.ftchinese.model.ftcsubs.Price
import kotlinx.serialization.decodeFromString

/**
 * Saves the latest order and price returned from api.
 */
class PayIntentStore private constructor(ctx: Context) {

    private val sharedPref = ctx.getSharedPreferences(FILE_NAME_PI, Context.MODE_PRIVATE)

    fun save(pi: FtcPayIntent) {
        sharedPref.edit(commit = true) {
            clear()

            putString(KEY_PRICE, pi.price.toJsonString())
            putString(KEY_ORDER, pi.order.toJsonString())
        }
    }

    fun load(): FtcPayIntent? {
        val price = sharedPref.getString(KEY_ORDER, null)?.let {
            try {
                marshaller.decodeFromString<Price>(it)
            } catch (e: Exception) {
                null
            }
        } ?: return null

        val order = sharedPref.getString(KEY_ORDER, null)?.let {
            try {
                marshaller.decodeFromString<Order>(it)
            } catch(e: Exception) {
                null
            }
        } ?: return null

        return FtcPayIntent(
            price = price,
            order = order
        )
    }

    companion object {

        const val FILE_NAME_PI = "com.ft.ftchinese.payment_intent"
        const val KEY_PRICE = "price"
        const val KEY_ORDER = "order"

        private var instance: PayIntentStore? = null

        @Synchronized
        @JvmStatic
        fun getInstance(ctx: Context): PayIntentStore {
            if (instance == null) {
                instance = PayIntentStore(ctx)
            }

            return instance!!
        }
    }
}
