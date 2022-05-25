package com.ft.ftchinese.ui.account

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityUpdateAccountBinding
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.ui.mobile.MobileFragment
import com.ft.ftchinese.ui.mobile.MobileViewModel

class UpdateActivity : ScopedAppActivity() {

    private lateinit var sessionManager: SessionManager

    private lateinit var mobileViewModel: MobileViewModel
    private lateinit var deleteViewModel: DeleteAccountViewModel

    private lateinit var binding: ActivityUpdateAccountBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_update_account)

        setSupportActionBar(binding.toolbar.toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        sessionManager = SessionManager.getInstance(this)

        mobileViewModel = ViewModelProvider(this)[MobileViewModel::class.java]

        deleteViewModel = ViewModelProvider(this)[DeleteAccountViewModel::class.java]

        connectionLiveData.observe(this) {
            mobileViewModel.isNetworkAvailable.value = it
            // MobileViewModel's network is configured in the its fragment.
            deleteViewModel.isNetworkAvailable.value = it
        }

        isConnected.let {
            mobileViewModel.isNetworkAvailable.value = it
            deleteViewModel.isNetworkAvailable.value = it
        }

        val fm = supportFragmentManager
                .beginTransaction()

        when (intent.getSerializableExtra(TARGET_FRAG)) {
            AccountRowId.MOBILE -> {
                supportActionBar?.title = "关联手机号码"
                fm.replace(R.id.first_frag, MobileFragment.newInstanceForUpdate())
            }
            AccountRowId.DELETE -> {
                supportActionBar?.setTitle(R.string.title_delete_account)
                fm.replace(R.id.first_frag, DeleteAccountFragment.newInstance())
            }
        }

        fm.commit()

        setupViewModel()
    }

    private fun setupViewModel() {
        deleteViewModel.progressLiveData.observe(this) {
            binding.inProgress = it
        }
    }

    companion object {

        private const val TARGET_FRAG = "extra_target_fragment"

        @JvmStatic
        fun start(context: Context, rowType: AccountRowId?) {
            context.startActivity(
                Intent(context, UpdateActivity::class.java).apply {
                    putExtra(TARGET_FRAG, rowType)
                }
            )
        }

        @JvmStatic
        fun intent(context: Context, rowType: AccountRowId) = Intent(context, UpdateActivity::class.java).apply {
            putExtra(TARGET_FRAG, rowType)
        }
    }
}
