package com.ft.ftchinese.ui.login

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentMobileAuthBinding
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.store.TokenManager
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.ui.data.FetchResult
import com.ft.ftchinese.ui.mobile.MobileViewModel
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
    private lateinit var sessionManager: SessionManager
    private lateinit var tokenManager: TokenManager

    private lateinit var mobileViewModel: MobileViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        sessionManager = SessionManager.getInstance(context)
        tokenManager = TokenManager.getInstance(context)
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

        // MobileViewModel is created under parent scope
        // since it needs to share data with SignInFragment.
        mobileViewModel = activity?.run {
            ViewModelProvider(this)
                .get(MobileViewModel::class.java)
        } ?: throw Exception("Invalid activity")

        connectionLiveData.observe(viewLifecycleOwner) {
            mobileViewModel.isNetworkAvailable.value = it
        }
        context?.isConnected?.let {
            mobileViewModel.isNetworkAvailable.value = it
        }

        binding.viewModel = mobileViewModel
        binding.handler = this
        binding.lifecycleOwner = viewLifecycleOwner

        setupViewModel()
    }

    private fun setupViewModel() {

        mobileViewModel.progressLiveData.observe(viewLifecycleOwner) {
            binding.inProgress = it
        }

        mobileViewModel.counterLiveData.observe(viewLifecycleOwner) {
            binding.requestCode.text = if (it == 0) {
                getString(R.string.mobile_request_code)
            } else {
                getString(R.string.mobile_code_counter, it)
            }
        }

        mobileViewModel.codeSent.observe(viewLifecycleOwner) {
            when (it) {
                is FetchResult.LocalizedError -> toast(it.msgId)
                is FetchResult.Error -> it.exception.message?.let { msg -> toast(msg) }
                is FetchResult.Success -> {
                    toast("验证码已发送")
                }
            }
        }

        // After user submitted the mobile number and the SMS
        // sent to that device, what we should to depends on
        // the result returned.
        // If user does not exist, start ui to ask for email
        // We need to share the mobile number to SignInFragment.
        mobileViewModel.mobileNotSet.observe(viewLifecycleOwner) {
            SignInFragment
                .forMobileLink()
                .show(childFragmentManager, "MobileLinkEmail")
        }

        // If the mobile is already linked to an email account.
        mobileViewModel.accountLoaded.observe(viewLifecycleOwner) {
            when (it) {
                is FetchResult.LocalizedError -> toast(it.msgId)
                is FetchResult.Error -> it.exception.message?.let { msg -> toast(msg) }
                is FetchResult.Success -> {

                    sessionManager.saveAccount(it.data)
                    context?.let { ctx ->
                        StatsTracker
                            .getInstance(ctx)
                            .setUserId(it.data.id)
                    }

                    activity?.setResult(Activity.RESULT_OK)
                    activity?.finish()
                }
            }
        }
    }

    fun onClickRequestCode(view: View) {
        info("Request code button clicked")
        mobileViewModel.requestSMSAuthCode()
    }

    // The result is handled in mobileAuthenticated observer.
    fun onSubmitForm(view: View) {
        mobileViewModel.verifySMSAuthCode(tokenManager.getToken())
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
