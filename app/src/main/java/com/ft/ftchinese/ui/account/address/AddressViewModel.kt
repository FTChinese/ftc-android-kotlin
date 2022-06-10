package com.ft.ftchinese.ui.account.address

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ft.ftchinese.model.reader.Address
import com.ft.ftchinese.ui.validator.LiveDataValidator
import com.ft.ftchinese.ui.validator.LiveDataValidatorResolver
import com.ft.ftchinese.ui.validator.Validator

class AddressViewModel : ViewModel() {

    val progressLiveData = MutableLiveData<Boolean>()
    private var current = Address()
    private val updated: Address
        get() = Address(
            province = provinceLiveData.value,
            city = cityLiveData.value,
            district = districtLiveData.value,
            street = streetLiveData.value,
            postcode = postCodeLiveData.value
        )

    val provinceLiveData = MutableLiveData("")
    val provinceValidator = LiveDataValidator(provinceLiveData).apply {
        addRule("", Validator::notEmpty)
        addRule("输入内容过长", Validator.maxLength(8))
    }

    val cityLiveData = MutableLiveData("")
    val cityValidator = LiveDataValidator(cityLiveData).apply {
        addRule("", Validator::notEmpty)
        addRule("输入内容过长", Validator.maxLength(16))
    }

    val districtLiveData = MutableLiveData("")
    val districtValidator = LiveDataValidator(districtLiveData).apply {
        addRule("", Validator::notEmpty)
        addRule("输入内容过长", Validator.maxLength(16))
    }

    val streetLiveData = MutableLiveData("")
    val streetValidator = LiveDataValidator(streetLiveData).apply {
        addRule("", Validator::notEmpty)
        addRule("输入内容过长",Validator.maxLength(128))
    }

    val postCodeLiveData = MutableLiveData("")
    val postCodeValidator = LiveDataValidator(postCodeLiveData).apply {
        addRule("", Validator::notEmpty)
        addRule("输入内容过长", Validator.maxLength(16))
    }

    private val formValidator = LiveDataValidatorResolver(listOf(
        provinceValidator,
        cityValidator,
        districtValidator,
        streetValidator,
        postCodeValidator,
    ))

    val isFormValid = MediatorLiveData<Boolean>().apply {
        addSource(provinceLiveData) {
            value = enableForm()
        }
        addSource(cityLiveData) {
            value = enableForm()
        }
        addSource(districtLiveData) {
            value = enableForm()
        }
        addSource(streetLiveData) {
            value = enableForm()
        }
        addSource(postCodeLiveData) {
            value = enableForm()
        }
        addSource(progressLiveData) {
            value = enableForm()
        }
    }

    private fun enableForm(): Boolean {
        if (progressLiveData.value == true) {
            return false
        }

        return formValidator.isValid()
    }

    init {
        progressLiveData.value = false
        isFormValid.value = false
    }

    private fun hasChanged(): Boolean {
        return current.province != provinceLiveData.value
            || current.city != cityLiveData.value
            || current.district != districtLiveData.value
            || current.street != streetLiveData.value
            || current.postcode != postCodeLiveData.value
    }

}
