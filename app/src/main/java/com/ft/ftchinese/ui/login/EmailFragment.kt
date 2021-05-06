package com.ft.ftchinese.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentEmailBinding
import com.ft.ftchinese.ui.base.ScopedFragment
import org.jetbrains.anko.AnkoLogger

@kotlinx.coroutines.ExperimentalCoroutinesApi
class EmailFragment : ScopedFragment(),
        AnkoLogger {

    private lateinit var viewModel: SignInViewModel
    private lateinit var emailViewModel: EmailExistsViewModel
    private lateinit var binding: FragmentEmailBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_email, container, false)

        binding.emailInput.requestFocus()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = activity?.run {
            ViewModelProvider(this)
                .get(SignInViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        emailViewModel = activity?.run {
            ViewModelProvider(this)
                .get(EmailExistsViewModel::class.java)
        } ?: throw Exception("Invalid activity")

        binding.viewModel = emailViewModel
        binding.handler = this
        binding.lifecycleOwner = viewLifecycleOwner

        // It's very weired if we assign the view model's
        // isFormEnabled directly in xml, it only works on initial
        // state. When progress changes, the button state does
        // not change.
        emailViewModel.isFormEnabled.observe(viewLifecycleOwner) {
            binding.isFormEnabled = it
        }
    }

    fun onSubmit(view: View) {
        emailViewModel.startChecking()
    }

    companion object {
        @JvmStatic
        fun newInstance() = EmailFragment()
    }
}


