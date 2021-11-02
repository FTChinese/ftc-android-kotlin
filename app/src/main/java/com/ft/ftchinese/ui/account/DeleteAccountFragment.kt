package com.ft.ftchinese.ui.account

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentDeleteAccountBinding
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.price.Edition
import com.ft.ftchinese.store.AccountCache
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.IntentsUtil
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.dialog.AlertDialogFragment
import com.ft.ftchinese.ui.dialog.DialogArgs
import com.ft.ftchinese.ui.formatter.formatEdition
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class DeleteAccountFragment : ScopedFragment() {

    private lateinit var binding: FragmentDeleteAccountBinding
    private lateinit var viewModel: DeleteAccountViewModel
    private lateinit var sessionManager: SessionManager

    override fun onAttach(context: Context) {
        super.onAttach(context)
        sessionManager = SessionManager.getInstance(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_delete_account,
            container,
            false
        )
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = activity?.run {
            ViewModelProvider(this)[DeleteAccountViewModel::class.java]
        } ?: throw Exception("Invalid activity")

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.handler = this

        viewModel.deleted.observe(viewLifecycleOwner) {
            when (it) {
                is FetchResult.LocalizedError -> alertError(getString(it.msgId))
                is FetchResult.Error -> it.exception.message?.let { msg -> alertError(msg) }
                is FetchResult.Success -> {
                    Log.i(TAG, "Account deleted")
                    toast(R.string.message_account_deleted)

                    sessionManager.logout()
                    activity?.finish()
                }
            }
        }

        viewModel.validSubsExists.observe(viewLifecycleOwner) {
            if (!it) {
                return@observe
            }

            alertDeleteDenied()
        }
    }

    private fun alertError(msg: String) {
        AlertDialogFragment.newErrInstance(msg).show(childFragmentManager, "DeleteAccountError")
    }

    private fun alertDeleteDenied() {
        AlertDialogFragment.newInstance(
            params = DialogArgs(
                title = R.string.title_delete_account_denied,
                message = getString(R.string.message_delete_account_valid_subs),
                positiveButton = R.string.button_send_email,
                negativeButton = R.string.action_cancel
            )
        )
            .onPositiveButtonClicked { dialog, _ ->
                dialog.dismiss()
                sendEmail()
            }
            .onNegativeButtonClicked { dialog, _ ->
                dialog.cancel()
            }
            .show(childFragmentManager, "DeleteAccountDenied")
    }

    private fun sendEmail() {

        val intent = IntentsUtil.emailCustomerService(
            title = getString(R.string.subject_delete_account_valid_subs),
            body = composeDeletionEmail()
        )

        activity?.run {
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                toast(R.string.prompt_no_email_app)
            }
        }
    }

    private fun composeDeletionEmail(): String? {
        val a = AccountCache.get() ?: return null
        val emailOrMobile = if (a.isMobileEmail) {
            a.mobile
        } else {
            a.email
        }

        if (a.membership.tier == null || a.membership.cycle == null) {
            return null
        }

        val edition = formatEdition(
            requireContext(),
            Edition(
                tier = a.membership.tier,
                cycle = a.membership.cycle,
            )
        )

        return "FT中文网，\n请删除我的账号 $emailOrMobile。\n我的账号已经购买了FT中文网付费订阅服务 $edition，到期时间 ${a.membership.localizeExpireDate()}。我已知悉删除账号的同时将删除我的订阅信息。"
    }

    fun onSubmit(view: View) {
        AccountCache.get()?.let {
            viewModel.deleteAccount(it)
        }
    }

    companion object {
        private const val TAG = "DeleteAccountFragment"

        @JvmStatic
        fun newInstance() = DeleteAccountFragment()
    }
}
