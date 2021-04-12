package com.ft.ftchinese.ui.mobile

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentUpdateMobileBinding
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.viewmodel.ProgressViewModel
import com.ft.ftchinese.viewmodel.Result
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast

/**
 * A simple [Fragment] subclass.
 * Use the [UpdateMobileFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class UpdateMobileFragment : ScopedFragment(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var binding: FragmentUpdateMobileBinding
    private lateinit var viewModel: MobileViewModel
    private lateinit var progressViewModel: ProgressViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        sessionManager = SessionManager.getInstance(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_update_mobile, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = activity?.run {
            ViewModelProvider(this).get(MobileViewModel::class.java)
        } ?: throw Exception("Invalid activity")

        connectionLiveData.observe(viewLifecycleOwner) {
            viewModel.isNetworkAvailable.value = it
        }
        context?.isConnected?.let {
            viewModel.isNetworkAvailable.value = it
        }

        progressViewModel = activity?.run {
            ViewModelProvider(this).get(ProgressViewModel::class.java)
        } ?: throw Exception("Invalid activity")

        binding.viewModel = viewModel
        binding.handler = this
        // Set the binding's lifecycle (otherwise Live Data won't work properly)
        binding.lifecycleOwner = viewLifecycleOwner

        viewModel.counterLiveData.observe(viewLifecycleOwner) {
            binding.requestCode.text = if (it == 0) {
                getString(R.string.mobile_request_code)
            } else {
                getString(R.string.mobile_code_counter, it)
            }
        }

        viewModel.codeSent.observe(viewLifecycleOwner) {
            progressViewModel.off()
            when (it) {
                is Result.LocalizedError -> toast(it.msgId)
                is Result.Error -> it.exception.message?.let { msg -> toast(msg) }
                is Result.Success -> {
                    toast("验证码已发送")
                }
            }
        }

        viewModel.mobileUpdated.observe(viewLifecycleOwner) {
            progressViewModel.off()
            when (it) {
                is Result.LocalizedError -> toast(it.msgId)
                is Result.Error -> it.exception.message?.let { msg -> toast(msg) }
                is Result.Success -> {
                    toast(R.string.prompt_updated)
                    sessionManager
                        .loadAccount()
                        ?.withBaseAccount(it.data)
                        ?.let { account ->
                            sessionManager.saveAccount(account)
                        }
                }
            }
        }
    }

    fun onClickRequestCode(view: View) {
        info("Request code button clicked")
        progressViewModel.on()
        sessionManager.loadAccount()?.let {
            viewModel.requestCode(it)
        }
    }

    fun onSubmitForm(view: View) {
        progressViewModel.on()
        sessionManager.loadAccount()?.let {
            viewModel.updateMobile(it)
        }
    }

    companion object {

        @JvmStatic
        fun newInstance() = UpdateMobileFragment()
    }
}
