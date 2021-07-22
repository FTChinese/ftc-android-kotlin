package com.ft.ftchinese.ui.webpage

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentWebpageBinding

class WebpageFragment : Fragment() {
    private lateinit var binding: FragmentWebpageBinding
    private lateinit var wpViewModel: WebpageViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_webpage, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        wpViewModel = activity?.run {
            ViewModelProvider(this)
                .get(WebpageViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        setupWebView()
        setupViewModel()
    }

    private fun setupViewModel() {
        wpViewModel.urlLiveData.observe(viewLifecycleOwner) {
            binding.webView.loadUrl(it)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        // Setup webview
        binding.webView.settings.apply {
            javaScriptEnabled = true
            loadsImagesAutomatically = true
            domStorageEnabled = true
            databaseEnabled = true
        }

        binding.webView.apply {

            setOnKeyListener { _, keyCode, _ ->
                if (keyCode == KeyEvent.KEYCODE_BACK && binding.webView.canGoBack()) {
                    binding.webView.goBack()
                    return@setOnKeyListener true
                }
                false
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = WebpageFragment()
    }
}
