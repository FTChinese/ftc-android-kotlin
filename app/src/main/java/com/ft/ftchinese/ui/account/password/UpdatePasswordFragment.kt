package com.ft.ftchinese.ui.account.password

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentUpdatePasswordBinding
import com.ft.ftchinese.store.AccountCache
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.ui.login.ForgotPasswordActivity
import org.jetbrains.anko.support.v4.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class UpdatePasswordFragment : ScopedFragment() {

    private lateinit var binding: FragmentUpdatePasswordBinding
    private lateinit var viewModel: PasswordViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_update_password,
            container,
            false,
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = activity?.run {
            ViewModelProvider(this)[PasswordViewModel::class.java]
        } ?: throw Exception("Invalid activity")

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.handler = this

        viewModel.updated.observe(viewLifecycleOwner) {
            when (it) {
                is FetchResult.LocalizedError -> handleApiError(it.msgId)
                is FetchResult.TextError -> toast(it.text)
                is FetchResult.Success -> {
                    toast(R.string.prompt_saved)
                }
            }
        }

        binding.oldPasswordInput.requestFocus()
    }

    private fun handleApiError(msgId: Int) {
        if (msgId == R.string.password_current_incorrect) {
            AlertDialog.Builder(requireContext())
                .setMessage(msgId)
                .setPositiveButton(R.string.forgot_password_link) { dialog, _ ->
                    ForgotPasswordActivity.start(
                        requireContext(),
                        AccountCache.get()?.email ?: ""
                    )
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.action_cancel) { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()

            return
        }

        toast(msgId)
    }

    fun onSubmit(view: View) {
        viewModel.updatePassword()
    }

    companion object {
        @JvmStatic
        fun newInstance() = UpdatePasswordFragment()
    }
}
