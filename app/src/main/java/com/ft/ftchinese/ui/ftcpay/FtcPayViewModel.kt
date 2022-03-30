package com.ft.ftchinese.ui.ftcpay

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.model.fetch.FetchUi
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
import com.ft.ftchinese.store.PaywallCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "FtcPayViewModel"

sealed class OrderResult {
    data class WxPay(val order: WxPayIntent) : OrderResult()
    data class AliPay(val order: AliPayIntent) : OrderResult()
}

class FtcPayViewModel(application: Application) : AndroidViewModel(application) {

    val payIntentStore = PayIntentStore.getInstance(application)
    val invoiceStore = InvoiceStore.getInstance(application)

    val isNetworkAvailable = MutableLiveData<Boolean>()

    var cartItemState by mutableStateOf<CartItemFtcV2?>(null)
        private set

    var messageState by mutableStateOf<String?>(null)
        private set

    var stringResState by mutableStateOf<Int?>(null)
        private set

    val orderLiveData: MutableLiveData<OrderResult> by lazy {
        MutableLiveData<OrderResult>()
    }

    var fetchState by mutableStateOf<FetchUi>(FetchUi.Progress(false))
        private set

    fun clearPaymentError() {
        fetchState = FetchUi.Progress(false)
    }

    fun showMessage(msg: String) {
        fetchState = FetchUi.TextMsg(msg)
    }

    fun buildCart(priceId: String, account: Account) {
        // TODO: check if cache exists.
        val price = PaywallCache.findFtcPrice(priceId)
        if (price != null) {
            cartItemState = price
                .buildCartItem(account.membership.normalize())
            return
        }

        messageState = "Price $priceId not found"

        // TODO: load from server if not found.
    }

    fun createOrder(account: Account, cartItem: CartItemFtcV2, payMethod: PayMethod) {

        if (isNetworkAvailable.value == false) {
            stringResState = R.string.prompt_no_network
            return
        }

        fetchState = FetchUi.Progress(true)

        val params = OrderParams(
            priceId = cartItem.price.id,
            discountId = cartItem.discount?.id
        )

        when (payMethod) {
            PayMethod.ALIPAY -> createAliOrder(account, params)
            PayMethod.WXPAY -> createWxOrder(account, params)
            else -> {
                fetchState = FetchUi.Progress(false)
                stringResState = R.string.toast_no_pay_method
            }
        }
    }

    private fun createWxOrder(account: Account, params: OrderParams) {

        Log.i(TAG, "Creating wx order $params")
        viewModelScope.launch {
            try {
                val wxOrder = withContext(Dispatchers.IO) {
                    FtcPayClient.createWxOrder(account, params)
                }

                if (wxOrder == null) {
                    fetchState = FetchUi.ResMsg(R.string.toast_order_failed)
                    return@launch
                }

                if (wxOrder.params.app == null) {
                    fetchState = FetchUi.TextMsg("WxPayIntent.params.app should not be null")
                    return@launch
                }

                withContext(Dispatchers.IO) {
                    payIntentStore.save(wxOrder)
                }

                fetchState = FetchUi.Progress(false)
                orderLiveData.value = OrderResult.WxPay(wxOrder)
            } catch (e: APIError) {
                fetchState = if (e.statusCode == 403) {
                    FetchUi.ResMsg(R.string.duplicate_purchase)
                } else {
                    FetchUi.fromApi(e)
                }
            } catch (e: Exception) {
                fetchState = FetchUi.fromException(e)
            }
        }
    }

    private fun createAliOrder(account: Account, params: OrderParams) {

        Log.i(TAG, "Creating alipay order $params")

        viewModelScope.launch {
            try {
                val aliOrder = withContext(Dispatchers.IO) {
                    FtcPayClient.createAliOrder(account, params)
                }

                if (aliOrder == null) {
                    fetchState = FetchUi.ResMsg(R.string.toast_order_failed)
                    return@launch
                }

                withContext(Dispatchers.IO) {
                    payIntentStore.save(aliOrder)
                }

                fetchState = FetchUi.Progress(false)
                orderLiveData.value = OrderResult.AliPay(aliOrder)
            } catch (e: APIError) {
                Log.i(TAG, "$e")
                val msgId = if (e.statusCode == 403) {
                    R.string.duplicate_purchase
                } else {
                    null
                }

                fetchState = if (msgId != null) {
                    FetchUi.ResMsg(msgId)
                } else {
                    FetchUi.fromApi(e)
                }
            } catch (e: Exception) {
                Log.i(TAG, "$e")
                fetchState = FetchUi.fromException(e)
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
