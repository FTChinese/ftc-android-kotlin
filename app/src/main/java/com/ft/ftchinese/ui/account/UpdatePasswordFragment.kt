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
import com.ft.ftchinese.databinding.FragmentUpdatePasswordBinding
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.base.isNetworkConnected
import com.ft.ftchinese.model.reader.Passwords
import com.ft.ftchinese.model.reader.SessionManager
import com.ft.ftchinese.ui.Validator
import com.ft.ftchinese.viewmodel.Result
import com.ft.ftchinese.viewmodel.UpdateViewModel
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.support.v4.toast
import java.lang.Exception

@kotlinx.coroutines.ExperimentalCoroutinesApi
class UpdatePasswordFragment : ScopedFragment(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var updateViewModel: UpdateViewModel
    private lateinit var binding: FragmentUpdatePasswordBinding

    override fun onAttach(context: Context) {
        super.onAttach(context)

        sessionManager = SessionManager.getInstance(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_update_password, container, false)
        binding.enableInput = true
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        updateViewModel = activity?.run {
            ViewModelProvider(this)
                    .get(UpdateViewModel::class.java)
        } ?: throw Exception("Invalid Exception")

        setUp()
    }

    private fun setUp() {

        binding.btnSave.setOnClickListener {
            val passwords = validate() ?: return@setOnClickListener

            if (activity?.isNetworkConnected() != true) {
                toast(R.string.prompt_no_network)
                return@setOnClickListener
            }

            val userId = sessionManager.loadAccount()?.id ?: return@setOnClickListener

            updateViewModel.showProgress(true)
            binding.enableInput = false

            updateViewModel.updatePassword(
                    userId = userId,
                    passwords = passwords
            )
        }

        // Re-enable input in case errors
        updateViewModel.updateResult.observe(viewLifecycleOwner, Observer {
            binding.enableInput = it !is Result.Success
        })
    }

    private fun validate(): Passwords? {
        binding.oldPasswordInput.error = null
        binding.newPasswordInput.error = null
        binding.confirmPasswordInput.error = null

        val oldPassword = binding.oldPasswordInput.text.toString().trim()
        val newPassword = binding.newPasswordInput.text.toString().trim()
        val confirmPassword = binding.confirmPasswordInput.text.toString().trim()

        if (oldPassword.isBlank()) {
            binding.oldPasswordInput.error = getString(R.string.error_invalid_password)
            binding.oldPasswordInput.requestFocus()
            return null
        }

        var msgId = Validator.ensurePassword(newPassword)
        if (msgId != null) {
            binding.newPasswordInput.error = getString(msgId)
            binding.newPasswordInput.requestFocus()
            return null
        }

        msgId = Validator.ensurePassword(confirmPassword)
        if (msgId != null) {
            binding.confirmPasswordInput.error = getString(msgId)
            binding.confirmPasswordInput.requestFocus()
            return null
        }

        if (newPassword != confirmPassword) {
            binding.confirmPasswordInput.error = getString(R.string.error_mismatched_confirm_password)
            binding.confirmPasswordInput.requestFocus()
            return null
        }

        return Passwords(
                oldPassword = oldPassword,
                newPassword = newPassword
        )
    }

    companion object {

        @JvmStatic
        fun newInstance() = UpdatePasswordFragment()
    }
}
