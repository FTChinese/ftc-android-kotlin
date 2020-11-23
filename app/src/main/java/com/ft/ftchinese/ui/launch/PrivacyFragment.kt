package com.ft.ftchinese.ui.launch

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentPrivacyBinding
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.store.ServiceAcceptance
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.viewmodel.AccountViewModel
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

@kotlinx.coroutines.ExperimentalCoroutinesApi
class PrivacyFragment : ScopedFragment(), AnkoLogger {

    private lateinit var binding: FragmentPrivacyBinding
    private lateinit var cache: FileCache
    private lateinit var session: SessionManager

    private lateinit var accountViewModel: AccountViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)

        cache = FileCache(context)
        session = SessionManager.getInstance(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_privacy, container, false)
        // Inflate the layout for this fragment
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
        }

        accountViewModel = activity?.run {
            ViewModelProvider(this).get(AccountViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        val text = cache.readPrivacy() ?: return

        info(text)

        binding.webView.loadDataWithBaseURL(
            Config.discoverServer(session.loadAccount()),
            text,
            "text/html",
            null,
            null)

        binding.declineBtn.setOnClickListener {
            activity?.finish()
        }

        binding.acceptBtn.setOnClickListener {
            accountViewModel.serviceAccepted.value = true
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = PrivacyFragment()
    }
}
