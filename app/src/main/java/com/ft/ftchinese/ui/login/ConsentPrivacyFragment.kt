package com.ft.ftchinese.ui.login

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentConsentPrivacyBinding
import com.ft.ftchinese.model.legal.legalPages
import com.ft.ftchinese.ui.webpage.WebpageActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.jetbrains.anko.sdk27.coroutines.onClick

@ExperimentalCoroutinesApi
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

        binding.btnAgreement.onClick {
            WebpageActivity.start(requireContext(), legalPages[0])
        }

        binding.btnPrivacy.onClick {
            WebpageActivity.start(requireContext(), legalPages[1])
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = ConsentPrivacyFragment()
    }
}
