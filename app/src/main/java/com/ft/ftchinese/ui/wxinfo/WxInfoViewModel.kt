package com.ft.ftchinese.ui.wxinfo

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.WxSession
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.ui.components.ToastMessage
import com.ft.ftchinese.viewmodel.UserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WxInfoViewModel(application: Application) : UserViewModel(application) {

    val reAuthLiveData: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>(false)
    }

    fun clearReAuth() {
        reAuthLiveData.value = false
    }

    // When user is logged in with Wechat, it must be a wechat-only account.
    // After rereshing, the account linking status could only have 2 case:
    // It is kept intact, so we only need to update the the ui data;
    // It is linked to an email account (possibly on other platforms). In such case wechat info is still kept, so we only show
    // the change info without switching to AccountActivity.
    // If is impossible for a wechat-only user to become
    // an email-only user.
    fun refreshWxInfo(a: Account) {
        val wxSession = session.loadWxSession()
        if (wxSession == null) {
            reAuthLiveData.value = true
            return
        }
        if (!ensureNetwork()) {
            return
        }

        refreshingLiveData.value = true

        viewModelScope.launch {
            val (done, toast) = asyncRefreshWxSession(wxSession)
            if (toast != null) {
                toastLiveData.value = toast
            }
            if (!done) {
                refreshingLiveData.value = false
                reAuthLiveData.value = true
                return@launch
            }

            asyncRefreshAccount(a)?.let {
                saveAccount(it)
            }
            progressLiveData.value = false
        }
    }

    private suspend fun asyncRefreshWxSession(sess: WxSession): Pair<Boolean, ToastMessage?> {
        return try {
            val done = withContext(Dispatchers.IO) {
                AccountRepo.refreshWxInfo(wxSession = sess)
            }

            Pair(done, null)
        } catch (e: APIError) {
            Pair(false, ToastMessage.fromApi(e))
        } catch (e: Exception) {
            Pair(false, ToastMessage.fromException(e))
        }
    }
}
