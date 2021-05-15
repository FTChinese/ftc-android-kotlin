package com.ft.ftchinese.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentLogoutBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class LogoutDialogFragment : BottomSheetDialogFragment() {
    private lateinit var viewModel: LogoutViewModel
    private lateinit var binding: FragmentLogoutBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_logout,
            container,
            false,
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = activity?.run {
            ViewModelProvider(this)
                .get(LogoutViewModel::class.java)
        } ?: throw Exception("Invalid exception")

        binding.handler = this
    }

    fun onClickLogout(view: View) {
        viewModel.logout()
        dismiss()
    }
}
