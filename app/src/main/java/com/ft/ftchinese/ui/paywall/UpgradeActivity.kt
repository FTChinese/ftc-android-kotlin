package com.ft.ftchinese.ui.paywall

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityUpgradeBinding
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.model.subscription.Plan
import com.ft.ftchinese.model.subscription.Tier
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.tracking.PaywallTracker
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.pay.UpgradePreviewActivity
import com.ft.ftchinese.util.RequestCode
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

@kotlinx.coroutines.ExperimentalCoroutinesApi
class UpgradeActivity : ScopedAppActivity(), AnkoLogger {

    private lateinit var tracker: StatsTracker
    private lateinit var sessionManager: SessionManager
    private lateinit var productViewModel: ProductViewModel
//    private var isFromArticle: Boolean = false
    private lateinit var binding: ActivityUpgradeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this,R.layout.activity_upgrade )
//        setContentView(R.layout.activity_upgrade)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        val isFromArticle = intent.getBooleanExtra(EXTRA_FROM_ARTICLE, false)
        binding.isFromArticle = isFromArticle

        sessionManager = SessionManager.getInstance(this)

        productViewModel = ViewModelProvider(this)
                .get(ProductViewModel::class.java)

        // Handle click event on price button.
        productViewModel.selected.observe(this, Observer<Plan> {
            UpgradePreviewActivity.startForResult(this, RequestCode.PAYMENT)
        })

        supportFragmentManager.beginTransaction()
                .replace(
                        R.id.frag_upgrade_premium,
                        ProductFragment.newInstance(Tier.PREMIUM)
                )
                .commit()

        tracker = StatsTracker.getInstance(this)
        tracker.displayPaywall()
        if (!isFromArticle) {
            PaywallTracker.fromUpgrade()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        info("onActivityResult requestCode: $requestCode, resultCode: $resultCode")

        when (requestCode) {
            RequestCode.PAYMENT -> {
                if (resultCode != Activity.RESULT_OK) {
                    return
                }

                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }

    companion object {
        private const val EXTRA_FROM_ARTICLE = "is_from_article"

        @JvmStatic
        fun startForResult(activity: Activity, requestCode: Int, fromArticle: Boolean = false) {
            activity.startActivityForResult(Intent(
                    activity,
                    UpgradeActivity::class.java
            ).apply {
                putExtra(EXTRA_FROM_ARTICLE, fromArticle)
            }, requestCode)
        }
    }
}
