package com.ft.ftchinese.ui.account

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentUpdateEmailBinding
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.ui.base.*
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.data.FetchResult
import com.ft.ftchinese.viewmodel.UpdateViewModel
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.support.v4.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class UpdateEmailFragment : ScopedFragment(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var updateViewModel: UpdateViewModel
    private lateinit var binding: FragmentUpdateEmailBinding
    private var account: Account? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        sessionManager = SessionManager.getInstance(context)
        account = sessionManager.loadAccount()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_update_email, container, false)
        binding.currentEmail = account?.email
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        updateViewModel = activity?.run {
            ViewModelProvider(this)
                    .get(UpdateViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        // Validate form data.
        // Note this only works if user actually entered something.
        // If user does not enter anything, and clicked save button,
        // empty string will be submitted directly.
        updateViewModel.updateFormState.observe(viewLifecycleOwner, Observer {
            val updateState = it ?: return@Observer

            binding.btnSave.isEnabled = updateState.isDataValid

            if (updateState.emailError != null) {
                binding.emailInput.error = getString(updateState.emailError)
                binding.emailInput.requestFocus()
            }
        })

        binding.emailInput.afterTextChanged {
            updateViewModel.emailDataChanged(
                    currentEmail = account?.email ?: "",
                    newEmail = binding.emailInput.text.toString().trim()
            )
        }

        binding.btnSave.setOnClickListener {
            updateViewModel.emailDataChanged(
                    currentEmail = account?.email ?: "",
                    newEmail = binding.emailInput.text.toString().trim()
            )

            if (context?.isConnected != true) {
                toast(R.string.prompt_no_network)

                return@setOnClickListener
            }

            val userId = sessionManager.loadAccount()?.id ?: return@setOnClickListener

            binding.enableInput = false
            updateViewModel.showProgress(true)

            updateViewModel.updateEmail(
                    userId = userId,
                    email = binding.emailInput.text.toString().trim()
            )
        }

        updateViewModel.updateResult.observe(viewLifecycleOwner, {
            binding.enableInput = it !is FetchResult.Success
        })
    }

    companion object {
        @JvmStatic
        fun newInstance() = UpdateEmailFragment()
    }
}
