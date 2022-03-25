package com.ft.ftchinese.store

import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Membership

@Deprecated("Removed after migration to single activity")
object AccountCache {
    private var account: Account? = null

    fun save(a: Account) {
        account = a
    }

    fun get(): Account? = account

    fun updateMembership(m: Membership) {
        account = account?.withMembership(m)
    }

    fun updateStripeID(cusID: String) {
        account = account?.withCustomerID(cusID)
    }

    fun clear() {
        account = null
    }
}
