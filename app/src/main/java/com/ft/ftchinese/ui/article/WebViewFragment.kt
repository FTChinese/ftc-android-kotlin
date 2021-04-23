package com.ft.ftchinese.ui.article

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.database.ArticleDb
import com.ft.ftchinese.databinding.FragmentWebViewBinding
import com.ft.ftchinese.model.content.Following
import com.ft.ftchinese.model.content.FollowingManager
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.store.AccountCache
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.ui.base.WVClient
import com.ft.ftchinese.ui.base.WVViewModel
import com.ft.ftchinese.ui.channel.JS_INTERFACE_NAME
import com.ft.ftchinese.viewmodel.Result
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast


class WebViewFragment : Fragment(), AnkoLogger {

    private lateinit var articleViewModel: ArticleViewModel
    private lateinit var wvViewModel: WVViewModel
    private lateinit var binding: FragmentWebViewBinding
    private lateinit var followingManager: FollowingManager

    override fun onAttach(context: Context) {
        super.onAttach(context)
        followingManager = FollowingManager.getInstance(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_web_view, container, false)
        // Inflate the layout for this fragment
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        articleViewModel = activity?.run {
            ViewModelProvider(
                this,
                ArticleViewModelFactory(
                    FileCache(requireContext()),
                    ArticleDb.getInstance(this)
                )
            ).get(ArticleViewModel::class.java)
        } ?: throw Exception("Invalid activity")

        wvViewModel = activity?.run {
            ViewModelProvider(this)
                .get(WVViewModel::class.java)
        } ?: throw Exception("Invalid activity")

        articleViewModel.htmlResult.observe(viewLifecycleOwner) { result ->

            articleViewModel.inProgress.value = false

            when (result) {
                is Result.LocalizedError -> {
                    toast(result.msgId)
                }
                is Result.Error -> {
                    result.exception.message?.let { toast(it) }
                }
                is Result.Success -> {

                    binding.webView.loadDataWithBaseURL(
                        Config.discoverServer(AccountCache.get()),
                        result.data,
                        "text/html",
                        null,
                        null)
                }
            }
        }

        articleViewModel.webUrlResult.observe(viewLifecycleOwner) { result ->
            articleViewModel.inProgress.value = false

            when (result) {
                is Result.LocalizedError -> toast(result.msgId)
                is Result.Error -> result.exception.message?.let { toast(it) }
                is Result.Success -> binding.webView.loadUrl(result.data)
            }
        }

        binding.webView.settings.apply {
            javaScriptEnabled = true
            loadsImagesAutomatically = true
            domStorageEnabled = true
            databaseEnabled = true
        }

        val wvClient = WVClient(requireContext(), wvViewModel)

        binding.webView.apply {
            addJavascriptInterface(
                this@WebViewFragment,
                JS_INTERFACE_NAME
            )

            webViewClient = wvClient
            webChromeClient = WebChromeClient()

            setOnKeyListener { _, keyCode, _ ->
                if (keyCode == KeyEvent.KEYCODE_BACK && binding.webView.canGoBack()) {
                    binding.webView.goBack()
                    return@setOnKeyListener true
                }
                false
            }
        }
    }

    @JavascriptInterface
    fun follow(message: String) {
        info("Clicked follow: $message")

        try {
            val following = json.parse<Following>(message) ?: return

            val isSubscribed = followingManager.save(following)

            if (isSubscribed) {
                FirebaseMessaging.getInstance()
                    .subscribeToTopic(following.topic)
                    .addOnCompleteListener { task ->
                        info("Subscribing to topic ${following.topic} success: ${task.isSuccessful}")
                    }
            } else {
                FirebaseMessaging.getInstance()
                    .unsubscribeFromTopic(following.topic)
                    .addOnCompleteListener { task ->
                        info("Unsubscribing from topic ${following.topic} success: ${task.isSuccessful}")
                    }
            }
        } catch (e: Exception) {
            info(e)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = WebViewFragment()
    }
}