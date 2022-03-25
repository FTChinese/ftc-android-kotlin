package com.ft.ftchinese.ui.paywall

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityPaywallBinding
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.paywall.Paywall
import com.ft.ftchinese.model.paywall.defaultPaywall
import com.ft.ftchinese.store.AccountCache
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.ui.checkout.CheckOutActivity
import com.ft.ftchinese.ui.login.AuthActivity
import com.ft.ftchinese.ui.product.ProductFragment
import com.ft.ftchinese.ui.product.ProductViewModel
import com.ft.ftchinese.util.RequestCode
import io.noties.markwon.Markwon
import org.jetbrains.anko.toast

/**
 * Paywall of products.
 */
@Deprecated("Use compose ui")
class PaywallActivity : ScopedAppActivity(),
    SwipeRefreshLayout.OnRefreshListener {

    private lateinit var cache: FileCache
    private lateinit var tracker: StatsTracker
    private lateinit var sessionManager: SessionManager
    private lateinit var productViewModel: ProductViewModel
    private lateinit var paywallViewModel: PaywallViewModel
    private lateinit var binding: ActivityPaywallBinding
    private lateinit var markwon: Markwon

    private var premiumFirst: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_paywall)
        setSupportActionBar(binding.toolbar.toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        // handle refreshing.
        binding.swipeRefresh.setOnRefreshListener(this)

        // If premium permission is required, show the premium product on top.
        premiumFirst = intent.getBooleanExtra(EXTRA_PREMIUM_FIRST, false)

        cache = FileCache(this)
        sessionManager = SessionManager.getInstance(this)
        markwon = Markwon.create(this)

        setupViewModel()
        initUI()
        loadData()

        tracker = StatsTracker.getInstance(this)
        tracker.displayPaywall()
    }

    // Load pricing data.
    private fun loadData() {
        // Fetch paywall from cache, then from server.
        paywallViewModel.loadPaywall(AccountCache.get()?.isTest ?: false)
        paywallViewModel.loadStripePrices()
    }

    private fun setupViewModel() {
        // Init viewmodels
        paywallViewModel = ViewModelProvider(this, PaywallViewModelFactory(cache))[PaywallViewModel::class.java]

        productViewModel = ViewModelProvider(this)[ProductViewModel::class.java]

        // Setup network
        connectionLiveData.observe(this) {
            paywallViewModel.isNetworkAvailable.value = it
        }
        paywallViewModel.isNetworkAvailable.value = isConnected

        productViewModel.checkoutItemSelected.observe(this) {
            val account = sessionManager.loadAccount()

            // If user is not logged in, start login.
            if (account == null) {
                AuthActivity.startForResult(this)
                return@observe
            }

            CheckOutActivity.startForResult(
                activity = this,
                requestCode = RequestCode.PAYMENT,
                item = it,
            )
        }

        /**
         * Load paywall from cache, and then from server.
         */
        paywallViewModel.paywallResult.observe(this) { result: FetchResult<Paywall> ->
            // For manual refreshing, show a toast after completion.
            val isManual = binding.swipeRefresh.isRefreshing

            binding.swipeRefresh.isRefreshing = false

            when (result) {
                is FetchResult.LocalizedError -> {
                    if (isManual) {
                        toast(getString(result.msgId))
                    }
                }
                is FetchResult.Error -> {
                    if (isManual) {
                        result.exception.message?.let { toast(it) }
                    }
                }
                is FetchResult.Success -> {

                    setPaywallData(result.data)

                    if (isManual) {
                        toast(R.string.paywall_updated)
                    }
                }
            }
        }
    }

    private fun initUI() {
        binding.premiumFirst = premiumFirst

        if (premiumFirst) {
            Log.i(TAG, "Should show premium card on top")
        }

        supportFragmentManager.commit {
            replace(R.id.subs_status, SubsStatusFragment.newInstance())

            replace(R.id.frag_promo_box, PromoBoxFragment.newInstance())

            if (premiumFirst) {
                replace(R.id.product_top, ProductFragment.newInstance(Tier.PREMIUM))
                replace(R.id.product_bottom, ProductFragment.newInstance(Tier.STANDARD))
            } else {
                replace(R.id.product_top, ProductFragment.newInstance(Tier.STANDARD))
                replace(R.id.product_bottom, ProductFragment.newInstance(Tier.PREMIUM))
            }

            replace(R.id.subs_rule_container, SubsRuleFragment.newInstance())

            // Customer service
            replace(R.id.frag_customer_service, CustomerServiceFragment.newInstance())
        }

        /**
         * Show login button, or expiration message on the SubStatusFragment.
         */
        productViewModel.accountChanged.value = sessionManager.loadAccount()

        setPaywallData(defaultPaywall)
    }

    private fun setPaywallData(pw: Paywall) {
        productViewModel.productsReceived.value = pw.products
        if (!pw.isPromoValid()) {
            binding.hasPromo = false
            return
        }

        // Display promo fragment container
        binding.hasPromo = true
        // Show promotion legal notice at bottom
        binding.promoTerms = if (pw.promo.terms != null) {
            markwon.toMarkdown(pw.promo.terms)
        } else {
            null
        }

        // Tell promo box to render.
        productViewModel.promoReceived.value = pw.promo
    }

    override fun onRefresh() {
        toast(R.string.refresh_paywall)
        paywallViewModel.refreshFtcPrice(AccountCache.get()?.isTest ?: false)
        paywallViewModel.refreshStripePrices()
    }

    // Upon payment succeeded, this activity should kill
    // itself so that user won't see it again.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // See the terrible new approach:
        // https://stackoverflow.com/questions/62671106/onactivityresult-method-is-deprecated-what-is-the-alternative
        super.onActivityResult(requestCode, resultCode, data)

        Log.i(TAG, "onActivityResult requestCode: $requestCode, resultCode: $resultCode")

        when (requestCode) {
            RequestCode.PAYMENT -> {
                if (resultCode != Activity.RESULT_OK) {
                    return
                }

                finish()
            }

            RequestCode.SIGN_IN, RequestCode.SIGN_UP -> {
                if (resultCode != Activity.RESULT_OK) {
                    return
                }

                productViewModel.accountChanged.value = sessionManager.loadAccount()

                toast(R.string.prompt_logged_in)
            }
        }
    }

    companion object {
        private const val EXTRA_PREMIUM_FIRST = "extra_premium_first"
        private const val TAG = "PaywallActivity"

        /**
         * @param premiumFirst determines whether put the premium card on top.
         */
        @JvmStatic
        fun start(context: Context?, premiumFirst: Boolean = false) {
            context?.startActivity(Intent(context, PaywallActivity::class.java).apply {
                putExtra(EXTRA_PREMIUM_FIRST, premiumFirst)
            })
        }
    }
}
