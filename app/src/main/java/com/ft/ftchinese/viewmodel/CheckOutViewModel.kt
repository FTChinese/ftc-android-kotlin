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

    val wxOrderResult: MutableLiveData<Result<WxOrder>> by lazy {
        MutableLiveData<Result<WxOrder>>()
    }
    val payResult: MutableLiveData<Result<PaymentResult>> by lazy {
        MutableLiveData<Result<PaymentResult>>()
    }

    val aliOrderResult: MutableLiveData<Result<AliOrder>> by lazy {
        MutableLiveData<Result<AliOrder>>()
    }

    val stripePlanResult: MutableLiveData<Result<StripePlan>> by lazy {
        MutableLiveData<Result<StripePlan>>()
    }

    val stripeSubscribedResult: MutableLiveData<Result<StripeSubResponse>> by lazy {
        MutableLiveData<Result<StripeSubResponse>>()
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
                    wxOrderResult.value = Result.LocalizedError(R.string.order_cannot_be_created)
                    return@launch
                }
                wxOrderResult.value = Result.Success(wxOrder)
            } catch (e: ClientError) {

                wxOrderResult.value = if (e.statusCode == 403) {
                    Result.LocalizedError(R.string.duplicate_purchase)
                } else {
                    parseApiError(e)
                }
            } catch (e: Exception) {
                wxOrderResult.value = parseException(e)
            }
        }
    }

    fun queryWxPayStatus(account: Account, orderId: String) {
        viewModelScope.launch {
            try {
                val paymentStatus = withContext(Dispatchers.IO) {
                    SubRepo.wxQueryOrder(account, orderId)
                }

                if (paymentStatus == null) {
                    payResult.value = Result.LocalizedError(R.string.order_cannot_be_queried)
                    return@launch
                }

                payResult.value = Result.Success(paymentStatus)
            } catch (e: Exception) {
                payResult.value = parseException(e)
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
                    aliOrderResult.value = Result.LocalizedError(R.string.order_cannot_be_created)
                    return@launch
                }
                aliOrderResult.value = Result.Success(aliOrder)
            } catch (e: ClientError) {
                info(e)
                val msgId = if (e.statusCode == 403) {
                    R.string.duplicate_purchase
                } else {
                    null
                }

                aliOrderResult.value = if (msgId != null) {
                    Result.LocalizedError(msgId)
                } else {
                    parseApiError(e)
                }
            } catch (e: Exception) {
                info(e)
                aliOrderResult.value = parseException(e)
            }
        }
    }

    fun verifyPayment(account: Account, orderId: String) {
        viewModelScope.launch {
            try {
                val pr = withContext(Dispatchers.IO) {
                    SubRepo.verifyPayment(account, orderId)
                }

                if (pr == null) {
                    payResult.value = Result.LocalizedError(R.string.order_cannot_be_queried)
                    return@launch
                }

                payResult.value = Result.Success(pr)
            } catch (e: Exception) {
                payResult.value = parseException(e)
            }
        }
    }

    fun getStripePlan(plan: Plan?) {
        if (plan == null) {
            stripePlanResult.value = Result.LocalizedError(R.string.prompt_unknown_plan)
            return
        }

        viewModelScope.launch {
            try {
                val stripePlan = withContext(Dispatchers.IO) {
                    StripeRepo.getStripePlan(plan.getNamedKey())
                }

                if (stripePlan == null) {
                    stripePlanResult.value = Result.LocalizedError(R.string.prompt_unknown_plan)
                    return@launch
                }

                stripePlanResult.value = Result.Success(stripePlan)


            } catch (e: Exception) {
                stripePlanResult.value = parseException(e)
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
                    StripeRepo.upgradeStripeSub(account, params)
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
