package com.ft.ftchinese.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.store.FileCache

class SettingsViewModelFactory(
    private val cache: FileCache
): ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(
                cache,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
