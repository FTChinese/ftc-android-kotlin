package com.ft.ftchinese.ui.checkout

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.model.ftcsubs.AliPayIntent
import com.ft.ftchinese.model.ftcsubs.CheckoutItem
import com.ft.ftchinese.model.ftcsubs.WxPayIntent
import com.ft.ftchinese.model.price.Price
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.stripesubs.StripeSubsResult
import com.ft.ftchinese.model.stripesubs.SubParams
import com.ft.ftchinese.repository.FtcPayClient
import com.ft.ftchinese.repository.StripeClient
import com.ft.ftchinese.ui.base.BaseViewModel
import com.ft.ftchinese.viewmodel.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class CheckOutViewModel : BaseViewModel(), AnkoLogger {

    val counterLiveData: MutableLiveData<PaymentCounter> by lazy {
        MutableLiveData<PaymentCounter>()
    }

    /**
     * When the UI is created, use price, optional discount and
     * membership to build PaymentCounter.
     */
    fun putIntoFtcCart(item: CheckoutItem, m: Membership) {
        counterLiveData.value = PaymentCounter.newFtcInstance(item, m)
    }

    /**
     * When the UI is created, use the price and current
     * membership to build PaymentCounter.
     */
    fun putIntoStripeCart(price: Price, m: Membership) {
        counterLiveData.value = PaymentCounter.newStripeInstance(price, m)
    }

    val paymentMethod: PayMethod?
        get() = payMethodSelected.value?.payMethod

    val payMethodSelected: MutableLiveData<PaymentIntent> by lazy {
        MutableLiveData<PaymentIntent>()
    }

    fun selectPayMethod(method: PayMethod) {
        counterLiveData.value?.selectPaymentMethod(method)?.let {
            payMethodSelected.value = it
        }
    }

    val wxPayIntentResult: MutableLiveData<FetchResult<WxPayIntent>> by lazy {
        MutableLiveData<FetchResult<WxPayIntent>>()
    }

    val aliPayIntentResult: MutableLiveData<FetchResult<AliPayIntent>> by lazy {
        MutableLiveData<FetchResult<AliPayIntent>>()
    }

    val stripeSubsResult: MutableLiveData<FetchResult<StripeSubsResult>> by lazy {
        MutableLiveData<FetchResult<StripeSubsResult>>()
    }

    // Create CheckoutCounter instance and tell CartItemFragment to update ui.
//    fun initFtcCounter(priceId: String, m: Membership) {
//        val price = FtcPriceCache.find(priceId)
//        if (price != null) {
//            priceSelected.value = price
//            val c = CheckoutCounter(price, m)
//            _checkoutCounter = c
//            counterResult.value = FetchResult.Success(c)
//        } else {
//            counterResult.value = FetchResult.Error(Exception("Price not found"))
//        }
//    }

//    @Deprecated("", replaceWith = ReplaceWith("putIntoStripeCart"))
//    fun initStripeCounter(priceId: String, m: Membership) {
//        val price = StripePriceCache.find(priceId)
//        if (price != null) {
//            priceSelected.value = price
//            val c = CheckoutCounter(price, m)
//            _checkoutCounter = c
//            counterResult.value = FetchResult.Success(c)
//        } else {
//            counterResult.value = FetchResult.Error(Exception("Price not found"))
//        }
//    }

    fun createWxOrder(account: Account) {
        if (isNetworkAvailable.value == false) {
            wxPayIntentResult.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        val item = counterLiveData.value?.item
        if (item == null) {
            wxPayIntentResult.value = FetchResult.Error(Exception("Price not found"))
            return
        }

        viewModelScope.launch {
            try {
                val wxOrder = withContext(Dispatchers.IO) {
                    FtcPayClient.createWxOrder(account, item.reqParams)
                }

                if (wxOrder == null) {
                    wxPayIntentResult.value = FetchResult.LocalizedError(R.string.toast_order_failed)
                    return@launch
                }
                wxPayIntentResult.value = FetchResult.Success(wxOrder)
            } catch (e: APIError) {

                wxPayIntentResult.value = if (e.statusCode == 403) {
                    FetchResult.LocalizedError(R.string.duplicate_purchase)
                } else {
                    FetchResult.fromServerError(e)
                }
            } catch (e: Exception) {
                wxPayIntentResult.value = FetchResult.fromException(e)
            }
        }
    }

    fun createAliOrder(account: Account) {
        if (isNetworkAvailable.value == false) {
            aliPayIntentResult.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        val item = counterLiveData.value?.item
        if (item == null) {
            aliPayIntentResult.value = FetchResult.Error(Exception("Price not found"))
            return
        }

        viewModelScope.launch {
            try {
                val aliOrder = withContext(Dispatchers.IO) {
                    FtcPayClient.createAliOrder(account, item.reqParams)
                }

                if (aliOrder == null) {
                    aliPayIntentResult.value = FetchResult.LocalizedError(R.string.toast_order_failed)
                    return@launch
                }
                aliPayIntentResult.value = FetchResult.Success(aliOrder)
            } catch (e: APIError) {
                info(e)
                val msgId = if (e.statusCode == 403) {
                    R.string.duplicate_purchase
                } else {
                    null
                }

                aliPayIntentResult.value = if (msgId != null) {
                    FetchResult.LocalizedError(msgId)
                } else {
                    FetchResult.fromServerError(e)
                }
            } catch (e: Exception) {
                info(e)
                aliPayIntentResult.value = FetchResult.fromException(e)
            }
        }
    }

    fun createStripeSub(account: Account, params: SubParams) {
        if (isNetworkAvailable.value == false) {
            stripeSubsResult.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val sub = withContext(Dispatchers.IO) {
                    StripeClient.createSubscription(account, params)
                }

                if (sub == null) {
                    stripeSubsResult.value = FetchResult.LocalizedError(R.string.error_unknown)
                    return@launch
                }

                stripeSubsResult.value = FetchResult.Success(sub)

            } catch (e: APIError) {
                stripeSubsResult.value = if (e.type == "idempotency_error") {
                    FetchResult.Error(IdempotencyError())
                } else {
                    FetchResult.fromServerError(e)
                }

            } catch (e: Exception) {
                stripeSubsResult.value = FetchResult.fromException(e)
            }
        }
    }

    fun upgradeStripeSub(account: Account, params: SubParams) {
        if (isNetworkAvailable.value == false) {
            stripeSubsResult.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val sub = withContext(Dispatchers.IO) {
                    StripeClient.updateSubs(account, params)
                }

                if (sub == null) {
                    stripeSubsResult.value = FetchResult.LocalizedError(R.string.error_unknown)
                    return@launch
                }

                stripeSubsResult.value = FetchResult.Success(sub)

            } catch (e: Exception) {
                stripeSubsResult.value = FetchResult.fromException(e)
            }
        }
    }
}
