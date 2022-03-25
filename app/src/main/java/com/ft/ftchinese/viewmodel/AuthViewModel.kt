package com.ft.ftchinese.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.store.SessionManager

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val session = SessionManager.getInstance(application)

    var account by mutableStateOf<Account?>(null)
        private set

    init {
        account = session.loadAccount(raw = true)
    }

    fun load() {
        account = session.loadAccount(raw = true)
    }

    fun save(a: Account) {
        account = a
        session.saveAccount(a)
    }

    fun saveMembership(m: Membership) {
        account = account?.withMembership(m)
        session.saveMembership(m)
    }

    fun saveStripeId(id: String) {
        account = account?.withCustomerID(id)
        session.saveStripeId(id)
    }

    fun clear() {
        account = null
        session.logout()
    }
}
