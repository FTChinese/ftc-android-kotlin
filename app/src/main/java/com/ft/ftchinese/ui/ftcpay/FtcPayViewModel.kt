package com.ft.ftchinese.ui.ftcpay

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.model.ftcsubs.AliPayIntent
import com.ft.ftchinese.model.ftcsubs.ConfirmationResult
import com.ft.ftchinese.model.ftcsubs.FtcPayIntent
import com.ft.ftchinese.model.ftcsubs.WxPayIntent
import com.ft.ftchinese.model.paywall.CartItemFtc
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.request.OrderParams
import com.ft.ftchinese.repository.FtcPayClient
import com.ft.ftchinese.store.InvoiceStore
import com.ft.ftchinese.store.PayIntentStore
import com.ft.ftchinese.tracking.BeginCheckoutParams
import com.ft.ftchinese.tracking.PaySuccessParams
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.components.ToastMessage
import com.ft.ftchinese.viewmodel.BaseAppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "FtcPayViewModel"

sealed class OrderResult {
    data class WxPay(val order: WxPayIntent) : OrderResult()
    data class AliPay(val order: AliPayIntent) : OrderResult()
}

class FtcPayViewModel(application: Application) : BaseAppViewModel(application) {

    private val tracker = StatsTracker.getInstance(application)
    private val payIntentStore = PayIntentStore.getInstance(application)
    private val invoiceStore = InvoiceStore.getInstance(application)

    val orderLiveData: MutableLiveData<OrderResult> by lazy {
        MutableLiveData<OrderResult>()
    }

    fun createOrder(account: Account, cartItem: CartItemFtc, payMethod: PayMethod) {

        if (!ensureNetwork()) {
            return
        }

        val params = OrderParams(
            priceId = cartItem.price.id,
            discountId = cartItem.discount?.id
        )

        when (payMethod) {
            PayMethod.ALIPAY -> {
                createAliOrder(account, params)

                tracker.beginCheckOut(
                    BeginCheckoutParams.ofFtc(
                        item = cartItem,
                        method = PayMethod.ALIPAY,
                    )
                )
            }
            PayMethod.WXPAY -> {
                createWxOrder(account, params)

                tracker.beginCheckOut(
                    BeginCheckoutParams.ofFtc(
                        item = cartItem,
                        method = PayMethod.WXPAY,
                    )
                )
            }
            else -> {
                toastLiveData.value = ToastMessage.Resource(R.string.toast_no_pay_method)
            }
        }
    }

    private fun createWxOrder(account: Account, params: OrderParams) {
        progressLiveData.value = true

        Log.i(TAG, "Creating wx order $params")
        viewModelScope.launch {
            try {
                val wxOrder = withContext(Dispatchers.IO) {
                    FtcPayClient.createWxOrder(account, params)
                }



                if (wxOrder == null) {
                    progressLiveData.value = false
                    toastLiveData.value = ToastMessage.Resource(R.string.toast_order_failed)
                    return@launch
                }

                if (wxOrder.params.app == null) {
                    progressLiveData.value = false
                    toastLiveData.value = ToastMessage.Text("WxPayIntent.params.app should not be null")
                    return@launch
                }

                withContext(Dispatchers.IO) {
                    payIntentStore.save(wxOrder.toPayIntent())
                }

                orderLiveData.value = OrderResult.WxPay(wxOrder)
                progressLiveData.value = false
            } catch (e: APIError) {
                progressLiveData.value = false
                toastLiveData.value = if (e.statusCode == 403) {
                    ToastMessage.Resource(R.string.duplicate_purchase)
                } else {
                    ToastMessage.fromApi(e)
                }
            } catch (e: Exception) {
                progressLiveData.value = false
                toastLiveData.value = ToastMessage.fromException(e)
            }
        }
    }

    private fun createAliOrder(account: Account, params: OrderParams) {
        progressLiveData.value = true

        Log.i(TAG, "Creating alipay order $params")

        viewModelScope.launch {
            try {
                val aliOrder = withContext(Dispatchers.IO) {
                    FtcPayClient.createAliOrder(account, params)
                }

                if (aliOrder == null) {
                    progressLiveData.value = false
                    toastLiveData.value = ToastMessage.Resource(R.string.toast_order_failed)
                    return@launch
                }

                withContext(Dispatchers.IO) {
                    payIntentStore.save(aliOrder.toPayIntent())
                }

                orderLiveData.value = OrderResult.AliPay(aliOrder)
                progressLiveData.value = false
            } catch (e: APIError) {
                progressLiveData.value = false
                toastLiveData.value = if (e.statusCode == 403) {
                    ToastMessage.Resource(R.string.duplicate_purchase)
                } else {
                    ToastMessage.fromApi(e)
                }

                Log.i(TAG, "$e")
            } catch (e: Exception) {
                progressLiveData.value = false
                toastLiveData.value = ToastMessage.fromException(e)
                Log.i(TAG, "$e")
            }
        }
    }

    fun saveConfirmation(confirmed: ConfirmationResult, pi: FtcPayIntent) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                invoiceStore.save(confirmed)
                payIntentStore.save(pi.withConfirmed(confirmed.order))
            }
        }

        tracker.paySuccess(PaySuccessParams.ofFtc(pi))
    }

    fun trackFailedPay(pi: FtcPayIntent) {
        tracker.payFailed(pi.price.edition)
    }
}
