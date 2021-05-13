package com.ft.ftchinese.ui.account

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityAccountBinding
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.util.RequestCode
import org.jetbrains.anko.AnkoLogger

@kotlinx.coroutines.ExperimentalCoroutinesApi
class WxInfoActivity : ScopedAppActivity(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var binding: ActivityAccountBinding

    private lateinit var infoViewModel: WxInfoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Here we reused the activity_account.
        binding = DataBindingUtil.setContentView(this, R.layout.activity_account)
        setSupportActionBar(binding.toolbar.toolbar)

        sessionManager = SessionManager.getInstance(this)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        infoViewModel = ViewModelProvider(this)
            .get(WxInfoViewModel::class.java)

        connectionLiveData.observe(this) {
            infoViewModel.isNetworkAvailable.value = it
        }
        isConnected.let {
            infoViewModel.isNetworkAvailable.value = it
        }

        supportFragmentManager.commit {
            replace(R.id.frag_account, WxInfoFragment.newInstance())
        }
    }

    /**
     * Received results from [UnlinkActivity] with
     * RequestCode.Unlink.
     * If [LinkWxDialogFragment] is used inside this activity,
     * user starts linking wechat to FTC account, ideally
     * this activity should receive a RequestCode.Link
     * message. But I'm not sure whether this works or not.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) {
            return
        }

        /**
         * Chain of unwrapping:
         * [AccountActivity] -> [WxInfoActivity] -> [UnlinkActivity]
         */
        if (requestCode == RequestCode.UNLINK) {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, WxInfoActivity::class.java))
        }
    }
}
