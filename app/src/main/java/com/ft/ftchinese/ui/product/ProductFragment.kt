package com.ft.ftchinese.ui.product

import android.content.Context
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentProductBinding
import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.ftcsubs.CheckoutItem
import com.ft.ftchinese.model.paywall.Product
import com.ft.ftchinese.model.paywall.defaultPaywall
import com.ft.ftchinese.model.price.Price
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.lists.MarginItemDecoration
import com.ft.ftchinese.ui.lists.SingleLineItemViewHolder
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

@kotlinx.coroutines.ExperimentalCoroutinesApi
class ProductFragment : ScopedFragment(),
        AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: ProductViewModel
    private lateinit var binding: FragmentProductBinding
    private val descListAdapter = DescListAdapter(listOf())
    private val priceListAdapter = PriceListAdapter()

    /**
     * Current product and its plans.
     */
//    private var product: Product? = null
    private lateinit var tier: Tier

    override fun onAttach(context: Context) {
        super.onAttach(context)
        sessionManager = SessionManager.getInstance(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.getParcelable<Tier>(ARG_TIER)?.let {
            tier = it
        }

        // Find the product this fragment is using.
//        product = defaultPaywall.products.find { tier == it.tier }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_product, container, false)

        binding.rvProdPrice.apply {
            layoutManager = LinearLayoutManager(context)
                .apply {
                    orientation = LinearLayoutManager.VERTICAL
                }
            adapter = priceListAdapter
        }

        binding.rvProdDesc.apply {
            layoutManager = LinearLayoutManager(context).apply {
                orientation = LinearLayoutManager.VERTICAL
            }
            addItemDecoration(MarginItemDecoration(
                topBottom = resources.getDimension(R.dimen.space_8).toInt(),
                leftRight = 0
            ))
            adapter = descListAdapter
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewModel()
    }

    private fun setupViewModel() {
        // Init view model
        viewModel = activity?.run {
            ViewModelProvider(this).get(ProductViewModel::class.java)
        } ?: throw Exception("Invalid Exception")

        // Enable or disable button
        viewModel.inputEnabled.observe(viewLifecycleOwner) {
            priceListAdapter.enabledBtn(it)
        }

        // Products received from server
        viewModel.productsReceived.observe(viewLifecycleOwner) { products ->
            val product = products.find {
                it.tier == tier
            }

            // Don't forget this step!
            initUI(product)
        }

        viewModel.productsReceived.value = defaultPaywall.products
    }

    private fun initUI(product: Product?) {
        binding.product = product

        // Update data of list adapter.
        product?.description
            ?.split("\n")
            ?.let {
                descListAdapter.setData(it)
            }

        product?.prices?.let {
            priceListAdapter.setData(it)
        }
    }

    // Buttons are disabled upon click to prevent loading
    // new activity multiple times.
    // When the fragment is resumed, enable those buttons.
    override fun onResume() {
        super.onResume()
        priceListAdapter.enabledBtn(true)
    }

    inner class PriceListAdapter : RecyclerView.Adapter<PriceItemViewHolder>() {

        private var prices = listOf<Price>()
        private var btnEnabled = true

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PriceItemViewHolder {
            return PriceItemViewHolder.create(parent)
        }

        override fun onBindViewHolder(holder: PriceItemViewHolder, position: Int) {
            val price = prices[position]

            // Find out the price's applicable offer.
            val checkout = CheckoutItem.newInstance(
                price = price,
                m = sessionManager.loadAccount()?.membership ?: Membership(),
            )

            info("Offer description ${checkout.discount?.description}")

            // Display discount's description field.
            holder.setOfferDesc(checkout.discount?.description)

            // Get the formatted price string.
            val priceText = formatPriceButton(requireContext(), checkout)

            // Display price text and handle click event.
            when (price.cycle) {
                Cycle.YEAR -> {
                    holder.setPrimaryButton(priceText, btnEnabled)
                    // Handle click on the price button.
                    holder.primaryButton.setOnClickListener {
                        viewModel.inputEnabled.value = false
//                        viewModel.priceSelected.value = price
                        viewModel.checkoutItemSelected.value = checkout
                    }
                }
                Cycle.MONTH -> {
                    holder.setSecondaryButton(priceText, btnEnabled)
                    holder.outlineButton.setOnClickListener {
                        viewModel.inputEnabled.value = false
//                        viewModel.priceSelected.value = price
                        viewModel.checkoutItemSelected.value = checkout
                    }
                }
            }
        }

        override fun getItemCount() = prices.size

        fun setData(plans: List<Price>) {
            this.prices = plans
            notifyDataSetChanged()
        }

        fun enabledBtn(enable: Boolean) {
            btnEnabled = enable
            notifyDataSetChanged()
        }
    }

    // List adapter for product description.
    inner class DescListAdapter(private var contents: List<String>) : RecyclerView.Adapter<SingleLineItemViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SingleLineItemViewHolder {
            return SingleLineItemViewHolder.create(parent)
        }

        override fun onBindViewHolder(holder: SingleLineItemViewHolder, position: Int) {
            holder.setTrailingIcon(null)
            holder.setText(SpannableString(contents[position]).apply {
                setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.colorBlack60)),
                    0,
                    length,
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE,
                )
            })
            holder.setPadding(0)
        }

        override fun getItemCount() = contents.size

        fun setData(lines: List<String>) {
            contents = lines
            notifyDataSetChanged()
        }
    }

    companion object {
        private const val ARG_TIER = "arg_tier"

        @JvmStatic
        fun newInstance(tier: Tier) = ProductFragment().apply {
            arguments = bundleOf(ARG_TIER to tier)
        }
    }
}

