package com.ft.ftchinese.ui.account.name

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentUpdateUsernameBinding
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.store.AccountCache
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.base.isConnected
import org.jetbrains.anko.support.v4.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class UpdateNameFragment : ScopedFragment() {
    private lateinit var sessionManager: SessionManager
    private lateinit var binding: FragmentUpdateUsernameBinding
    private lateinit var viewModel: NameViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        sessionManager = SessionManager.getInstance(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_update_username,
            container,
            false,
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = activity?.run {
            ViewModelProvider(this)
                .get(NameViewModel::class.java)
        } ?: throw Exception("Invalid activity")

        connectionLiveData.observe(viewLifecycleOwner) {
            viewModel.isNetworkAvailable.value = it
        }
        context?.isConnected?.let {
            viewModel.isNetworkAvailable.value = it
        }

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.handler = this

        setupViewModel()
        initUI()
    }

    private fun setupViewModel() {
        viewModel.nameUpdated.observe(viewLifecycleOwner) {
            when (it) {
                is FetchResult.LocalizedError -> toast(it.msgId)
                is FetchResult.TextError -> toast(it.text)
                is FetchResult.Success -> {
                    toast(R.string.refresh_success)
                    sessionManager
                        .loadAccount()
                        ?.withBaseAccount(it.data)
                        ?.let { account ->
                            sessionManager.saveAccount(account)
                        }

                    // Refresh UI.
                    binding.userName = it.data.userName
                }
            }
        }
    }

    private fun initUI() {
        binding.userName = AccountCache.get()?.userName
        binding.userNameInput.requestFocus()
    }

    fun onSubmit(view: View) {
        sessionManager.loadAccount()?.let {
            viewModel.update(it.id)
        }
    }
    companion object {

        @JvmStatic
        fun newInstance() = UpdateNameFragment()
    }
}
