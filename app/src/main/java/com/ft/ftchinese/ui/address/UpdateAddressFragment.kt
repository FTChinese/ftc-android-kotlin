package com.ft.ftchinese.ui.address

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentUpdateAddressBinding
import com.ft.ftchinese.model.reader.Address
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.base.afterTextChanged
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.viewmodel.Result
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.toast

/**
 * A simple [Fragment] subclass.
 * Use the [UpdateAddressFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class UpdateAddressFragment : ScopedFragment() {

    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: AddressViewModel
    private lateinit var binding: FragmentUpdateAddressBinding

    override fun onAttach(context: Context) {
        super.onAttach(context)

        sessionManager = SessionManager.getInstance(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_update_address, container, false)
        binding.address = Address()
        binding.inputEnabled = true
        binding.btnEnabled = false
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = activity?.run {
            ViewModelProvider(this)
                .get(AddressViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        connectionLiveData.observe(viewLifecycleOwner) {
            viewModel.isNetworkAvailable.value = it
        }
        context?.isConnected?.let {
            viewModel.isNetworkAvailable.value = it
        }

        sessionManager.loadAccount()?.let {
            viewModel.loadAddress(it)
        }

        viewModel.addressRetrieved.observe(viewLifecycleOwner) {
            binding.inputEnabled = true

            when (it) {
                is Result.LocalizedError -> toast(it.msgId)
                is Result.Error -> it.exception.message?.let { msg -> toast(msg) }
                is Result.Success -> {
                    binding.address = it.data
                }
            }
        }

        viewModel.formState.observe(viewLifecycleOwner) {
            when (it.status) {
                FormStatus.Intact, FormStatus.Invalid -> {
                    binding.btnEnabled = false
                    val control = when (it.field) {
                        AddressField.Province -> binding.inputProvince
                        AddressField.City -> binding.inputCity
                        AddressField.District -> binding.inputDistrict
                        AddressField.Street -> binding.inputStreet
                        AddressField.Postcode -> binding.inputPostcode
                        else -> null
                    }

                    if (control == null) {
                        it.error?.let { msg -> toast(msg) }
                        return@observe
                    }

                    it.error?.let { msg ->
                        control.apply {
                            error = getString(msg)
                            requestFocus()
                        }
                    }
                }
                FormStatus.Changed -> {
                    binding.btnEnabled = true
                }
            }
        }

        binding.inputProvince.afterTextChanged {
            viewModel.changeProvince(it)
        }

        binding.inputCity.afterTextChanged {
            viewModel.changeCity(it)
        }

        binding.inputDistrict.afterTextChanged {
            viewModel.changeDistrict(it)
        }

        binding.inputStreet.afterTextChanged {
            viewModel.changeStreet(it)
        }

        binding.inputPostcode.afterTextChanged {
            viewModel.changePostcode(it)
        }

        binding.btnSave.onClick {
            sessionManager.loadAccount()?.let {
                viewModel.inProgress.value = true
                binding.inputEnabled = false
                binding.btnEnabled = false

                viewModel.updateAddress(it)
            }
        }

        viewModel.addressUpdated.observe(viewLifecycleOwner) {
            viewModel.inProgress.value = false
            when (it) {
                is Result.LocalizedError -> {
                    binding.inputEnabled = true
                    binding.btnEnabled = true
                }
                is Result.Error -> {
                    it.exception.message?.let { msg -> toast(msg) }
                    binding.inputEnabled = true
                    binding.btnEnabled = true
                }
                is Result.Success -> toast(R.string.prompt_saved)
            }
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *UpdateAddressFragment.
         */
        @JvmStatic
        fun newInstance() =
            UpdateAddressFragment()
    }
}
