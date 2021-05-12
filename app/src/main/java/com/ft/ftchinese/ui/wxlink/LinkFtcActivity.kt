package com.ft.ftchinese.ui.wxlink

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityFragmentDoubleBinding
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.ui.email.EmailViewModel
import com.ft.ftchinese.ui.email.EmailExistsFragment
import com.ft.ftchinese.ui.login.*
import com.ft.ftchinese.util.RequestCode
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

/**
 * UI for a wx-only user to link to an email account.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class LinkFtcActivity : ScopedAppActivity(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var emailViewModel: EmailViewModel
    private lateinit var binding: ActivityFragmentDoubleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_fragment_double,
        )

        setSupportActionBar(binding.toolbar.toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        sessionManager = SessionManager.getInstance(this)

        emailViewModel = ViewModelProvider(this)
            .get(EmailViewModel::class.java)

        connectionLiveData.observe(this) {
            emailViewModel.isNetworkAvailable.value = it
        }

        isConnected.let {
            emailViewModel.isNetworkAvailable.value = it
        }

        setupViewModel()

        supportFragmentManager.commit {
            replace(R.id.double_frag_primary, EmailExistsFragment.newInstance())
        }
    }

    private fun setupViewModel() {

        emailViewModel.progressLiveData.observe(this) {
            binding.inProgress = it
        }

        emailViewModel.existsResult.observe(this) { result ->
            when (result) {
                is FetchResult.LocalizedError -> toast(result.msgId)
                is FetchResult.Error -> result.exception.message?.let { msg -> toast(msg) }
                is FetchResult.Success -> {
                    // If email exists, show sign in.
                    if (result.data) {
                        SignInFragment
                            .forWechatLink()
                            .show(supportFragmentManager, "WechatLink")
                    } else {
                        // If email does not exist, show sing up.
                        SignUpFragment
                            .forWechatLink()
                            .show(supportFragmentManager, "WechatLink")
                    }
                }
            }
        }
    }

    /**
     * Unwrapping chain:
     * [AccountActivity] <- current activity <- [LinkPreviewFragment]
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        info("onActivityResult: requestCode $requestCode, $resultCode")

        if (requestCode != RequestCode.LINK) {
            return
        }

        if (resultCode != Activity.RESULT_OK) {
            setResult(Activity.RESULT_CANCELED)
            return
        }

        info("Bound to an existing ftc account")
        /**
         * Pass data back to [AccountActivity]
         */
        setResult(Activity.RESULT_OK)
        finish()
    }

    companion object {
        @JvmStatic
        fun startForResult(activity: Activity?) {
            activity?.startActivityForResult(
                Intent(activity, LinkFtcActivity::class.java),
                RequestCode.LINK
            )

        }
    }
}
