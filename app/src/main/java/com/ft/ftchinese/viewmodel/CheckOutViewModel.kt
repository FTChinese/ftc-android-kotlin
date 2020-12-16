package com.ft.ftchinese.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.order.*
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.subscription.*
import com.ft.ftchinese.repository.StripeRepo
import com.ft.ftchinese.repository.SubRepo
import com.ft.ftchinese.repository.ClientError
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

    val stripePriceResult: MutableLiveData<Result<StripePrice>> by lazy {
        MutableLiveData<Result<StripePrice>>()
    }

    val stripeSubscribedResult: MutableLiveData<Result<StripeSubResult>> by lazy {
        MutableLiveData<Result<StripeSubResult>>()
    }

    val upgradePreviewResult: MutableLiveData<Result<PaymentIntent>> by lazy {
        MutableLiveData<Result<PaymentIntent>>()
    }

    val freeUpgradeResult: MutableLiveData<Result<Boolean>> by lazy {
        MutableLiveData<Result<Boolean>>()
    }

    fun createWxOrder(account: Account, plan: Plan) {
        viewModelScope.launch {
            try {
                val wxOrder = withContext(Dispatchers.IO) {
                    SubRepo.wxPlaceOrder(account, plan)
                }

                if (wxOrder == null) {
                    wxPayIntentResult.value = Result.LocalizedError(R.string.order_cannot_be_created)
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
        viewModelScope.launch {
            try {
                val aliOrder = withContext(Dispatchers.IO) {
                    SubRepo.aliPlaceOrder(account, plan)
                }

                if (aliOrder == null) {
                    aliPayIntentResult.value = Result.LocalizedError(R.string.order_cannot_be_created)
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

    fun getStripePlan(plan: Plan?) {
        if (isNetworkAvailable.value == false) {
            stripePriceResult.value = Result.LocalizedError(R.string.prompt_no_network)
            return
        }

        if (plan == null) {
            stripePriceResult.value = Result.LocalizedError(R.string.prompt_unknown_plan)
            return
        }

        viewModelScope.launch {
            try {
                val stripePlan = withContext(Dispatchers.IO) {
                    StripeRepo.loadPlan(plan.getNamedKey())
                }

                if (stripePlan == null) {
                    stripePriceResult.value = Result.LocalizedError(R.string.prompt_unknown_plan)
                    return@launch
                }

                stripePriceResult.value = Result.Success(stripePlan)


            } catch (e: Exception) {
                stripePriceResult.value = parseException(e)
            }
        }
    }

    fun createStripeSub(account: Account, params: StripeSubParams) {
        viewModelScope.launch {
            try {
                val sub = withContext(Dispatchers.IO) {
                    StripeRepo.createSubscription(account, params)
                }

                if (sub == null) {
                    stripeSubscribedResult.value = Result.LocalizedError(R.string.error_unknown)
                    return@launch
                }

                stripeSubscribedResult.value = Result.Success(sub)

            } catch (e: ClientError) {
                stripeSubscribedResult.value = if (e.type == "idempotency_error") {
                    Result.Error(IdempotencyError())
                } else {
                    parseApiError(e)
                }

            } catch (e: Exception) {
                stripeSubscribedResult.value = parseException(e)
            }
        }
    }

    fun upgradeStripeSub(account: Account, params: StripeSubParams) {
        viewModelScope.launch {
            try {
                val sub = withContext(Dispatchers.IO) {
                    StripeRepo.upgradeSub(account, params)
                }

                if (sub == null) {
                    stripeSubscribedResult.value = Result.LocalizedError(R.string.error_unknown)
                    return@launch
                }

                stripeSubscribedResult.value = Result.Success(sub)

            } catch (e: Exception) {
                stripeSubscribedResult.value = parseException(e)
            }
        }
    }

    fun previewUpgrade(account: Account) {
        viewModelScope.launch {
            try {
                val up = withContext(Dispatchers.IO) {
                    SubRepo.previewUpgrade(account)
                }

                info("Preview upgrade $up")
                if (up == null) {
                    upgradePreviewResult.value = Result.LocalizedError(R.string.balance_query_failed)
                    return@launch
                }

                upgradePreviewResult.value = Result.Success(up)

            } catch (e: ClientError) {

                upgradePreviewResult.value = if (e.statusCode == 404) {
                    Result.LocalizedError(R.string.api_member_not_found)
                } else {
                    parseApiError(e)
                }

            } catch (e: Exception) {
                upgradePreviewResult.value = parseException(e)
            }
        }
    }

    fun freeUpgrade(account: Account) {
        viewModelScope.launch {
            try {
                val (ok, pi) = withContext(Dispatchers.IO) {
                    SubRepo.directUpgrade(account)
                }

                if (ok) {
                    freeUpgradeResult.value = Result.Success(ok)
                    return@launch
                }

                if (pi == null) {
                    freeUpgradeResult.value = Result.LocalizedError(R.string.loading_failed)
                    return@launch
                }

                freeUpgradeResult.value = Result.Error(FreeUpgradeDeniedError(pi))

            } catch (e: ClientError) {
                val msgId = when (e.statusCode) {
                    404 -> R.string.api_member_not_found
                    422 -> when (e.error?.key) {
                        "membership_already_upgraded" -> R.string.api_already_premium
                        else -> null
                    }
                    else -> null
                }

                freeUpgradeResult.value = if (msgId != null) {
                    Result.LocalizedError(msgId)
                } else {
                    parseApiError(e)
                }

            } catch (e: Exception) {
                freeUpgradeResult.value = parseException(e)
            }
        }
    }
}
