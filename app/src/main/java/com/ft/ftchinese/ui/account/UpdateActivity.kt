package com.ft.ftchinese.ui.account

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityUpdateAccountBinding
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
import org.jetbrains.anko.AnkoLogger

@kotlinx.coroutines.ExperimentalCoroutinesApi
class UpdateActivity : ScopedAppActivity(), AnkoLogger {

    private lateinit var sessionManager: SessionManager

    private lateinit var addressViewModel: AddressViewModel
    private lateinit var emailViewModel: EmailViewModel
    private lateinit var nameViewModel: NameViewModel
    private lateinit var passwordViewModel: PasswordViewModel
    private lateinit var mobileViewModel: MobileViewModel

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

        emailViewModel = ViewModelProvider(this)
            .get(EmailViewModel::class.java)

        nameViewModel = ViewModelProvider(this)
            .get(NameViewModel::class.java)

        passwordViewModel = ViewModelProvider(this)
            .get(PasswordViewModel::class.java)

        addressViewModel = ViewModelProvider(this)
            .get(AddressViewModel::class.java)

        mobileViewModel = ViewModelProvider(this)
            .get(MobileViewModel::class.java)

        connectionLiveData.observe(this) {
            emailViewModel.isNetworkAvailable.value = it
            nameViewModel.isNetworkAvailable.value = it
            passwordViewModel.isNetworkAvailable.value = it
            addressViewModel.isNetworkAvailable.value = it
            // MobileViewModel's network is configured in the its fragment.
        }

        isConnected.let {
            emailViewModel.isNetworkAvailable.value = it
            nameViewModel.isNetworkAvailable.value = it
            passwordViewModel.isNetworkAvailable.value = it
            addressViewModel.isNetworkAvailable.value = it
        }

        val fm = supportFragmentManager
                .beginTransaction()

        when (intent.getSerializableExtra(TARGET_FRAG)) {
            AccountRowType.EMAIL -> {
                supportActionBar?.setTitle(R.string.title_change_email)
                if (sessionManager.loadAccount()?.isVerified == false) {
                    fm.replace(R.id.update_frag_holder, RequestVerificationFragment.newInstance())
                }

                fm.replace(R.id.update_frag_holder, UpdateEmailFragment.newInstance())
            }
            AccountRowType.USER_NAME -> {
                supportActionBar?.setTitle(R.string.title_change_username)
                fm.replace(R.id.update_frag_holder, UpdateNameFragment.newInstance())
            }
            AccountRowType.PASSWORD -> {
                supportActionBar?.setTitle(R.string.title_change_password)
                fm.replace(R.id.update_frag_holder, UpdatePasswordFragment.newInstance())
            }
            AccountRowType.Address -> {
                supportActionBar?.title = "设置地址"
                fm.replace(R.id.update_frag_holder, UpdateAddressFragment.newInstance())
            }
            AccountRowType.MOBILE -> {
                supportActionBar?.title = "关联手机号码"
                fm.replace(R.id.update_frag_holder, MobileFragment.newInstanceForUpdate())
            }
        }

        fm.commit()

        setupViewModel()
    }

    private fun setupViewModel() {

        emailViewModel.progressLiveData.observe(this) {
            binding.progressing = it
        }

        nameViewModel.progressLiveData.observe(this) {
            binding.progressing = it
        }

        passwordViewModel.progressLiveData.observe(this) {
            binding.progressing = it
        }

        addressViewModel.progressLiveData.observe(this) {
            binding.progressing = it
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
    }

}
