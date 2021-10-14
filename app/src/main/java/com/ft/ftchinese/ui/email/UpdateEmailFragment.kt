package com.ft.ftchinese.ui.email

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentUpdateEmailBinding
import com.ft.ftchinese.store.AccountCache
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.base.isNetworkConnected
import com.ft.ftchinese.model.fetch.FetchResult
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.support.v4.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class UpdateEmailFragment : ScopedFragment(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var binding: FragmentUpdateEmailBinding
    private lateinit var emailViewModel: EmailViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)

        sessionManager = SessionManager.getInstance(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_update_email,
            container,
            false,
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        emailViewModel = activity?.run {
            ViewModelProvider(this)
                .get(EmailViewModel::class.java)
        } ?: throw Exception("Invalid activity")

        connectionLiveData.observe(viewLifecycleOwner) {
            emailViewModel.isNetworkAvailable.value = it
        }

        activity?.isNetworkConnected()?.let {
            emailViewModel.isNetworkAvailable.value = it
        }

        binding.viewModel = emailViewModel
        binding.handler = this
        binding.lifecycleOwner = viewLifecycleOwner

        setupViewModel()

        // Show user current email.
        // For mobile-created account, treat the email as null.
        binding.currentEmail = AccountCache.get()?.let {
            if (it.isMobileEmail) {
                null
            } else {
                it.email
            }
        }

        binding.emailInput.requestFocus()
    }

    private fun setupViewModel() {
        emailViewModel.emailUpdated.observe(viewLifecycleOwner) {
            when (it) {
                is FetchResult.LocalizedError -> alertErrorMsg(it.msgId)
                is FetchResult.Error -> it.exception.message?.let { msg -> toast(msg) }
                is FetchResult.Success -> {
                    toast(R.string.prompt_updated)
                    sessionManager
                        .loadAccount()
                        ?.withBaseAccount(it.data)
                        ?.let { account ->
                            sessionManager.saveAccount(account)
                        }

                    // Refresh UI.
                    binding.currentEmail = it.data.email
                }
            }
        }
    }

    private fun alertErrorMsg(id: Int) {
        AlertDialog.Builder(requireContext())
            .setMessage(id)
            .setPositiveButton(R.string.action_ok) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    fun onSubmit(view: View) {
        sessionManager.loadAccount()?.let {
            emailViewModel.updateEmail(it.id)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = UpdateEmailFragment()
    }
}
