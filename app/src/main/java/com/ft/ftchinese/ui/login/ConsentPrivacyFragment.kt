package com.ft.ftchinese.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentConsentPrivacyBinding
import com.ft.ftchinese.model.legal.legalPages
import com.ft.ftchinese.ui.webpage.WebpageActivity

class ConsentPrivacyFragment : Fragment() {

    lateinit var binding: FragmentConsentPrivacyBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_consent_privacy, container, false)

        // Inflate the layout for this fragment
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnAgreement.setOnClickListener {
            WebpageActivity.start(requireContext(), legalPages[0])
        }

        binding.btnPrivacy.setOnClickListener {
            WebpageActivity.start(requireContext(), legalPages[1])
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = ConsentPrivacyFragment()
    }
}
