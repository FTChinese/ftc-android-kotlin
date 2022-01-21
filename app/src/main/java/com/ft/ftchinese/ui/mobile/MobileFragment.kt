package com.ft.ftchinese.ui.mobile

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentMobileBinding
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.store.TokenManager
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.ui.dialog.AlertDialogFragment
import com.ft.ftchinese.ui.dialog.DialogArgs
import com.ft.ftchinese.ui.login.SignInFragment
import org.jetbrains.anko.support.v4.toast

/**
 * Hosted inside [com.ft.ftchinese.ui.login.AuthActivity]
 * or [com.ft.ftchinese.ui.account.UpdateActivity] depending its usage.
 * Flow chain of UI when used for authorization:
 * If account could be fetched, the hosting activity will be destroyed.
 * If account could not be fetched, show a dialog to give user option:
 * 1. Use the mobile number to create an account directly;
 * 2. Link to existing email account by going to
 * [com.ft.ftchinese.ui.login.SignInFragment] and then go to
 * [com.ft.ftchinese.ui.login.SignUpFragment].
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class MobileFragment : ScopedFragment() {

    private lateinit var sessionManager: SessionManager
    private lateinit var binding: FragmentMobileBinding
    private lateinit var viewModel: MobileViewModel

    // Tells where it is used.
    private var usage: Int? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        sessionManager = SessionManager.getInstance(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            usage = it.getInt(ARG_USAGE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_mobile,
            container,
            false)
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
        activity?.isConnected?.let {
            viewModel.isNetworkAvailable.value = it
        }

        binding.viewModel = viewModel
        binding.handler = this
        // Set the binding's lifecycle (otherwise Live Data won't work properly)
        binding.lifecycleOwner = viewLifecycleOwner

        setupViewModel()

        binding.btnName = when (usage) {
            USAGE_AUTH -> getString(R.string.btn_login)
            USAGE_UPDATE -> getString(R.string.btn_save)
            else -> ""
        }
        binding.mobileInput.requestFocus()
    }

    private fun setupViewModel() {
        viewModel.counterLiveData.observe(viewLifecycleOwner) {
            binding.requestCode.text = if (it == 0) {
                getString(R.string.mobile_request_code)
            } else {
                getString(R.string.mobile_code_counter, it)
            }
        }

        viewModel.codeSent.observe(viewLifecycleOwner) {
            when (it) {
                is FetchResult.LocalizedError -> onSendCodeError(it.msgId)
                is FetchResult.Error -> it.exception.message?.let { msg -> toast(msg) }
                is FetchResult.Success -> {
                    toast("验证码已发送")
                }
            }
        }

        // In case we find out the mobile is not linked to any existing ftc account.
        viewModel.mobileNotSet.observe(viewLifecycleOwner) {
            AlertDialogFragment.newInstance(DialogArgs(
                message = getString(R.string.mobile_login_dialog_message),
                positiveButton = R.string.mobile_login_dialog_positive_buton,
                negativeButton = R.string.mobile_login_dialog_negative_button,
                title = R.string.mobile_login_dialog_title,
            ))
                .onPositiveButtonClicked { dialog, _ ->
                    dialog.dismiss()

                    SignInFragment
                        .forMobileLink()
                        .show(
                            childFragmentManager,
                            "MobileLinkEmail",
                        )
                }
                .onNegativeButtonClicked { dialog, _ ->
                    dialog.cancel()
                    viewModel.signUp(
                        TokenManager
                            .getInstance(requireContext())
                            .getToken()
                    )
                }
                .show(childFragmentManager, "MobileInitialLogin")
        }

        // Used for login.
        viewModel.accountLoaded.observe(viewLifecycleOwner) {
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

        // Used for updating mobile.
        viewModel.mobileUpdated.observe(viewLifecycleOwner) {
            when (it) {
                is FetchResult.LocalizedError -> onUpdateError(it.msgId)
                is FetchResult.Error -> it.exception.message?.let { msg -> toast(msg) }
                is FetchResult.Success -> {
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

    private fun onSendCodeError(msgId: Int) {
        if (msgId == R.string.mobile_conflict) {
            AlertDialogFragment
                .newMsgInstance(getString(msgId))
                .show(childFragmentManager, "AlertMobileCodeError")
        } else {
            toast(msgId)
        }
    }

    private fun onUpdateError(msgId: Int) {
        when (msgId) {
            R.string.mobile_code_not_found,
            R.string.mobile_already_exists -> {
                AlertDialogFragment
                    .newMsgInstance(getString(msgId))
                    .show(childFragmentManager, "AlertUpdateMobileError")
            }
            else -> toast(msgId)
        }
    }

    fun onClickRequestCode(view: View) {
        Log.i(TAG, "Request code button clicked")

        when (usage) {
            USAGE_AUTH -> {
                viewModel.requestSMSAuthCode()
            }
            USAGE_UPDATE -> {
                sessionManager
                    .loadAccount()
                    ?.let {
                        viewModel.requestCodeForUpdate(it)
                    }
            }
        }
    }

    // Send request to different endpoints depending on how this view is used.
    fun onSubmitForm(view: View) {
        when (usage) {
            // For authorization, possibilities depending on the request result:
            // If user id returned, fetch account data directly;
            // If user id not found, user could:
            // * signup with the mobile number as an email
            // * signup with a new email
            // * link an existing email.
            USAGE_AUTH -> {
                context?.let {
                    viewModel.verifySMSAuthCode(
                        TokenManager
                            .getInstance(it)
                            .getToken()
                    )
                }
            }
            USAGE_UPDATE -> {
                sessionManager.loadAccount()?.let {
                    viewModel.updateMobile(it)
                }
            }
        }
    }

    companion object {
        private const val ARG_USAGE = "arg_usage"
        private const val USAGE_AUTH = 1
        private const val USAGE_UPDATE = 2

        private const val TAG = "MobileFragment"

        @JvmStatic
        fun newInstanceForAuth() = MobileFragment().apply {
            arguments = bundleOf(
                ARG_USAGE to USAGE_AUTH
            )
        }

        @JvmStatic
        fun newInstanceForUpdate() = MobileFragment().apply {
            arguments = bundleOf(
                ARG_USAGE to USAGE_UPDATE
            )
        }
    }
}
