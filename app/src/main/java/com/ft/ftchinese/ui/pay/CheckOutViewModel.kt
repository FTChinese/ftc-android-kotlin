package com.ft.ftchinese.ui.pay

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.Account
import com.ft.ftchinese.model.order.PlanPayable
import com.ft.ftchinese.ui.StringResult
import com.ft.ftchinese.util.ClientError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CheckOutViewModel : ViewModel() {

    val directUpgradeResult = MutableLiveData<UpgradeResult>()
    val inputEnabled = MutableLiveData<Boolean>()
    val customerCreated = MutableLiveData<CustomerResult>()
    val wxOrderResult = MutableLiveData<WxOrderResult>()
    val aliOrderResult = MutableLiveData<AliOrderResult>()
    val stripeOrderResult = MutableLiveData<OrderResult>()
    val clientSecretResult = MutableLiveData<StringResult>()

    // Enable/Disable a UI, like button.
    fun enableInput(v: Boolean) {
        inputEnabled.value = v
    }

    fun createCustomer(account: Account) {
        viewModelScope.launch {
            try {
                val id = withContext(Dispatchers.IO) {
                    account.createCustomer()
                }

                if (id == null) {
                    customerCreated.value = CustomerResult(
                            error = R.string.stripe_customer_not_created
                    )
                    return@launch
                }

                customerCreated.value = CustomerResult(
                        success = id
                )
            } catch (e: ClientError) {
                customerCreated.value = CustomerResult(
                        exception = e
                )
            } catch (e: Exception) {
                customerCreated.value = CustomerResult(
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
                        plan = plan
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

    fun createWxOrder(account: Account, plan: PlanPayable) {
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

    fun createAliOrder(account: Account, plan: PlanPayable) {
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

    fun createStripeOrder(account: Account, plan: PlanPayable) {
        viewModelScope.launch {
            try {
                val order = withContext(Dispatchers.IO) {
                    account.createStripeOrder(plan.tier, plan.cycle)
                }

                stripeOrderResult.value = OrderResult(
                        success = order
                )
            } catch (e: ClientError) {
                val msgId = if (e.statusCode == 403) {
                    R.string.renewal_not_allowed
                } else {
                    e.statusMessage()
                }

                stripeOrderResult.value = OrderResult(
                        error = msgId,
                        exception = e
                )
            } catch (e: Exception) {
                stripeOrderResult.value = OrderResult(
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
