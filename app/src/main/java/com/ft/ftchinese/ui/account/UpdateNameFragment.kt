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
import com.ft.ftchinese.databinding.FragmentUpdateUsernameBinding
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.ui.base.*
import com.ft.ftchinese.model.reader.SessionManager
import com.ft.ftchinese.viewmodel.Result
import com.ft.ftchinese.viewmodel.UpdateViewModel
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.support.v4.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class UpdateNameFragment : ScopedFragment(), AnkoLogger {
    private lateinit var sessionManager: SessionManager
    private lateinit var updateViewModel: UpdateViewModel
    private lateinit var binding: FragmentUpdateUsernameBinding
    private var account: Account? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        sessionManager = SessionManager.getInstance(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        account = sessionManager.loadAccount()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_update_username, container, false)
        binding.userNameInput.isEnabled = true
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.userName = account?.userName
        binding.userNameInput.requestFocus()
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
        // Validate input data.
        updateViewModel.updateFormState.observe(viewLifecycleOwner, Observer {
            val updateState = it ?: return@Observer

            binding.btnSave.isEnabled = updateState.isDataValid

            if (updateState.nameError != null) {
                binding.userNameInput.error = getString(updateState.nameError)
                binding.userNameInput.requestFocus()
            }
        })

        binding.userNameInput.afterTextChanged {
            updateViewModel.userNameDataChanged(
                    currentName = account?.userName,
                    newName = binding.userNameInput.text.toString().trim()
            )
        }

        binding.btnSave.setOnClickListener {
            updateViewModel.userNameDataChanged(
                    currentName = account?.userName,
                    newName = binding.userNameInput.text.toString().trim()
            )

            if (activity?.isNetworkConnected() != true) {
                toast(R.string.prompt_no_network)

                return@setOnClickListener
            }

            val userId = account?.id ?: return@setOnClickListener

            updateViewModel.showProgress(true)
            binding.enableInput = false

            updateViewModel.updateUserName(
                    userId = userId,
                    name = binding.userNameInput.text.toString().trim()
            )
        }

        // Re-enable button on error.
        updateViewModel.updateResult.observe(viewLifecycleOwner, Observer {
            binding.enableInput = it !is Result.Success
        })
    }

    companion object {

        @JvmStatic
        fun newInstance() = UpdateNameFragment()
    }
}
