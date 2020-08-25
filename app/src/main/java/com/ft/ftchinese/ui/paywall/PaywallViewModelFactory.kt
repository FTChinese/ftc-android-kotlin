package com.ft.ftchinese.ui.paywall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.store.FileCache

class PaywallViewModelFactory (
    private val cache: FileCache
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PaywallViewModel::class.java)) {
            return PaywallViewModel(
                cache
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
