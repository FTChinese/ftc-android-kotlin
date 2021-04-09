package com.ft.ftchinese.ui.article

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentPermissionDeniedBinding
import com.ft.ftchinese.model.reader.Access
import com.ft.ftchinese.model.reader.MemberStatus
import com.ft.ftchinese.model.reader.Permission
import com.ft.ftchinese.ui.channel.DenialReason
import com.ft.ftchinese.ui.login.LoginActivity
import com.ft.ftchinese.ui.paywall.PaywallActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.jetbrains.anko.sdk27.coroutines.onClick

/**
 * create an instance of this fragment.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class PermissionDeniedFragment(
    private val denied: Access,
    private val cancellable: Boolean = false, // If the bottom sheet is cancellable, user can dismiss it without affecting the parent activity; otherwise the parent activity will be destroyed upon cancellation.
) : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentPermissionDeniedBinding

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        // For language switcher, user can cancel the dialog and content will not be switched.
        if (!cancellable) {
            activity?.finish()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_permission_denied, container, false)

        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.denied = DenialReason.from(requireContext(), denied)

        binding.loginOrSubscribe.onClick {
            if (denied.status == MemberStatus.NotLoggedIn) {
                LoginActivity.startForResult(requireActivity())
            } else {
                PaywallActivity.start(
                    context = requireContext(),
                    premiumFirst = denied.content == Permission.PREMIUM,
                )
            }
        }
    }
}
