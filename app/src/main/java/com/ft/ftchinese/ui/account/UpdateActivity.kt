package com.ft.ftchinese.ui.account

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedAppActivity
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.AnkoLogger

@kotlinx.coroutines.ExperimentalCoroutinesApi
class UpdateActivity : ScopedAppActivity(), AnkoLogger {

    private lateinit var viewModel: UpdateViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fragment_single)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        val fm = supportFragmentManager
                .beginTransaction()

        when (intent.getIntExtra(TARGET_FRAG, 0)) {
            FRAG_EMAIL -> {
                supportActionBar?.setTitle(R.string.title_change_email)
                fm.replace(R.id.single_frag_holder, UpdateEmailFragment.newInstance())
            }
            FRAG_USER_NAME -> {
                supportActionBar?.setTitle(R.string.title_change_username)
                fm.replace(R.id.single_frag_holder, UpdateNameFragment.newInstance())
            }
            FRAG_PASSWORD -> {
                supportActionBar?.setTitle(R.string.title_change_password)
                fm.replace(R.id.single_frag_holder, UpdatePasswordFragment.newInstance())
            }
        }

        fm.commit()

        viewModel = ViewModelProviders.of(this)
                .get(UpdateViewModel::class.java)

        viewModel.inProgress.observe(this, Observer<Boolean> {
            showProgress(it)
        })
    }

    private fun showProgress(show: Boolean) {
        if (show) {
            progress_bar.visibility = View.VISIBLE
        } else {
            progress_bar.visibility = View.GONE
        }
    }

    companion object {

        private const val TARGET_FRAG = "extra_target_fragment"
        private const val FRAG_EMAIL = 1
        private const val FRAG_USER_NAME = 2
        private const val FRAG_PASSWORD = 3

        @JvmStatic
        fun startForEmail(context: Context?) {
            context?.startActivity(
                    Intent(context, UpdateActivity::class.java).apply {
                        putExtra(TARGET_FRAG, FRAG_EMAIL)
                    }
            )
        }

        @JvmStatic
        fun startForUserName(context: Context?) {
            context?.startActivity(
                    Intent(context, UpdateActivity::class.java).apply {
                        putExtra(TARGET_FRAG, FRAG_USER_NAME)
                    }
            )
        }

        @JvmStatic
        fun startForPassword(context: Context?) {
            context?.startActivity(
                    Intent(context, UpdateActivity::class.java).apply {
                        putExtra(TARGET_FRAG, FRAG_PASSWORD)
                    }
            )
        }
    }

}
