package com.ft.ftchinese.ui.checkout

import android.util.Log
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.ftcsubs.AliPayIntent
import com.ft.ftchinese.model.ftcsubs.WxPayIntent
import com.ft.ftchinese.model.paywall.*
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.request.OrderParams
import com.ft.ftchinese.repository.FtcPayClient
import com.ft.ftchinese.ui.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "CheckOutViewModel"

class CheckOutViewModel : BaseViewModel() {

    val messageLiveData: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }

    val ftcItemLiveData: MutableLiveData<CartItemFtc> by lazy {
        MutableLiveData<CartItemFtc>()
    }

    val isTrial: Boolean
        get() = ftcItemLiveData.value?.isIntro ?: false

    // A list of payment methods
    val paymentChoices: MutableLiveData<PaymentChoices> by lazy {
        MutableLiveData<PaymentChoices>()
    }

    // For intro price, let user to select; otherwise
    // set it automatically.
    val stripePriceIdLiveData = MutableLiveData<String>()

    // The recurring prices a stripe trial attached to.
    val stripeRecurringChoicesLiveData: MutableLiveData<Array<String>> by lazy {
        MutableLiveData<Array<String>>()
    }

    // When user selected a payment method, trigger ui change.
    val paymentIntent = MutableLiveData<PaymentIntent>()

    val wxPayIntentResult: MutableLiveData<FetchResult<WxPayIntent>> by lazy {
        MutableLiveData<FetchResult<WxPayIntent>>()
    }

    val aliPayIntentResult: MutableLiveData<FetchResult<AliPayIntent>> by lazy {
        MutableLiveData<FetchResult<AliPayIntent>>()
    }

    val isAliPayEnabled = MediatorLiveData<Boolean>().apply {
        addSource(progressLiveData) {
            value = enablePayMethod(PayMethod.ALIPAY)
        }
        addSource(paymentChoices) {
            value = enablePayMethod(PayMethod.ALIPAY)
        }
    }

    val isWxPayEnabled = MediatorLiveData<Boolean>().apply {
        addSource(progressLiveData) {
            value = enablePayMethod(PayMethod.WXPAY)
        }
        addSource(paymentChoices) {
            value = enablePayMethod(PayMethod.WXPAY)
        }
    }

    val isStripeEnabled = MediatorLiveData<Boolean>().apply {
        addSource(progressLiveData) {
            value = enablePayMethod(PayMethod.STRIPE)
        }
        addSource(paymentChoices) {
            value = enablePayMethod(PayMethod.STRIPE)
        }
    }

    private fun enablePayMethod(m: PayMethod): Boolean {
        if (progressLiveData.value == true) {
            return false
        }

        return paymentChoices.value?.isPayMethodEnabled(m) ?: false
    }

    // Please not that you must put those getter methods after
    // the source variable declarations.
    val isFormEnabled = MediatorLiveData<Boolean>().apply {
        addSource(progressLiveData) {
            value = enableSubmit()
        }
        addSource(paymentIntent) {
            value = enableSubmit()
        }
        addSource(stripePriceIdLiveData) {
            value = enableSubmit()
        }
    }

    private fun enableSubmit(): Boolean {
        if (progressLiveData.value == true) {
            return false
        }

        if (paymentIntent.value == null) {
            return false
        }

        // As long as user selected alipay/wxpay, the check is over here.
        if (paymentIntent.value?.payMethod != PayMethod.STRIPE) {
            return true
        }

        // When user wants to pay via stripe, we have to ensure
        // stripe price id is chosen.
        if (stripePriceIdLiveData.value == null) {
            return false
        }

        return true
    }

    /**
     * When the UI is created, use price, optional discount and
     * membership to build PaymentCounter.
     */
    fun putIntoCart(item: CartItemFtc, m: Membership) {
        ftcItemLiveData.value = item
        paymentChoices.value = PaymentChoices.newInstance(m, item.price.edition)
        if (!item.isIntro) {
            stripePriceIdLiveData.value = item.price.stripePriceId
        }
    }

    /**
     * Select a payment method and compose PaymentIntent
     * based on user selection.
     */
    fun selectPayMethod(method: PayMethod) {
        Log.i(TAG, "Selecting payment method $method")
        ftcItemLiveData.value?.let { item ->

            paymentChoices.value?.findOrderIntent(method)?.kind?.let {
                paymentIntent.value = PaymentIntent(
                    item = item,
                    orderKind = it,
                    payMethod = method,
                )
            }

            if (method != PayMethod.STRIPE) {
                return@let
            }

            // When the item is an introductory, then we don't
            // know which recurring prices is being used.
            // Find the recurring prices and
            // ask user to select.
            // The observer will show a single choice dialog.
            if (item.isIntro && item.stripeTrialParents.size > 1) {
                stripeRecurringChoicesLiveData.value = item
                    .stripeTrialParents
                    .toTypedArray()
            }
        }
    }

    /**
     * Callback to handle stripe recurring price selection.
     * @param index the selected item index in a single choice list.
     */
    fun selectStripeRecurring(index: Int) {
        stripeRecurringChoicesLiveData.value?.let {
            if (index > it.size) {
                return@let
            }

            stripePriceIdLiveData.value = it[index]
        }
    }

    fun collectStripePriceIDs(): StripePriceIDs? {
        val orderKind = paymentChoices.value
            ?.findOrderIntent(PayMethod.STRIPE)
            ?.kind
            ?: return null

        return stripePriceIdLiveData.value?.let {
            StripePriceIDs(
                orderKind = orderKind,
                recurring = it,
                trial = ftcItemLiveData.value?.stripeTrialId()
            )
        }
    }

    fun createOrder(account: Account, pi: PaymentIntent) {
        if (isNetworkAvailable.value == false) {
            messageLiveData.value = R.string.prompt_no_network
            return
        }

        messageLiveData.value = R.string.toast_creating_order
        progressLiveData.value = true

        val params = OrderParams(
            priceId = pi.item.price.id,
            discountId = pi.item.discount?.id,
        )

        when (pi.payMethod) {
            PayMethod.ALIPAY -> createAliOrder(account, params)
            PayMethod.WXPAY -> createWxOrder(account, params)
            else -> {
                messageLiveData.value = R.string.toast_no_pay_method
                progressLiveData.value = false
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

                progressLiveData.value = false
                if (wxOrder == null) {
                    wxPayIntentResult.value = FetchResult.LocalizedError(R.string.toast_order_failed)
                    return@launch
                }
                wxPayIntentResult.value = FetchResult.Success(wxOrder)
            } catch (e: APIError) {
                progressLiveData.value = false
                wxPayIntentResult.value = if (e.statusCode == 403) {
                    FetchResult.LocalizedError(R.string.duplicate_purchase)
                } else {
                    FetchResult.fromApi(e)
                }
            } catch (e: Exception) {
                progressLiveData.value = false
                wxPayIntentResult.value = FetchResult.fromException(e)
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

                progressLiveData.value = false
                if (aliOrder == null) {
                    aliPayIntentResult.value = FetchResult.LocalizedError(R.string.toast_order_failed)
                    return@launch
                }
                aliPayIntentResult.value = FetchResult.Success(aliOrder)
            } catch (e: APIError) {
                progressLiveData.value = false
                Log.i(TAG, "$e")
                val msgId = if (e.statusCode == 403) {
                    R.string.duplicate_purchase
                } else {
                    null
                }

                aliPayIntentResult.value = if (msgId != null) {
                    FetchResult.LocalizedError(msgId)
                } else {
                    FetchResult.fromApi(e)
                }
            } catch (e: Exception) {
                progressLiveData.value = false
                Log.i(TAG, "$e")
                aliPayIntentResult.value = FetchResult.fromException(e)
            }
        }
    }
}
