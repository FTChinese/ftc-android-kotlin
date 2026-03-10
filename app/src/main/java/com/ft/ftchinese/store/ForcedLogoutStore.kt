package com.ft.ftchinese.store

import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import com.ft.ftchinese.R

enum class ForcedLogoutReason(@StringRes val messageId: Int) {
    ACCOUNT_REPLACED(R.string.account_signed_in_elsewhere),
    SESSION_EXPIRED(R.string.session_expired_relogin),
}

data class ForcedLogoutEvent(
    val reason: ForcedLogoutReason,
    val triggeredAtMillis: Long = System.currentTimeMillis(),
)

object ForcedLogoutStore {
    val eventLiveData = MutableLiveData<ForcedLogoutEvent?>()

    fun trigger(reason: ForcedLogoutReason) {
        eventLiveData.postValue(ForcedLogoutEvent(reason = reason))
    }

    fun clear() {
        eventLiveData.postValue(null)
    }
}
