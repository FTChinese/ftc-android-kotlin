package com.ft.ftchinese.ui.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.store.FileCache

class CustomerViewModelFactory(
    private val cache: FileCache,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CustomerViewModel::class.java)) {
            return CustomerViewModel(
                cache,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
