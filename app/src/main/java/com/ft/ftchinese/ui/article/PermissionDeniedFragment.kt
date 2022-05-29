package com.ft.ftchinese.ui.article

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentPermissionDeniedBinding
import com.ft.ftchinese.model.reader.Access
import com.ft.ftchinese.model.reader.Permission
import com.ft.ftchinese.ui.MemberActivity
import com.ft.ftchinese.ui.SubsActivity
import com.ft.ftchinese.ui.channel.DenialReason
import com.ft.ftchinese.ui.login.AuthActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PermissionDeniedFragment : BottomSheetDialogFragment() {

    private var access: Access? = null

    private lateinit var binding: FragmentPermissionDeniedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            access = it.getParcelable(ARG_ACCESS)
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        // For language switcher, user can cancel the dialog and content will not be switched.
        if (access?.cancellable != true) {
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

    private fun setupUI(access: Access) {

        binding.isLoggedIn = access.loggedIn
        binding.denied = DenialReason.from(requireContext(), access)

        binding.loginOrSubscribe.setOnClickListener {
            if (access.loggedIn) {
                SubsActivity.start(
                    context = requireContext(),
                    premiumFirst = access.content == Permission.PREMIUM,
                )
            } else {
                AuthActivity.startForResult(requireActivity())
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        access?.let {
            setupUI(it)
        }

        binding.showMemberStatus.setOnClickListener() {
            MemberActivity.startForResult(activity)
        }
    }

    companion object {
        private const val ARG_ACCESS = "arg_access"
        private const val ARG_CANCELLABLE = "arg_cancellable"

        @JvmStatic
        fun newInstance(
            access: Access,
            cancellable: Boolean = false, // If the bottom sheet is cancellable, user can dismiss it without affecting the parent activity; otherwise the parent activity will be destroyed upon cancellation.
        ) = PermissionDeniedFragment().apply {
            arguments = bundleOf(
                ARG_ACCESS to access,
                ARG_CANCELLABLE to cancellable,
            )
        }
    }
}
