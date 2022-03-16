package com.ft.ftchinese.ui.product

import android.content.Context
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
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
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.paywall.CartItemFtc
import com.ft.ftchinese.model.paywall.PaywallProduct
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.formatter.FormatHelper
import com.ft.ftchinese.ui.lists.MarginItemDecoration
import com.ft.ftchinese.ui.lists.SingleLineItemViewHolder
import org.jetbrains.anko.sdk27.coroutines.onClick

@kotlinx.coroutines.ExperimentalCoroutinesApi
class ProductFragment : ScopedFragment() {

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
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_product,
            container,
            false)

        binding.rvProdPrice.apply {
            layoutManager = LinearLayoutManager(context)
                .apply {
                    orientation = LinearLayoutManager.VERTICAL
                }
            adapter = priceListAdapter
        }

        // RecyclerView to show the the product description.
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

        // Products received from server
        viewModel.productsReceived.observe(viewLifecycleOwner) { products ->
            products.find {
                it.tier == tier
            }?.let {
                initUI(it)
            }
        }
    }

    // Set/Change product description and price buttons.
    private fun initUI(product: PaywallProduct) {
        binding.product = product

        // Update data of list adapter.
        product.descWithDailyCost()
            .split("\n")
            .let {
                descListAdapter.setData(it)
            }

        val member = sessionManager
            .loadAccount()
            ?.membership
            ?: Membership()

        val introPrice = product.introPrice(member)
        binding.introPrice = introPrice?.price
        if (introPrice != null) {
            binding.btnIntro.onClick {
                viewModel.checkoutItemSelected.value = introPrice
            }
        }

        priceListAdapter.setData(product.recurringPrices(member))

    }

    inner class PriceListAdapter : RecyclerView.Adapter<PriceItemViewHolder>() {

        private var checkoutPrices = listOf<CartItemFtc>()
        private var btnEnabled = true

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PriceItemViewHolder {
            return PriceItemViewHolder.create(parent)
        }

        override fun onBindViewHolder(holder: PriceItemViewHolder, position: Int) {
            val checkout = checkoutPrices[position]

            // Display discount's description field.
            holder.setOfferDesc(checkout.discount?.description)

            // Get the formatted price string.
            val priceText =
                FormatHelper.productPriceButton(requireContext(), checkout)

            // Display price text and handle click event.
            when {
                checkout.price.isAnnual() -> {
                    Log.i(TAG, "Set annual button")
                    holder.setPrimaryButton(priceText, btnEnabled)
                    // Handle click on the price button.
                    holder.primaryButton.setOnClickListener {
                        viewModel.checkoutItemSelected.value = checkout
                    }
                }
                checkout.price.isMonthly() -> {
                    Log.i(TAG, "Set monthly button")
                    holder.setSecondaryButton(priceText, btnEnabled)
                    holder.outlineButton.setOnClickListener {
                        viewModel.checkoutItemSelected.value = checkout
                    }
                }
            }

        }

        override fun getItemCount() = checkoutPrices.size

        fun setData(checkout: List<CartItemFtc>) {
            this.checkoutPrices = checkout
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
        private const val TAG = "ProductFragment"
        private const val ARG_TIER = "arg_tier"

        @JvmStatic
        fun newInstance(tier: Tier) = ProductFragment().apply {
            arguments = bundleOf(ARG_TIER to tier)
        }
    }
}

