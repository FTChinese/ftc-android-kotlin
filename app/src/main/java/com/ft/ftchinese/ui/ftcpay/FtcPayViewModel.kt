package com.ft.ftchinese.ui.ftcpay

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.model.ftcsubs.AliPayIntent
import com.ft.ftchinese.model.ftcsubs.ConfirmationResult
import com.ft.ftchinese.model.ftcsubs.PayIntent
import com.ft.ftchinese.model.ftcsubs.WxPayIntent
import com.ft.ftchinese.model.paywall.CartItemFtcV2
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.request.OrderParams
import com.ft.ftchinese.repository.FtcPayClient
import com.ft.ftchinese.store.InvoiceStore
import com.ft.ftchinese.store.PayIntentStore
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.ui.components.ToastMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "FtcPayViewModel"

sealed class OrderResult {
    data class WxPay(val order: WxPayIntent) : OrderResult()
    data class AliPay(val order: AliPayIntent) : OrderResult()
}

class FtcPayViewModel(application: Application) : AndroidViewModel(application) {

    private val payIntentStore = PayIntentStore.getInstance(application)
    private val invoiceStore = InvoiceStore.getInstance(application)

    val isNetworkAvailable = MutableLiveData(application.isConnected)
    val inProgress = MutableLiveData(false)
    val toastMessage: MutableLiveData<ToastMessage> by lazy {
        MutableLiveData<ToastMessage>()
    }

    val orderLiveData: MutableLiveData<OrderResult> by lazy {
        MutableLiveData<OrderResult>()
    }

    fun clearPaymentError() {
        toastMessage.value = null
    }

    fun createOrder(account: Account, cartItem: CartItemFtcV2, payMethod: PayMethod) {

        if (isNetworkAvailable.value == false) {
            toastMessage.value = ToastMessage.Resource(R.string.prompt_no_network)
            return
        }

        val params = OrderParams(
            priceId = cartItem.price.id,
            discountId = cartItem.discount?.id
        )

        when (payMethod) {
            PayMethod.ALIPAY -> createAliOrder(account, params)
            PayMethod.WXPAY -> createWxOrder(account, params)
            else -> {
                toastMessage.value = ToastMessage.Resource(R.string.toast_no_pay_method)
            }
        }
    }

    private fun createWxOrder(account: Account, params: OrderParams) {
        inProgress.value = true

        Log.i(TAG, "Creating wx order $params")
        viewModelScope.launch {
            try {
                val wxOrder = withContext(Dispatchers.IO) {
                    FtcPayClient.createWxOrder(account, params)
                }

                inProgress.value = false

                if (wxOrder == null) {
                    toastMessage.value = ToastMessage.Resource(R.string.toast_order_failed)
                    return@launch
                }

                if (wxOrder.params.app == null) {
                    toastMessage.value = ToastMessage.Text("WxPayIntent.params.app should not be null")
                    return@launch
                }

                orderLiveData.value = OrderResult.WxPay(wxOrder)

                withContext(Dispatchers.IO) {
                    payIntentStore.save(wxOrder)
                }
            } catch (e: APIError) {
                inProgress.value = false
                toastMessage.value = if (e.statusCode == 403) {
                    ToastMessage.Resource(R.string.duplicate_purchase)
                } else {
                    ToastMessage.fromApi(e)
                }
            } catch (e: Exception) {
                inProgress.value = false
                toastMessage.value = ToastMessage.fromException(e)
            }
        }
    }

    private fun createAliOrder(account: Account, params: OrderParams) {
        inProgress.value = true

        Log.i(TAG, "Creating alipay order $params")

        viewModelScope.launch {
            try {
                val aliOrder = withContext(Dispatchers.IO) {
                    FtcPayClient.createAliOrder(account, params)
                }

                inProgress.value = false

                if (aliOrder == null) {
                    toastMessage.value = ToastMessage.Resource(R.string.toast_order_failed)
                    return@launch
                }

                orderLiveData.value = OrderResult.AliPay(aliOrder)

                withContext(Dispatchers.IO) {
                    payIntentStore.save(aliOrder)
                }
            } catch (e: APIError) {
                inProgress.value = false
                toastMessage.value = if (e.statusCode == 403) {
                    ToastMessage.Resource(R.string.duplicate_purchase)
                } else {
                    ToastMessage.fromApi(e)
                }

                Log.i(TAG, "$e")
            } catch (e: Exception) {
                inProgress.value = false
                toastMessage.value = ToastMessage.fromException(e)
                Log.i(TAG, "$e")
            }
        }
    }

    fun saveConfirmation(confirmed: ConfirmationResult, pi: PayIntent) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                invoiceStore.save(confirmed)
                payIntentStore.save(pi.withConfirmed(confirmed.order))
            }
        }
    }
}
