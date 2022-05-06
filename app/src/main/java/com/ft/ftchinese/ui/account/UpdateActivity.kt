package com.ft.ftchinese.ui.account

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityUpdateAccountBinding
import com.ft.ftchinese.store.AccountCache
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.account.password.PasswordViewModel
import com.ft.ftchinese.ui.account.password.UpdatePasswordFragment
import com.ft.ftchinese.ui.account.address.AddressViewModel
import com.ft.ftchinese.ui.account.address.UpdateAddressFragment
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.ui.email.EmailViewModel
import com.ft.ftchinese.ui.email.RequestVerificationFragment
import com.ft.ftchinese.ui.email.UpdateEmailFragment
import com.ft.ftchinese.ui.mobile.MobileFragment
import com.ft.ftchinese.ui.mobile.MobileViewModel
import com.ft.ftchinese.ui.account.name.NameViewModel
import com.ft.ftchinese.ui.account.name.UpdateNameFragment

class UpdateActivity : ScopedAppActivity() {

    private lateinit var sessionManager: SessionManager

    private lateinit var addressViewModel: AddressViewModel
    private lateinit var emailViewModel: EmailViewModel
    private lateinit var nameViewModel: NameViewModel
    private lateinit var passwordViewModel: PasswordViewModel
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

        emailViewModel = ViewModelProvider(this)[EmailViewModel::class.java]

        nameViewModel = ViewModelProvider(this)[NameViewModel::class.java]

        passwordViewModel = ViewModelProvider(this)[PasswordViewModel::class.java]

        addressViewModel = ViewModelProvider(this)[AddressViewModel::class.java]

        mobileViewModel = ViewModelProvider(this)[MobileViewModel::class.java]

        deleteViewModel = ViewModelProvider(this)[DeleteAccountViewModel::class.java]

        connectionLiveData.observe(this) {
            emailViewModel.isNetworkAvailable.value = it
            nameViewModel.isNetworkAvailable.value = it
            passwordViewModel.isNetworkAvailable.value = it
            addressViewModel.isNetworkAvailable.value = it
            // MobileViewModel's network is configured in the its fragment.
            deleteViewModel.isNetworkAvailable.value = it
        }

        isConnected.let {
            emailViewModel.isNetworkAvailable.value = it
            nameViewModel.isNetworkAvailable.value = it
            passwordViewModel.isNetworkAvailable.value = it
            addressViewModel.isNetworkAvailable.value = it
            deleteViewModel.isNetworkAvailable.value = it
        }

        val fm = supportFragmentManager
                .beginTransaction()

        when (intent.getSerializableExtra(TARGET_FRAG)) {
            AccountRowType.EMAIL -> {
                supportActionBar?.setTitle(R.string.title_change_email)
                val account = AccountCache.get() ?: return
                // Only show verification button for real emails that are not verified.
                // Do not show it for mobile-created account since that email address is not usable.
                if (!account.isMobileEmail && !account.isVerified) {
                    fm.replace(R.id.first_frag, RequestVerificationFragment.newInstance())
                }

                fm.replace(R.id.second_frag, UpdateEmailFragment.newInstance())
            }
            AccountRowType.USER_NAME -> {
                supportActionBar?.setTitle(R.string.title_change_username)
                fm.replace(R.id.first_frag, UpdateNameFragment.newInstance())
            }
            AccountRowType.PASSWORD -> {
                supportActionBar?.setTitle(R.string.title_change_password)
                fm.replace(R.id.first_frag, UpdatePasswordFragment.newInstance())
            }
            AccountRowType.Address -> {
                supportActionBar?.title = "设置地址"
                fm.replace(R.id.first_frag, UpdateAddressFragment.newInstance())
            }
            AccountRowType.MOBILE -> {
                supportActionBar?.title = "关联手机号码"
                fm.replace(R.id.first_frag, MobileFragment.newInstanceForUpdate())
            }
            AccountRowType.DELETE -> {
                supportActionBar?.setTitle(R.string.title_delete_account)
                fm.replace(R.id.first_frag, DeleteAccountFragment.newInstance())
            }
        }

        fm.commit()

        setupViewModel()
    }

    private fun setupViewModel() {

        emailViewModel.progressLiveData.observe(this) {
            binding.inProgress = it
        }

        nameViewModel.progressLiveData.observe(this) {
            binding.inProgress = it
        }

        passwordViewModel.progressLiveData.observe(this) {
            binding.inProgress = it
        }

        addressViewModel.progressLiveData.observe(this) {
            binding.inProgress = it
        }

        deleteViewModel.progressLiveData.observe(this) {
            binding.inProgress = it
        }
    }

    companion object {

        private const val TARGET_FRAG = "extra_target_fragment"

        @JvmStatic
        fun start(context: Context, rowType: AccountRowType?) {
            context.startActivity(
                Intent(context, UpdateActivity::class.java).apply {
                    putExtra(TARGET_FRAG, rowType)
                }
            )
        }

        @JvmStatic
        fun intent(context: Context, rowType: AccountRowType) = Intent(context, UpdateActivity::class.java).apply {
            putExtra(TARGET_FRAG, rowType)
        }
    }
}
