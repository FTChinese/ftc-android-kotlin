package com.ft.ftchinese.ui.validator

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.jetbrains.anko.AnkoLogger

class LiveDataValidator(private val liveData: LiveData<String>) : AnkoLogger {
    private val validationRules = mutableListOf<Predicate>()
    private val errorMessages = mutableListOf<String>()

    var error = MutableLiveData<String?>()

    fun isValid(): Boolean {
        for (i in 0 until validationRules.size) {
            if (!validationRules[i](liveData.value)) {
                emitErrorMessage(errorMessages[i])
                return false
            }
        }

        emitErrorMessage(null)
        return true
    }

    fun isDirty(): Boolean {
        return !liveData.value?.trim().isNullOrBlank()
    }

    private fun emitErrorMessage(messageRes: String?) {
        error.value = messageRes
    }

    fun addRule(errorMsg: String, predicate: Predicate) {
        validationRules.add(predicate)
        errorMessages.add(errorMsg)
    }
}
