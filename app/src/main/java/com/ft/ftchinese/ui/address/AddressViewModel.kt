package com.ft.ftchinese.ui.address

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Address
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.ui.validator.Validator
import com.ft.ftchinese.viewmodel.Result
import com.ft.ftchinese.viewmodel.parseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class AddressViewModel : ViewModel(), AnkoLogger {

    val inProgress: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    private var previous = Address()
    private var updated = Address()

    val isNetworkAvailable: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    val formState: MutableLiveData<AddressFormState> by lazy {
        MutableLiveData<AddressFormState>()
    }

    fun changeProvince(p: String) {
        val err = Validator.ensureLength(p, 0, 8)
        if (err != null) {
            formState.value = AddressFormState(
                error = err,
                field = AddressField.Province,
                status = FormStatus.Invalid,
            )
            return
        }

        updated = updated.withProvince(p)
        ensureSavable()
    }

    fun changeCity(c: String) {
        // The longest one: 克孜勒苏柯尔克孜自治州
        val err = Validator.ensureLength(c, 0, 16)
        if (err != null) {
            formState.value = AddressFormState(
                error = err,
                field = AddressField.City,
                status = FormStatus.Invalid,
            )
            return
        }

        updated = updated.withCity(c)
        ensureSavable()
    }

    fun changeDistrict(d: String) {
        val err = Validator.ensureLength(d, 0, 16)
        if (err != null) {
            formState.value = AddressFormState(
                error = err,
                field = AddressField.District,
                status = FormStatus.Invalid,
            )
            return
        }

        updated = updated.withDistrict(d)
        ensureSavable()
    }

    fun changeStreet(s: String) {
        val err = Validator.ensureLength(s, 0, 512)
        if (err != null) {
            formState.value = AddressFormState(
                error = err,
                field = AddressField.Street,
                status = FormStatus.Invalid,
            )
            return
        }

        updated = updated.withStreet(s)
        ensureSavable()
    }

    fun changePostcode(pc: String) {
        val err = Validator.ensureLength(pc, 0, 16)
        if (err != null) {
            formState.value = AddressFormState(
                error = err,
                field = AddressField.Postcode,
                status = FormStatus.Invalid,
            )
            return
        }

        updated = updated.withPostcode(pc)
        ensureSavable()
    }

    private fun ensureSavable() {
        info("Current address: $previous")
        info("Updated address: $updated")
        if (updated == previous) {
            formState.value = AddressFormState()
            return
        }

        if (updated.province.isNullOrBlank() || updated.city.isNullOrBlank() || updated.district.isNullOrBlank() || updated.street.isNullOrBlank() || updated.postcode.isNullOrBlank()) {
            formState.value = AddressFormState(
                error = null,
                field = null,
                status = FormStatus.Invalid
            )

            return
        }

        formState.value = AddressFormState(
            error = null,
            field = null,
            status = FormStatus.Changed,
        )
    }

    val addressRetrieved: MutableLiveData<Result<Address>> by lazy {
        MutableLiveData<Result<Address>>()
    }

    val addressUpdated: MutableLiveData<Result<Boolean>> by lazy {
        MutableLiveData<Result<Boolean>>()
    }

    fun loadAddress(account: Account) {
        if (isNetworkAvailable.value != true) {
            addressRetrieved.value = Result.LocalizedError(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val address = withContext(Dispatchers.IO) {
                    AccountRepo.loadAddress(account.id)
                } ?: return@launch

                previous = address

                addressRetrieved.value = Result.Success(address)
            } catch (e: Exception) {
                addressRetrieved.value = parseException(e)
            }
        }
    }

    fun updateAddress(account: Account) {
        if (isNetworkAvailable.value != true) {
            addressRetrieved.value = Result.LocalizedError(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val ok = withContext(Dispatchers.IO) {
                    AccountRepo.updateAddress(account.id, updated)
                }

                addressUpdated.value = Result.Success(ok)
            } catch (e: Exception) {
                addressUpdated.value = parseException(e)
            }
        }
    }
}
