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
import com.ft.ftchinese.databinding.FragmentRequestVerificationBinding
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.base.isNetworkConnected
import com.ft.ftchinese.model.reader.SessionManager
import com.ft.ftchinese.viewmodel.Result
import com.ft.ftchinese.viewmodel.UpdateViewModel
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.toast
import java.lang.Exception

@kotlinx.coroutines.ExperimentalCoroutinesApi
class RequestVerificationFragment : ScopedFragment(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var updateViewModel: UpdateViewModel
    private lateinit var binding: FragmentRequestVerificationBinding

    override fun onAttach(context: Context) {
        super.onAttach(context)
        sessionManager = SessionManager.getInstance(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_request_verification, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        updateViewModel = activity?.run {
            ViewModelProvider(this)
                    .get(UpdateViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        updateViewModel.sendEmailResult.observe(viewLifecycleOwner, Observer {
            onEmailSent(it)
        })

        binding.btnSendRequest.setOnClickListener {
            val userId = sessionManager.loadAccount()?.id ?: return@setOnClickListener

            if (activity?.isNetworkConnected() != true) {
                toast(R.string.prompt_no_network)

                return@setOnClickListener
            }

            updateViewModel.showProgress(true)
            it.isEnabled = false

            toast(R.string.progress_request_verification)

            updateViewModel.requestVerification(userId)
        }
    }

    private fun onEmailSent(result: Result<Boolean>) {

        updateViewModel.inProgress.value = false

        when (result) {
            is Result.LocalizedError -> {
                toast(result.msgId)
                binding.btnSendRequest.isEnabled = true
            }
            is Result.Error -> {
                result.exception.message?.let { toast(it) }
                binding.btnSendRequest.isEnabled  = true
            }
            is Result.Success -> {
                alert(R.string.prompt_letter_sent) {
                    positiveButton("Got it") {
                        it.dismiss()
                    }
                }.show()
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
                RequestVerificationFragment()
    }
}
