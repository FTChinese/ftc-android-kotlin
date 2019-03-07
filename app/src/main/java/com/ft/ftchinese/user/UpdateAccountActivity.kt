package com.ft.ftchinese.user

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import com.ft.ftchinese.R
import com.ft.ftchinese.models.SessionManager
import com.ft.ftchinese.util.ClientError
import com.ft.ftchinese.util.handleApiError
import com.ft.ftchinese.util.handleException
import com.ft.ftchinese.util.isNetworkConnected
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import kotlinx.coroutines.*
import org.jetbrains.anko.toast

class UpdateAccountActivity : AppCompatActivity(),
        OnUpdateAccountListener {

    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment_single)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        val fragType = intent.getIntExtra(TARGET_FRAG, 0)

        val fragment: androidx.fragment.app.Fragment = when (fragType) {
            FRAG_EMAIL -> {
                supportActionBar?.setTitle(R.string.title_change_email)
                UpdateEmailFragment.newInstance()
            }
            FRAG_USER_NAME -> {
                supportActionBar?.setTitle(R.string.title_change_username)
                UpdateNameFragment.newInstance()
            }
            FRAG_PASSWORD -> {
                supportActionBar?.setTitle(R.string.title_change_password)
                UpdatePasswordFragment.newInstance()
            }
            else -> {
                return
            }
        }

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.single_frag_holder, fragment)
                .commit()
    }

    override fun onProgress(show: Boolean) {
        if (show) {
            progress_bar.visibility = View.VISIBLE
        } else {
            progress_bar.visibility = View.GONE
        }
    }

    override fun onUpdateAccount() {
        if (!isNetworkConnected()) {
            toast(R.string.prompt_no_network)
            return
        }

        onProgress(true)

        toast(R.string.progress_refresh_account)

        val sessionManager = SessionManager.getInstance(this)
        val account = sessionManager.loadAccount()

        if (account == null) {
            onProgress(false)
            return
        }

        job = GlobalScope.launch(Dispatchers.Main) {
            try {
                val updatedAccount = withContext(Dispatchers.IO) {
                    account.refresh()
                }

                onProgress(false)

                if (updatedAccount == null) {
                    finish()
                    return@launch
                }

                sessionManager.saveAccount(updatedAccount)

                finish()
            } catch (e: ClientError) {
                onProgress(false)

                handleApiError(e)
            } catch (e: Exception) {
                
                handleException(e)
            }
        }
    }

    companion object {
        private const val TARGET_FRAG = "extra_target_fragment"
        private const val FRAG_EMAIL = 1
        private const val FRAG_USER_NAME = 2
        private const val FRAG_PASSWORD = 3

        fun startForEmail(context: Context?) {
            context?.startActivity(
                    Intent(context, UpdateAccountActivity::class.java).apply {
                        putExtra(TARGET_FRAG, FRAG_EMAIL)
                    }
            )
        }

        fun startForUserName(context: Context?) {
            context?.startActivity(
                    Intent(context, UpdateAccountActivity::class.java).apply {
                        putExtra(TARGET_FRAG, FRAG_USER_NAME)
                    }
            )
        }

        fun startForPassword(context: Context?) {
            context?.startActivity(
                    Intent(context, UpdateAccountActivity::class.java).apply {
                        putExtra(TARGET_FRAG, FRAG_PASSWORD)
                    }
            )
        }
    }
}