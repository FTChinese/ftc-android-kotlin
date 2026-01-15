package com.ft.ftchinese.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.ft.ftchinese.model.iapsubs.IAPSubsResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.reader.WxSession
import com.ft.ftchinese.model.stripesubs.StripeSubsResult
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.tracking.StatsTracker
import com.stripe.android.CustomerSession

private const val TAG = "UserViewModel"

open class UserViewModel(application: Application) : AndroidViewModel(application) {
    private val session = SessionManager.getInstance(application)
    private val tracker = StatsTracker.getInstance(application)

    val accountLiveData: MutableLiveData<Account> by lazy {
        MutableLiveData<Account>()
    }

    val loggedInLiveData = MediatorLiveData<Boolean>().apply {
        addSource(accountLiveData) {
            value = accountLiveData.value != null
        }
    }

    val testUserLiveData = MediatorLiveData<Boolean>().apply {
        addSource(accountLiveData) {
            value = accountLiveData.value?.isTest == true
        }
    }

    /**
     * When you changed account and wants to access its latest value in a callback,
     * use this getter since the callback might be using the values enclosed rather than
     * the latest data.
     * DO not rely this for composable update!
     */
    val account: Account?
        get() = accountLiveData.value

    init {
        accountLiveData.value = session.loadAccount(raw = true)
        syncFirebaseUserId(accountLiveData.value)
    }

    val isLoggedIn: Boolean
        get() = accountLiveData.value != null

    val isWxOnly: Boolean
        get() = accountLiveData.value?.isWxOnly == true

    fun reloadAccount(): Account? {
        // TODO: the copy might not be needed.
        val a = session.loadAccount(raw = true)?.copy()
        accountLiveData.value = a
        syncFirebaseUserId(a)
        Log.i(TAG, "Account reloaded $account")
        return a
    }

    fun saveAccount(a: Account) {
        accountLiveData.value = a
        session.saveAccount(a)
        syncFirebaseUserId(a)
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

    fun saveWxSession(wxSession: WxSession) {
        session.saveWxSession(wxSession)
    }

    /**
     * Check whether wechat session has expired.
     * Wechat refresh token expires after 30 days.
     */
    fun isWxSessionExpired(): Boolean {
        val a = account ?: return false

        if (!a.isWxOnly) {
            return false
        }

        val wxSess = session.loadWxSession() ?: return false
        return if (wxSess.isExpired) {
            logout()
            true
        } else {
            false
        }
    }

    fun logout() {
        accountLiveData.value = null
        syncFirebaseUserId(null)
        CustomerSession.endCustomerSession()
        session.logout()
    }

    private fun syncFirebaseUserId(account: Account?) {
        val id = account?.id?.takeIf { it.isNotBlank() }
        tracker.setUserId(id)
    }
}
