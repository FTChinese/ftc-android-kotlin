package com.ft.ftchinese.ui.login

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentMobileAuthBinding
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.ui.mobile.MobileViewModel
import com.ft.ftchinese.viewmodel.Result
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast

/**
 * A simple [Fragment] subclass.
 * Use the [MobileAuthFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class MobileAuthFragment : ScopedFragment(), AnkoLogger {

    private lateinit var binding: FragmentMobileAuthBinding
    private lateinit var viewModel: MobileViewModel
    private lateinit var sessionManager: SessionManager

    override fun onAttach(context: Context) {
        super.onAttach(context)
        sessionManager = SessionManager.getInstance(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_mobile_auth,
            container,
            false,
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = activity?.run {
            ViewModelProvider(this)
                .get(MobileViewModel::class.java)
        } ?: throw Exception("Invalid activity")

        connectionLiveData.observe(viewLifecycleOwner) {
            viewModel.isNetworkAvailable.value = it
        }
        context?.isConnected?.let {
            viewModel.isNetworkAvailable.value = it
        }

        binding.viewModel = viewModel
        binding.handler = this
        binding.lifecycleOwner = viewLifecycleOwner

        setupViewModel()
    }

    private fun setupViewModel() {
        viewModel.mobileValidator.error.observe(viewLifecycleOwner) {
            info(it)
        }

        viewModel.counterLiveData.observe(viewLifecycleOwner) {
            binding.requestCode.text = if (it == 0) {
                getString(R.string.mobile_request_code)
            } else {
                getString(R.string.mobile_code_counter, it)
            }
        }

        viewModel.codeSent.observe(viewLifecycleOwner) {
            when (it) {
                is Result.LocalizedError -> toast(it.msgId)
                is Result.Error -> it.exception.message?.let { msg -> toast(msg) }
                is Result.Success -> {
                    toast("验证码已发送")
                }
            }
        }
    }

    fun onClickRequestCode(view: View) {
        info("Request code button clicked")
        sessionManager.loadAccount()?.let {
            viewModel.requestCodeForUpdate(it)
        }
    }

    fun onSubmitForm(view: View) {
        sessionManager.loadAccount()?.let {
            viewModel.updateMobile(it)
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         */
        @JvmStatic
        fun newInstance() = MobileAuthFragment()
    }
}
