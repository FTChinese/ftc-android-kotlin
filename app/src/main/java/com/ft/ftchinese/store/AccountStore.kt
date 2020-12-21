package com.ft.ftchinese.store

import android.content.Context
import androidx.core.content.edit
import com.ft.ftchinese.model.subscription.StripeCustomer
import com.ft.ftchinese.model.fetch.json

private const val PREF_FILE_NAME = "com.ft.ftchinese.account"
private const val KEY_STRIPE_CUSTOMER = "stripe_customer"

object AccountStore {
    var customer: StripeCustomer? = null

    fun saveStripeCustomer(ctx: Context, c: StripeCustomer) {
        customer = c

        val data = try {
            json.toJsonString(c)
        } catch (e: Exception) {
            return
        }

        ctx.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(KEY_STRIPE_CUSTOMER, data)
            }
    }

    fun loadStripeCustomer(ctx: Context): StripeCustomer? {
        val data = ctx
            .getSharedPreferences(
                PREF_FILE_NAME,
                Context.MODE_PRIVATE
            )
            .getString(KEY_STRIPE_CUSTOMER, null)
            ?: return null

        return try {
            json.parse<StripeCustomer>(data)
        } catch (e: Exception) {
            null
        }
    }
}
