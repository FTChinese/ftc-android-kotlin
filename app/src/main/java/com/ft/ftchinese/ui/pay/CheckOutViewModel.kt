package com.ft.ftchinese.ui.pay

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.subscription.Plan
import com.ft.ftchinese.model.order.StripeSubParams
import com.ft.ftchinese.repository.StripeRepo
import com.ft.ftchinese.repository.SubRepo
import com.ft.ftchinese.util.ClientError
import com.ft.ftchinese.util.statusCodeMeaning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class CheckOutViewModel : ViewModel(), AnkoLogger {

    val wxOrderResult = MutableLiveData<WxOrderResult>()
    val wxPayResult = MutableLiveData<WxPayResult>()

    val aliOrderResult = MutableLiveData<AliOrderResult>()
//    val clientSecretResult = MutableLiveData<StringResult>()

    val stripePlanResult = MutableLiveData<StripePlanResult>()
    val stripeSubscribedResult = MutableLiveData<StripeSubscribedResult>()

    val upgradePreviewResult = MutableLiveData<UpgradePreviewResult>()
    val freeUpgradeResult = MutableLiveData<UpgradeResult>()

    fun previewUpgrade(account: Account) {
        viewModelScope.launch {
            try {
                val up = withContext(Dispatchers.IO) {
                    SubRepo.previewUpgrade(account)
                }

                info("Preview upgrade $up")

                upgradePreviewResult.value = UpgradePreviewResult(
                        success = up
                )
            } catch (e: ClientError) {

                upgradePreviewResult.value = UpgradePreviewResult(
                        errorId = when (e.statusCode) {
                            404 -> R.string.api_member_not_found
                            else -> statusCodeMeaning[e.statusCode]
                        },
                        exception = e
                )

            } catch (e: Exception) {
                upgradePreviewResult.value = UpgradePreviewResult(
                        exception = e
                )
            }
        }
    }

    fun freeUpgrade(account: Account) {
        viewModelScope.launch {
            try {
                val (ok, plan) = withContext(Dispatchers.IO) {
                    SubRepo.directUpgrade(account)
                }

                if (ok) {
                    freeUpgradeResult.value = UpgradeResult(
                            success = true
                    )
                    return@launch
                }

                freeUpgradeResult.value = UpgradeResult(
                        preview = plan
                )

            } catch (e: ClientError) {
                val msgId = when (e.statusCode) {
                    404 -> R.string.api_member_not_found
                    422 -> when (e.error?.key) {
                        "membership_already_upgraded" -> R.string.api_already_premium
                        else -> null
                    }
                    else -> e.parseStatusCode()
                }

                freeUpgradeResult.value = UpgradeResult(
                        error = msgId,
                        exception = e
                )
            } catch (e: Exception) {
                freeUpgradeResult.value = UpgradeResult(
                        exception = e
                )
            }
        }
    }

    fun createWxOrder(account: Account, plan: Plan) {
        viewModelScope.launch {
            try {
                val wxOrder = withContext(Dispatchers.IO) {
                    SubRepo.wxPlaceOrder(account, plan)
                }

                wxOrderResult.value = WxOrderResult(
                        success = wxOrder
                )
            } catch (e: ClientError) {
                val msgId = if (e.statusCode == 403) {
                    R.string.duplicate_purchase
                } else {
                    e.parseStatusCode()
                }

                wxOrderResult.value = WxOrderResult(
                        error =  msgId,
                        exception = e
                )
            } catch (e: Exception) {
                wxOrderResult.value = WxOrderResult(
                        exception = e
                )
            }
        }
    }

    fun queryWxPayStatus(account: Account, orderId: String) {
        viewModelScope.launch {
            try {
                val paymentStatus = withContext(Dispatchers.IO) {
                    SubRepo.wxQueryOrder(account, orderId)
                }

                wxPayResult.value = WxPayResult(
                        success = paymentStatus
                )
            } catch (e: Exception) {
                wxPayResult.value = WxPayResult(
                        exception = e
                )
            }
        }
    }

    fun createAliOrder(account: Account, plan: Plan) {
        viewModelScope.launch {
            try {
                val aliOrder = withContext(Dispatchers.IO) {
                    SubRepo.aliPlaceOrder(account, plan)
                }

                aliOrderResult.value = AliOrderResult(
                        success = aliOrder
                )
            } catch (e: ClientError) {
                val msgId = if (e.statusCode == 403) {
                    R.string.duplicate_purchase
                } else {
                    e.parseStatusCode()
                }

                aliOrderResult.value = AliOrderResult(
                        error = msgId,
                        exception = e
                )
            } catch (e: Exception) {
                aliOrderResult.value = AliOrderResult(
                        exception = e
                )
            }
        }
    }

    fun getStripePlan(plan: Plan?) {
        if (plan == null) {
            stripePlanResult.value = StripePlanResult(
                    error = R.string.prompt_unknown_plan
            )
            return
        }

        viewModelScope.launch {
            try {
                val stripePlan = withContext(Dispatchers.IO) {
                    StripeRepo.getStripePlan(plan.getId())
                }

                if (stripePlan == null) {
                    stripePlanResult.value = StripePlanResult(
                            error = R.string.prompt_unknown_plan
                    )
                    return@launch
                }

                stripePlanResult.value = StripePlanResult(
                        success = stripePlan
                )

            } catch (e: Exception) {
                stripePlanResult.value = StripePlanResult(
                        exception = e
                )
            }
        }
    }

    fun createStripeSub(account: Account, params: StripeSubParams) {
        viewModelScope.launch {
            try {
                val sub = withContext(Dispatchers.IO) {
                    StripeRepo.createSubscription(account, params)
                }

                stripeSubscribedResult.value = StripeSubscribedResult(
                        success = sub
                )
            } catch (e: ClientError) {
                stripeSubscribedResult.value = StripeSubscribedResult(
                        isIdempotencyError = e.type == "idempotency_error",
                        exception = e
                )

            } catch (e: Exception) {
                stripeSubscribedResult.value = StripeSubscribedResult(
                        exception = e
                )
            }
        }
    }

    fun upgradeStripeSub(account: Account, params: StripeSubParams) {
        viewModelScope.launch {
            try {
                val sub = withContext(Dispatchers.IO) {
                    StripeRepo.upgradeStripeSub(account, params)
                }

                stripeSubscribedResult.value = StripeSubscribedResult(
                        success = sub
                )
            } catch (e: Exception) {
                stripeSubscribedResult.value = StripeSubscribedResult(
                        exception = e
                )
            }
        }
    }

}
