package com.ft.ftchinese.ui.pay

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.Account
import com.ft.ftchinese.model.order.Plan
import com.ft.ftchinese.ui.StringResult
import com.ft.ftchinese.util.ClientError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CheckOutViewModel : ViewModel() {


    val inputEnabled = MutableLiveData<Boolean>()
    val wxOrderResult = MutableLiveData<WxOrderResult>()
    val aliOrderResult = MutableLiveData<AliOrderResult>()
    val clientSecretResult = MutableLiveData<StringResult>()
    val stripePlanResult = MutableLiveData<StripePlanResult>()

    val upgradePreviewResult = MutableLiveData<UpgradePreviewResult>()
    val directUpgradeResult = MutableLiveData<UpgradeResult>()

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

    fun directUpgrade(account: Account) {
        viewModelScope.launch {
            try {
                val (ok, plan) = withContext(Dispatchers.IO) {
                    account.directUpgrade()
                }

                if (ok) {
                    directUpgradeResult.value = UpgradeResult(
                            success = true
                    )
                    return@launch
                }

                directUpgradeResult.value = UpgradeResult(
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

                directUpgradeResult.value = UpgradeResult(
                        error = msgId,
                        exception = e
                )
            } catch (e: Exception) {
                directUpgradeResult.value = UpgradeResult(
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

    fun createPaymentIntent(account: Account, orderId: String) {
        viewModelScope.launch {
            try {
                val secret = withContext(Dispatchers.IO) {
                    account.createPaymentIntent(orderId)
                }

                clientSecretResult.value = StringResult(
                        success = secret
                )

            } catch (e: Exception) {
                clientSecretResult.value = StringResult(
                        exception = e
                )
            }
        }
    }
}
