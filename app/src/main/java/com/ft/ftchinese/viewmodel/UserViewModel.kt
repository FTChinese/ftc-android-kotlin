package com.ft.ftchinese.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.ft.ftchinese.model.iapsubs.IAPSubsResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.stripesubs.StripeSubsResult
import com.ft.ftchinese.store.SessionManager

open class UserViewModel(application: Application) : BaseAppViewModel(application) {
    protected val session = SessionManager.getInstance(application)

    val accountLiveData: MutableLiveData<Account> by lazy {
        MutableLiveData<Account>()
    }

    val account: Account?
        get() = accountLiveData.value

    init {
        accountLiveData.value = session.loadAccount(raw = true)
    }

    val isLoggedIn: Boolean
        get() = accountLiveData.value != null

    val isWxOnly: Boolean
        get() = accountLiveData.value?.isWxOnly == true

    fun reloadAccount() {
        accountLiveData.value = session.loadAccount(raw = true)
    }

    fun saveAccount(a: Account) {
        accountLiveData.value = a
        session.saveAccount(a)
    }

    fun saveMembership(m: Membership) {
        accountLiveData.value = accountLiveData.value?.withMembership(m)
        session.saveMembership(m)
    }

    fun saveStripeSubs(subsResult: StripeSubsResult) {
        saveMembership(subsResult.membership)
        session.saveStripeSubs(subsResult.subs)
    }

    fun saveIapSubs(subsResult: IAPSubsResult) {
        saveMembership(subsResult.membership)
        session.saveIapSus(subsResult.subscription)
    }

    fun logout() {
        accountLiveData.value = null
        session.logout()
    }
}
