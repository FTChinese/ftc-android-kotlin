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
import com.ft.ftchinese.viewmodel.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.sdk27.coroutines.onClick
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        sessionManager = SessionManager.getInstance(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        binding.viewModel = viewModel



        viewModel.codeSent.observe(viewLifecycleOwner) {
            when (it) {
                is Result.LocalizedError -> toast(it.msgId)
                is Result.Error -> it.exception.message?.let { msg -> toast(msg) }
                is Result.Success -> {
                    startCounting()
                    toast("验证码已发送")
                }
            }
        }

        viewModel.mobileUpdated.observe(viewLifecycleOwner) {
            when (it) {
                is Result.LocalizedError -> toast(it.msgId)
                is Result.Error -> it.exception.message?.let { msg -> toast(msg) }
                is Result.Success -> {
                    toast(R.string.prompt_updated)
                    sessionManager.loadAccount()?.withBaseAccount(it.data)?.let { account ->
                        sessionManager.saveAccount(account)
                    }
                }
            }
        }

        binding.requestCode.onClick {

            sessionManager.loadAccount()?.let {
                viewModel.requestCode(it)
            }
        }

        binding.btnSave.onClick {

        }
    }

    private fun startCounting() {
        binding.requestCode.isEnabled = false


        launch(Dispatchers.Main) {
            for (i in 60 downTo 1) {
                binding.requestCode.text = "重新获取{$i}s"
                delay(1000)
            }

            binding.requestCode.isEnabled = true
            binding.requestCode.text = "获取验证码"
        }
    }

    companion object {

        @JvmStatic
        fun newInstance() = UpdateMobileFragment()
    }
}
