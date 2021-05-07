package com.ft.ftchinese.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentPasswordResetBinding
import com.ft.ftchinese.model.reader.PwResetBearer
import com.ft.ftchinese.ui.base.ScopedBottomSheetDialogFragment
import com.ft.ftchinese.ui.data.FetchResult
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class PasswordResetFragment(
    private val tokenBearer: PwResetBearer,
) : ScopedBottomSheetDialogFragment(), AnkoLogger {

    private lateinit var viewModel: PasswordResetViewModel
    private lateinit var binding: FragmentPasswordResetBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = DataBindingUtil.inflate(
            inflater.cloneInContext(
                ContextThemeWrapper(
                    requireContext(),
                    R.style.AppTheme,
                ),
            ),
            R.layout.fragment_password_reset,
            container,
            false)
        binding.email = tokenBearer.email
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = activity?.run {
            ViewModelProvider(this)
                .get(PasswordResetViewModel::class.java)
        } ?: throw Exception("Invalid activity")

        binding.viewModel = viewModel
        binding.handler = this
        binding.lifecycleOwner = viewLifecycleOwner

        setupViewModel()
        setupUI()
    }

    private fun setupViewModel() {
        viewModel.resetResult.observe(viewLifecycleOwner) {
            when (it) {
                is FetchResult.LocalizedError -> toast(it.msgId)
                is FetchResult.Error -> it.exception.message?.let { msg -> toast(msg) }
                is FetchResult.Success -> {
                    if (it.data) {
                        toast("Password reset successfully.")
                        activity?.finish()
                    } else {
                        toast("Password reset failed. Please retry later")
                    }
                }
            }
        }
    }

    private fun setupUI() {
        binding.toolbar.bottomSheetToolbar.onClick {
            dismiss()
        }
    }

    fun onSubmit(view: View) {
        viewModel.resetPassword(tokenBearer.token)
    }
}
