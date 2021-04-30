package com.ft.ftchinese.ui.address

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Address
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.ui.validator.LiveDataValidator
import com.ft.ftchinese.ui.validator.LiveDataValidatorResolver
import com.ft.ftchinese.ui.validator.Validator
import com.ft.ftchinese.viewmodel.Result
import com.ft.ftchinese.viewmodel.parseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class AddressViewModel : ViewModel(), AnkoLogger {
    val progressLiveData = MutableLiveData<Boolean>()
    val isNetworkAvailable: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

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
        info("should enable form")
        if (progressLiveData.value == true) {
            return false
        }

        info("Current address $current, updated address $updated")
        return formValidator.isValid()
    }

    init {
        progressLiveData.value = false
        isFormValid.value = false
    }

    val addressRetrieved: MutableLiveData<Result<Address>> by lazy {
        MutableLiveData<Result<Address>>()
    }

    fun loadAddress(account: Account) {
        if (isNetworkAvailable.value != true) {
            addressRetrieved.value = Result.LocalizedError(R.string.prompt_no_network)
            return
        }

        progressLiveData.value = true

        viewModelScope.launch {
            try {
                val address = withContext(Dispatchers.IO) {
                    AccountRepo.loadAddress(account.id)
                } ?: return@launch

                current = address

                provinceLiveData.value = address.province
                cityLiveData.value = address.city
                districtLiveData.value = address.district
                streetLiveData.value = address.street
                postCodeLiveData.value = address.postcode

                progressLiveData.value = false

                addressRetrieved.value = Result.Success(address)
            } catch (e: Exception) {
                addressRetrieved.value = parseException(e)

                progressLiveData.value = false
            }
        }
    }

    val addressUpdated: MutableLiveData<Result<Boolean>> by lazy {
        MutableLiveData<Result<Boolean>>()
    }

    private fun hasChanged(): Boolean {
        return current.province != provinceLiveData.value
            || current.city != cityLiveData.value
            || current.district != districtLiveData.value
            || current.street != streetLiveData.value
            || current.postcode != postCodeLiveData.value
    }

    fun updateAddress(account: Account) {
        info("Start updating address $updated")

        if (!hasChanged()) {
            info("Address not changed.")
            addressUpdated.value = Result.Success(true)
            return
        }

        if (isNetworkAvailable.value != true) {
            addressUpdated.value = Result.LocalizedError(R.string.prompt_no_network)
            return
        }

        progressLiveData.value = true

        viewModelScope.launch {
            try {
                val ok = withContext(Dispatchers.IO) {
                    AccountRepo.updateAddress(account.id, updated)
                }

                addressUpdated.value = Result.Success(ok)
                progressLiveData.value = false
            } catch (e: Exception) {
                addressUpdated.value = parseException(e)
                progressLiveData.value = false
            }
        }
    }
}
