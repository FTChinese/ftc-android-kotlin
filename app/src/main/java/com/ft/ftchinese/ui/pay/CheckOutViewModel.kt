package com.ft.ftchinese.ui.pay

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.Account
import com.ft.ftchinese.model.order.Plan
import com.ft.ftchinese.model.order.StripeSubParams
import com.ft.ftchinese.util.ClientError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class CheckOutViewModel : ViewModel(), AnkoLogger {


    val inputEnabled = MutableLiveData<Boolean>()
    val wxOrderResult = MutableLiveData<WxOrderResult>()
    val aliOrderResult = MutableLiveData<AliOrderResult>()
//    val clientSecretResult = MutableLiveData<StringResult>()
    val stripePlanResult = MutableLiveData<StripePlanResult>()
    val stripeSubResult = MutableLiveData<StripeSubResult>()

    val upgradePreviewResult = MutableLiveData<UpgradePreviewResult>()
    val freeUpgradeResult = MutableLiveData<UpgradeResult>()

    // Enable/Disable a UI, like button.
    fun enableInput(v: Boolean) {
        inputEnabled.value = v
    }

    fun previewUpgrade(account: Account) {
        viewModelScope.launch {
            try {
                val up = withContext(Dispatchers.IO) {
                    account.previewUpgrade()
                }

                info("Preview upgrade $up")

                upgradePreviewResult.value = UpgradePreviewResult(
                        success = up
                )
            } catch (e: ClientError) {
                val msgId = when (e.statusCode) {
                    404 -> R.string.api_member_not_found
                    else -> e.statusMessage()
                }

                upgradePreviewResult.value = UpgradePreviewResult(
                        error = msgId,
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
                    account.directUpgrade()
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
                    else -> e.statusMessage()
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
                    account.wxPlaceOrder(plan.tier, plan.cycle)
                }

                wxOrderResult.value = WxOrderResult(
                        success = wxOrder
                )
            } catch (e: ClientError) {
                val msgId = if (e.statusCode == 403) {
                    R.string.renewal_not_allowed
                } else {
                    e.statusMessage()
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

    fun createAliOrder(account: Account, plan: Plan) {
        viewModelScope.launch {
            try {
                val aliOrder = withContext(Dispatchers.IO) {
                    account.aliPlaceOrder(plan.tier, plan.cycle)
                }

                aliOrderResult.value = AliOrderResult(
                        success = aliOrder
                )
            } catch (e: ClientError) {
                val msgId = if (e.statusCode == 403) {
                    R.string.renewal_not_allowed
                } else {
                    e.statusMessage()
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

    fun getStripePlan(account: Account, plan: Plan?) {
        if (plan == null) {
            stripePlanResult.value = StripePlanResult(
                    error = R.string.prompt_unknown_plan
            )
            return
        }

        viewModelScope.launch {
            try {
                val stripePlan = withContext(Dispatchers.IO) {
                    account.getStripePlan(plan.getId())
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
                    account.createSubscription(params)
                }

                stripeSubResult.value = StripeSubResult(
                        success = sub
                )
            } catch (e: Exception) {
                stripeSubResult.value = StripeSubResult(
                        exception = e
                )
            }
        }
    }

    fun upgradeStripeSub(account: Account, params: StripeSubParams) {
        viewModelScope.launch {
            try {
                val sub = withContext(Dispatchers.IO) {
                    account.upgradeStripeSub(params)
                }

                stripeSubResult.value = StripeSubResult(
                        success = sub
                )
            } catch (e: Exception) {
                stripeSubResult.value = StripeSubResult(
                        exception = e
                )
            }
        }
    }

    fun refreshStripeSub(account: Account) {
        viewModelScope.launch {
            try {
                val stripeSub = withContext(Dispatchers.IO) {
                    account.refreshStripeSub()
                }

                stripeSubResult.value = StripeSubResult(
                        success = stripeSub
                )

            } catch (e: ClientError) {
                if (e.statusCode == 404) {
                    stripeSubResult.value = StripeSubResult(
                            success = null
                    )

                    return@launch
                }

                stripeSubResult.value = StripeSubResult(
                        exception = e
                )
            } catch (e: Exception) {
                stripeSubResult.value = StripeSubResult(
                        exception = e
                )
            }
        }
    }

//    fun createPaymentIntent(account: Account, orderId: String) {
//        viewModelScope.launch {
//            try {
//                val secret = withContext(Dispatchers.IO) {
//                    account.createPaymentIntent(orderId)
//                }
//
//                clientSecretResult.value = StringResult(
//                        success = secret
//                )
//
//            } catch (e: Exception) {
//                clientSecretResult.value = StringResult(
//                        exception = e
//                )
//            }
//        }
//    }
}
