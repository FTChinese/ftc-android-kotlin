package com.ft.ftchinese.ui.validator

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class LiveDataValidator(private val liveData: LiveData<String>) : AnkoLogger {
    private val validationRules = mutableListOf<Predicate>()
    private val errorMessages = mutableListOf<String>()

    var error = MutableLiveData<String?>()

    fun isValid(): Boolean {
        info("Validating live data ${liveData.value}")
        for (i in 0 until validationRules.size) {
            if (validationRules[i](liveData.value)) {
                emitErrorMessage(errorMessages[i])
                return false
            }
        }

        emitErrorMessage(null)
        return true
    }

    private fun emitErrorMessage(messageRes: String?) {
        error.value = messageRes
    }

    fun addRule(errorMsg: String, predicate: Predicate) {
        validationRules.add(predicate)
        errorMessages.add(errorMsg)
    }
}