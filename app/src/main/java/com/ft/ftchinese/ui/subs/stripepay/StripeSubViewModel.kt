package com.ft.ftchinese.ui.subs.stripepay

import android.app.Application
import android.util.Log
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.model.paywall.CartItemStripe
import com.ft.ftchinese.model.paywall.IntentKind
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.stripesubs.*
import com.ft.ftchinese.repository.StripeClient
import com.ft.ftchinese.tracking.BeginCheckoutParams
import com.ft.ftchinese.tracking.PaySuccessParams
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.base.ToastMessage
import com.ft.ftchinese.viewmodel.StripeViewModel
import com.stripe.android.PaymentSession
import com.stripe.android.PaymentSessionData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "StripeSubViewModel"

sealed class FailureStatus {
    class Message(val message: String) : FailureStatus()
    class NextAction(val secret: String) : FailureStatus()
}

class StripeSubViewModel(application: Application)
    : StripeViewModel(application) {

    private val tracker = StatsTracker.getInstance(application)
    private val idempotency = Idempotency.getInstance(application)

    fun clearIdempotency() {
        idempotency.clear()
    }

    private val paymentSessionInProgress: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    private fun showProgress(): Boolean {
        if (progressLiveData.value == true) {
            return true
        }

        if (paymentSessionInProgress.value == true) {
            return true
        }

        return false
    }

    val inProgress = MediatorLiveData<Boolean>().apply {
        addSource(progressLiveData) {
            value = showProgress()
        }
        addSource(paymentSessionInProgress) {
            value = showProgress()
        }
    }

    val itemLiveData: MutableLiveData<CartItemStripe> by lazy {
        MutableLiveData<CartItemStripe>()
    }

    private val isUpdate: Boolean
        get() = itemLiveData.value?.intent?.kind == IntentKind.Upgrade

    val subsCreated: MutableLiveData<StripeSubs> by lazy {
        MutableLiveData<StripeSubs>()
    }

    val failureLiveData: MutableLiveData<FailureStatus> by lazy {
        MutableLiveData<FailureStatus>()
    }

    fun clearFailureState() {
        failureLiveData.value = null
    }

    val membershipUpdated: MutableLiveData<Membership> by lazy {
        MutableLiveData<Membership>()
    }

    /**
     * When the UI is created, use the price and current
     * membership to build PaymentCounter.
     */
    fun putIntoCart(item: CartItemStripe) {
        Log.i(TAG, "Stripe shopping cart $item")
        itemLiveData.value = item
    }

    fun subscribe(account: Account) {
        if (!ensureNetwork()) {
            return
        }

        if (isUpdate) {
            idempotency.clear()
        }

        val item = itemLiveData.value ?: return
        val params = SubParams(
            priceId = item.recurring.id,
            introductoryPriceId = item.trial?.id,
            defaultPaymentMethod = paymentMethodInUse.value?.current?.id,
            idempotency = idempotency.retrieveKey(),
        )

        when (item.intent.kind) {
            IntentKind.Create -> {
                toastLiveData.value = ToastMessage.Resource(R.string.creating_subscription)
                createSub(account, params)
            }
            IntentKind.Upgrade, IntentKind.Downgrade -> {
                toastLiveData.value = ToastMessage.Resource(R.string.confirm_upgrade)
                updateSub(account, params)
            }
            else -> {
                toastLiveData.value = ToastMessage.Resource(R.string.unknown_order_kind)
                progressLiveData.value = false
            }
        }
        tracker.beginCheckOut(BeginCheckoutParams.ofStripe(item))
    }

    private fun handleSubsResult(result: StripeSubsResult) {
        if (result.subs.paymentIntent?.requiresAction == false) {
            toastLiveData.value = ToastMessage.Resource(R.string.subs_success)
            membershipUpdated.value = result.membership
            subsCreated.value = result.subs

            itemLiveData.value?.let {
                tracker.paySuccess(PaySuccessParams.ofStripe(it))
            }
            return
        }

        if (result.subs.paymentIntent?.clientSecret == null) {
            idempotency.clear()
            failureLiveData.value = FailureStatus.Message("订阅失败！请重试或更换支付方式")
            return
        }

        failureLiveData.value = FailureStatus.NextAction(result.subs.paymentIntent.clientSecret)
    }

    private fun createSub(account: Account, params: SubParams) {
        progressLiveData.value = true
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    StripeClient.createSubscription(account, params)
                }

                progressLiveData.value = false
                if (result == null) {
                    toastLiveData.value = ToastMessage.Resource(R.string.error_unknown)
                    return@launch
                }

                handleSubsResult(result)
            } catch (e: APIError) {
                progressLiveData.value = false
                toastLiveData.value = ToastMessage.fromApi(e)

                itemLiveData.value?.let {
                    tracker.payFailed(it.recurring.edition)
                }
            } catch (e: Exception) {
                progressLiveData.value = false
                toastLiveData.value = ToastMessage.fromException(e)
                itemLiveData.value?.let {
                    tracker.payFailed(it.recurring.edition)
                }
            }
        }
    }

    private fun updateSub(account: Account, params: SubParams) {
        progressLiveData.value = true
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    StripeClient.updateSubs(account, params)
                }

                progressLiveData.value = false
                if (result == null) {
                    toastLiveData.value = ToastMessage.Resource(R.string.error_unknown)
                    return@launch
                }

                handleSubsResult(result)
            } catch (e: Exception) {
                progressLiveData.value = false
                toastLiveData.value = ToastMessage.fromException(e)
                itemLiveData.value?.let {
                    tracker.payFailed(it.recurring.edition)
                }
            }
        }
    }

    val onPaymentSessionData = object : PaymentSession.PaymentSessionListener {
        // Implement a PaymentSessionListener
        override fun onCommunicatingStateChanged(isCommunicating: Boolean) {
            paymentSessionInProgress.value = isCommunicating
        }

        override fun onError(errorCode: Int, errorMessage: String) {
            paymentSessionInProgress.value = false
            toastLiveData.value = ToastMessage.Text(errorMessage)
        }

        override fun onPaymentSessionDataChanged(data: PaymentSessionData) {
            paymentSessionInProgress.value = false
            data.paymentMethod?.let {
                paymentMethodSelected.value = StripePaymentMethod.newInstance(it)
            }
        }
    }
}