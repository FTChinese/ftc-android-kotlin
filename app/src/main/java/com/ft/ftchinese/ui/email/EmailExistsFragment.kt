package com.ft.ftchinese.ui.email

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentEmailExistsBinding
import com.ft.ftchinese.ui.base.ScopedFragment

@kotlinx.coroutines.ExperimentalCoroutinesApi
class EmailExistsFragment : ScopedFragment() {

    private lateinit var emailViewModel: EmailViewModel
    private lateinit var binding: FragmentEmailExistsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_email_exists,
            container,
            false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        emailViewModel = activity?.run {
            ViewModelProvider(this)
                .get(EmailViewModel::class.java)
        } ?: throw Exception("Invalid activity")

        binding.viewModel = emailViewModel
        binding.handler = this
        binding.lifecycleOwner = viewLifecycleOwner

        binding.emailInput.requestFocus()
    }

    fun onSubmit(view: View) {
        emailViewModel.checkExists()
    }

    companion object {
        @JvmStatic
        fun newInstance() = EmailExistsFragment()
    }
}


