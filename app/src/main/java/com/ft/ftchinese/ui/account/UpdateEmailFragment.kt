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
import com.ft.ftchinese.ui.base.*
import com.ft.ftchinese.model.reader.SessionManager
import com.ft.ftchinese.viewmodel.Result
import com.ft.ftchinese.viewmodel.UpdateViewModel
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.support.v4.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class UpdateEmailFragment : ScopedFragment(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var updateViewModel: UpdateViewModel
    private lateinit var binding: FragmentUpdateEmailBinding
    private var currentEmail: String? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        sessionManager = SessionManager.getInstance(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_update_email, container, false)

        binding.emailInput.isEnabled = true
        binding.labelEmailTv.text
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentEmail = sessionManager.loadAccount()?.email

        binding.labelEmailTv.text = if (currentEmail != null) {
            getString(R.string.label_current_email, currentEmail)
        } else {
            getString(R.string.label_current_email, getString(R.string.prompt_not_set))
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        updateViewModel = activity?.run {
            ViewModelProvider(this)
                    .get(UpdateViewModel::class.java)
        } ?: throw Exception("Invalid Exception")

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
                    currentEmail = currentEmail ?: "",
                    newEmail = binding.emailInput.text.toString().trim()
            )
        }

        binding.btnSave.setOnClickListener {
            updateViewModel.emailDataChanged(
                    currentEmail = currentEmail ?: "",
                    newEmail = binding.emailInput.text.toString().trim()
            )

            if (activity?.isNetworkConnected() != true) {
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

        updateViewModel.updateResult.observe(viewLifecycleOwner, Observer {
            binding.enableInput = it !is Result.Success
        })
    }

    companion object {
        @JvmStatic
        fun newInstance() = UpdateEmailFragment()
    }
}
