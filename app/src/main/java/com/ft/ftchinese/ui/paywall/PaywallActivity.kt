package com.ft.ftchinese.ui.paywall

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityPaywallBinding
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.paywall.Paywall
import com.ft.ftchinese.model.paywall.Promo
import com.ft.ftchinese.model.paywall.defaultPaywall
import com.ft.ftchinese.model.ftcsubs.*
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.ui.checkout.CheckOutActivity
import com.ft.ftchinese.ui.login.LoginActivity
import com.ft.ftchinese.ui.product.ProductFragment
import com.ft.ftchinese.ui.product.ProductViewModel
import com.ft.ftchinese.util.RequestCode
import com.ft.ftchinese.ui.checkout.CheckOutViewModel
import com.ft.ftchinese.viewmodel.Result
import io.noties.markwon.Markwon
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

/**
 * Paywall of products.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class PaywallActivity : ScopedAppActivity(),
    SwipeRefreshLayout.OnRefreshListener,
    AnkoLogger {

    private lateinit var cache: FileCache
    private lateinit var tracker: StatsTracker
    private lateinit var sessionManager: SessionManager
    private lateinit var checkoutViewModel: CheckOutViewModel
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
        setUpPromo(defaultPaywall.promo)
        loadData(false)

        tracker = StatsTracker.getInstance(this)
        tracker.displayPaywall()
    }

    private fun setupViewModel() {
        // Init viewmodels
        paywallViewModel = ViewModelProvider(this, PaywallViewModelFactory(cache))
            .get(PaywallViewModel::class.java)

        productViewModel = ViewModelProvider(this)
            .get(ProductViewModel::class.java)

        checkoutViewModel = ViewModelProvider(this)
            .get(CheckOutViewModel::class.java)

        // Setup network
        connectionLiveData.observe(this, {
            paywallViewModel.isNetworkAvailable.value = it
        })
        paywallViewModel.isNetworkAvailable.value = isConnected

        /**
         * When a price button in ProductFragment is clicked,
         * the selected Plan is passed.
         */
        productViewModel.priceSelected.observe(this, Observer {

            val account = sessionManager.loadAccount()

            // If user is not logged in, start login.
            if (account == null) {
                LoginActivity.startForResult(this)
                return@Observer
            }

            CheckOutActivity.startForResult(
                activity = this,
                requestCode = RequestCode.PAYMENT,
                priceId = it.id
            )
        })

        /**
         * Load paywall from cache, and then from server.
         */
        paywallViewModel.paywallResult.observe(this, { result: Result<Paywall> ->
            // For manual refreshing, show a toast after completion.
            val isManual = binding.swipeRefresh.isRefreshing

            binding.swipeRefresh.isRefreshing = false

            when (result) {
                is Result.LocalizedError -> {
                    if (isManual) {
                        toast(getString(result.msgId))
                    }
                }
                is Result.Error -> {
                    if (isManual) {
                        result.exception.message?.let { toast(it) }
                    }
                }
                is Result.Success -> {
                    info("Paywall data ${result.data}")

                    setUpPromo(result.data.promo)
                    productViewModel.productsReceived.value = result.data.products

                    if (isManual) {
                        toast(R.string.paywall_updated)
                    }
                }
            }
        })
    }

    private fun initUI() {
        binding.premiumFirst = premiumFirst

        if (premiumFirst) {
            info("Should show premium card on top")
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

            // Customer service
            replace(R.id.frag_customer_service, CustomerServiceFragment.newInstance())
        }

        markwon.setParsedMarkdown(binding.paymentGuide, markwon.toMarkdown(paywallGuide))
        /**
         * Show login button, or expiration message on the SubStatusFragment.
         */
        productViewModel.accountChanged.value = sessionManager.loadAccount()
    }

    // Load pricing data.
    private fun loadData(isRefreshing: Boolean) {
        // Fetch paywall from cache, then from server.
        paywallViewModel.loadPaywall(isRefreshing)
        paywallViewModel.refreshStripePrices()
    }

    // Convert Promo to PromoUI if it is valid.
    private fun setUpPromo(p: Promo) {
        if (!p.isValid()) {
            binding.hasPromo = false
            return
        }

        binding.hasPromo = true

        binding.promoTerms = if (p.terms != null) {
            markwon.toMarkdown(p.terms)
        } else {
            null
        }
        productViewModel.promoCreated.value = p
    }

    override fun onRefresh() {
        toast(R.string.refresh_paywall)
        loadData(true)
    }

    // Upon payment succeeded, this activity should kill
    // itself so that user won't see it again.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        info("onActivityResult requestCode: $requestCode, resultCode: $resultCode")

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
