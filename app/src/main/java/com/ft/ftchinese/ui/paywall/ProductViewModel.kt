package com.ft.ftchinese.ui.paywall

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ft.ftchinese.model.subscription.Plan
import org.jetbrains.anko.AnkoLogger

/**
 * Used by ProductFragment to pass information to host
 * activity which product is selected.
 */
class ProductViewModel : ViewModel(), AnkoLogger {

    val selected: MutableLiveData<Plan> by lazy {
        MutableLiveData<Plan>()
    }

    val inputEnabled: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    // When host activity retrieved paywall data, pass products to ProductFragment.
    val plansReceived: MutableLiveData<List<Plan>> by lazy {
        MutableLiveData<List<Plan>>()
    }

    fun setPlans(plans: List<Plan>) {
        plansReceived.value = plans
    }
}
