package com.ft.ftchinese.ui.pay

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel;
import com.ft.ftchinese.model.subscription.Plan

/**
 * Used by [ProductFragment] to pass information to host
 * activity which product is selected.
 */
class ProductViewModel : ViewModel() {

    val selected: MutableLiveData<Plan> by lazy {
        MutableLiveData<Plan>()
    }

    val inputEnabled: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }
}
