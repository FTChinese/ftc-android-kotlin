package com.ft.ftchinese.ui.account.address

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
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.model.fetch.FetchResult
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.support.v4.toast

/**
 * A simple [Fragment] subclass.
 * Use the [UpdateAddressFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class UpdateAddressFragment : ScopedFragment(), AnkoLogger {

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
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_update_address,
            container,
            false,
        )
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

        binding.viewModel = viewModel
        binding.handler = this
        binding.lifecycleOwner = viewLifecycleOwner

        viewModel.addressRetrieved.observe(viewLifecycleOwner) {
            when (it) {
                is FetchResult.LocalizedError -> toast(it.msgId)
                is FetchResult.Error -> it.exception.message?.let { msg -> toast(msg) }
                is FetchResult.Success -> {
                }
            }
        }

        viewModel.addressUpdated.observe(viewLifecycleOwner) {
            when (it) {
                is FetchResult.LocalizedError -> {
                    toast(it.msgId)
                }
                is FetchResult.Error -> {
                    it.exception.message?.let { msg -> toast(msg) }
                }
                is FetchResult.Success -> toast(R.string.prompt_saved)
            }
        }

        sessionManager.loadAccount()?.let {
            viewModel.loadAddress(it)
        }
    }

    fun onSubmitForm(view: View) {
        sessionManager.loadAccount()?.let {
            viewModel.updateAddress(it)
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *UpdateAddressFragment.
         */
        @JvmStatic
        fun newInstance() = UpdateAddressFragment()
    }
}
