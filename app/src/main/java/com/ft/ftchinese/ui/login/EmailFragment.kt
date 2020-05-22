package com.ft.ftchinese.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentEmailBinding
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.base.afterTextChanged
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.viewmodel.LoginViewModel
import com.ft.ftchinese.viewmodel.Result
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.support.v4.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class EmailFragment : ScopedFragment(),
        AnkoLogger {

    private lateinit var viewModel: LoginViewModel
    private lateinit var binding: FragmentEmailBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_email, container, false)
        // Initially we want the button disabled and input
        // enabled so that user cannot submit empty email.
        binding.emailInput.isEnabled = true
        binding.emailInput.requestFocus()

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProvider(this)
                    .get(LoginViewModel::class.java)
        } ?: throw Exception("Invalid Activity")


        // Validate email upon changed.
        binding.emailInput.afterTextChanged {
            viewModel.emailDataChanged(binding.emailInput.text.toString().trim())
        }

        viewModel.loginFormState.observe(viewLifecycleOwner, Observer {
            val loginState = it ?: return@Observer

            binding.nextBtn.isEnabled = loginState.isEmailValid

            if (loginState.error != null) {
                binding.emailInput.error = getString(loginState.error)
                binding.emailInput.requestFocus()
            }
        })

        binding.nextBtn.setOnClickListener {
            if (context?.isConnected != true) {
                toast(R.string.prompt_no_network)
                return@setOnClickListener
            }

            // Upon submitting data, disable input and button.
            // Shwo progress.
            binding.enableInput = false
            viewModel.inProgress.value = true

            viewModel.checkEmail(binding.emailInput.text.toString().trim())
        }

        // Enable or disable input depending on network result.
        // Only re-enable button if there's any error.
        viewModel.emailResult.observe(viewLifecycleOwner, Observer {

            // In case any error occurred, allow user to
            // re-enter and re-submit data.
            binding.enableInput = it !is Result.Success
        })
    }

    override fun onResume() {
        super.onResume()
        binding.enableInput = true
    }

    companion object {
        @JvmStatic
        fun newInstance() = EmailFragment()
    }
}


