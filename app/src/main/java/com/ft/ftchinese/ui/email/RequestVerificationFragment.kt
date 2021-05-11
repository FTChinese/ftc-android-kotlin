package com.ft.ftchinese.ui.email

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentRequestVerificationBinding
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.data.FetchResult
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class RequestVerificationFragment : ScopedFragment(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var binding: FragmentRequestVerificationBinding
    private lateinit var viewModel: EmailViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        sessionManager = SessionManager.getInstance(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_request_verification,
            container,
            false,
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = activity?.run {
            ViewModelProvider(this)
                .get(EmailViewModel::class.java)
        } ?: throw Exception("Invalid activity")

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.handler = this

        viewModel.letterSent.observe(viewLifecycleOwner) {
            when (it) {
                is FetchResult.LocalizedError -> {
                    toast(it.msgId)
                    binding.btnSendRequest.isEnabled = true
                }
                is FetchResult.Error -> {
                    it.exception.message?.let { toast(it) }
                    binding.btnSendRequest.isEnabled  = true
                }
                is FetchResult.Success -> {
                    showAlert()
                }
            }
        }
    }

    private fun showAlert() {
        alert(R.string.email_vrf_letter_sent) {
            positiveButton("Got it") {
                it.dismiss()
            }
        }.show()
    }

    fun onClickButton(view: View) {
        viewModel.requestVerification()
        toast(R.string.progress_request_verification)
    }

    companion object {
        @JvmStatic
        fun newInstance() =
                RequestVerificationFragment()
    }
}
