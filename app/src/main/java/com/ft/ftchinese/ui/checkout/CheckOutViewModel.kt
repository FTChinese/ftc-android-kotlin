package com.ft.ftchinese.ui.checkout

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.ClientError
import com.ft.ftchinese.model.order.StripeSubParams
import com.ft.ftchinese.model.order.StripeSubResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.subscription.AliPayIntent
import com.ft.ftchinese.model.subscription.Plan
import com.ft.ftchinese.model.subscription.WxPayIntent
import com.ft.ftchinese.repository.StripeClient
import com.ft.ftchinese.repository.SubRepo
import com.ft.ftchinese.viewmodel.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class CheckOutViewModel : ViewModel(), AnkoLogger {

    val isNetworkAvailable = MutableLiveData<Boolean>()

    val wxPayIntentResult: MutableLiveData<Result<WxPayIntent>> by lazy {
        MutableLiveData<Result<WxPayIntent>>()
    }

    val aliPayIntentResult: MutableLiveData<Result<AliPayIntent>> by lazy {
        MutableLiveData<Result<AliPayIntent>>()
    }

    val stripeSubsResult: MutableLiveData<Result<StripeSubResult>> by lazy {
        MutableLiveData<Result<StripeSubResult>>()
    }

    val freeUpgradeResult: MutableLiveData<Result<Boolean>> by lazy {
        MutableLiveData<Result<Boolean>>()
    }

    fun createWxOrder(account: Account, plan: Plan) {
        if (isNetworkAvailable.value == false) {
            wxPayIntentResult.value = Result.LocalizedError(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val wxOrder = withContext(Dispatchers.IO) {
                    SubRepo.createWxOrder(account, plan)
                }

                if (wxOrder == null) {
                    wxPayIntentResult.value = Result.LocalizedError(R.string.toast_order_failed)
                    return@launch
                }
                wxPayIntentResult.value = Result.Success(wxOrder)
            } catch (e: ClientError) {

                wxPayIntentResult.value = if (e.statusCode == 403) {
                    Result.LocalizedError(R.string.duplicate_purchase)
                } else {
                    parseApiError(e)
                }
            } catch (e: Exception) {
                wxPayIntentResult.value = parseException(e)
            }
        }
    }

    fun createAliOrder(account: Account, plan: Plan) {
        if (isNetworkAvailable.value == false) {
            aliPayIntentResult.value = Result.LocalizedError(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val aliOrder = withContext(Dispatchers.IO) {
                    SubRepo.createAliOrder(account, plan)
                }

                if (aliOrder == null) {
                    aliPayIntentResult.value = Result.LocalizedError(R.string.toast_order_failed)
                    return@launch
                }
                aliPayIntentResult.value = Result.Success(aliOrder)
            } catch (e: ClientError) {
                info(e)
                val msgId = if (e.statusCode == 403) {
                    R.string.duplicate_purchase
                } else {
                    null
                }

                aliPayIntentResult.value = if (msgId != null) {
                    Result.LocalizedError(msgId)
                } else {
                    parseApiError(e)
                }
            } catch (e: Exception) {
                info(e)
                aliPayIntentResult.value = parseException(e)
            }
        }
    }

    fun createStripeSub(account: Account, params: StripeSubParams) {
        if (isNetworkAvailable.value == false) {
            stripeSubsResult.value = Result.LocalizedError(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val sub = withContext(Dispatchers.IO) {
                    StripeClient.createSubscription(account, params)
                }

                if (sub == null) {
                    stripeSubsResult.value = Result.LocalizedError(R.string.error_unknown)
                    return@launch
                }

                stripeSubsResult.value = Result.Success(sub)

            } catch (e: ClientError) {
                stripeSubsResult.value = if (e.type == "idempotency_error") {
                    Result.Error(IdempotencyError())
                } else {
                    parseApiError(e)
                }

            } catch (e: Exception) {
                stripeSubsResult.value = parseException(e)
            }
        }
    }

    fun upgradeStripeSub(account: Account, params: StripeSubParams) {
        if (isNetworkAvailable.value == false) {
            stripeSubsResult.value = Result.LocalizedError(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val sub = withContext(Dispatchers.IO) {
                    StripeClient.upgradeSub(account, params)
                }

                if (sub == null) {
                    stripeSubsResult.value = Result.LocalizedError(R.string.error_unknown)
                    return@launch
                }

                stripeSubsResult.value = Result.Success(sub)

            } catch (e: Exception) {
                stripeSubsResult.value = parseException(e)
            }
        }
    }
}
