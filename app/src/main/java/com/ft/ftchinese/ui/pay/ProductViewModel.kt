package com.ft.ftchinese.ui.pay

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel;
import com.ft.ftchinese.models.Plan

/**
 * Used by [ProductFragment] to pass information to host
 * activity which product is selected.
 */
class ProductViewModel : ViewModel() {

    val selected = MutableLiveData<Plan>()
    val input = MutableLiveData<Boolean>()

    fun select(plan: Plan) {
        selected.value = plan
    }

    fun enableInput(enable: Boolean) {
        input.value = enable
    }
}
