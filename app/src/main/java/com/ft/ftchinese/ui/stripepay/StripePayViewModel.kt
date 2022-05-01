package com.ft.ftchinese.ui.stripepay

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.model.paywall.CartItemStripe
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.stripesubs.PaymentSheetParams
import com.ft.ftchinese.repository.StripeClient
import com.ft.ftchinese.ui.base.BaseViewModel
import com.stripe.android.ApiResultCallback
import com.stripe.android.Stripe
import com.stripe.android.model.SetupIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "StripePayViewModel"

class StripePayViewModel : BaseViewModel() {

    val inProgress: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    val status: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    val stringRes: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }

    val setupLiveData: MutableLiveData<PaymentSheetParams> by lazy {
        MutableLiveData<PaymentSheetParams>()
    }

    fun createSetupIntent(account: Account) {
        Log.i(TAG, "Creating setup intent")

        val customerId = account.stripeId ?: return

        inProgress.value = true
        viewModelScope.launch {

            try {
                val resp = withContext(Dispatchers.IO) {
                    StripeClient.setupWithEphemeral(customerId = customerId)
                }
                Log.i(TAG, "Http request result ${resp.body}")
                if (resp.body == null) {
                    stringRes.value = R.string.error_unknown
                    return@launch
                }

                inProgress.value = false
                setupLiveData.value = resp.body
            } catch (e: APIError) {
                inProgress.value = false
                status.value = e.message
            } catch (e: Exception) {
                inProgress.value = false
                status.value = e.message
            }
        }
    }

    fun subscribe(account: Account, item: CartItemStripe) {

    }
}
