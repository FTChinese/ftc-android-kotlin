package com.ft.ftchinese.ui.product

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentProductBinding
import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.subscription.*
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.formatter.buildPlanPrice
import com.ft.ftchinese.ui.formatter.formatPriceCycle
import com.ft.ftchinese.ui.lists.SingleLineItemViewHolder
import org.jetbrains.anko.AnkoLogger

@kotlinx.coroutines.ExperimentalCoroutinesApi
class ProductFragment : ScopedFragment(),
        AnkoLogger {

    private lateinit var viewModel: ProductViewModel
    private lateinit var binding: FragmentProductBinding
    private val descListAdapter = DescListAdapter(listOf())
    private val priceListAdapter = PriceListAdapter()

    /**
     * Current product and its plans.
     */
    private var product: Product? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tier = arguments?.getParcelable<Tier>(ARG_TIER)

        // Find the product this fragment is using.
        product = defaultPaywall.products.find { tier == it.tier }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_product, container, false)

        binding.rvProdDesc.apply {
            layoutManager = LinearLayoutManager(context).apply {
                orientation = LinearLayoutManager.VERTICAL
            }
            adapter = descListAdapter
        }

        binding.rvProdPrice.apply {
            layoutManager = LinearLayoutManager(context)
                .apply {
                    orientation = LinearLayoutManager.VERTICAL
                }
            adapter = priceListAdapter
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()
    }

    private fun initUI() {
        binding.product = product

        // Update data of list adapter.
        product?.description
            ?.split("\n")
            ?.let {
                descListAdapter.setData(it)
            }

        product?.plans?.let {
            priceListAdapter.setData(it)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

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
            product = products.find {
                it.tier == product?.tier
            }
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

        private var plans = listOf<Plan>()
        private var btnEnabled = true

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PriceItemViewHolder {
            return PriceItemViewHolder.create(parent)
        }

        override fun onBindViewHolder(holder: PriceItemViewHolder, position: Int) {
            val plan = plans[position]

            val price = buildPlanPrice(requireContext(), plan)

            if (price.original != null) {
                holder.text.text = price.original
            } else {
                holder.text.visibility = View.GONE
            }

            when (plan.cycle) {
                Cycle.YEAR -> {
                    holder.outlineButton.visibility = View.GONE
                    holder.primaryButton.text = price.payable
                    holder.primaryButton.isEnabled = btnEnabled
                    holder.primaryButton.setOnClickListener {
                        viewModel.inputEnabled.value = false
                        viewModel.planSelected.value = plan
                    }
                }
                Cycle.MONTH -> {
                    holder.primaryButton.visibility = View.GONE
                    holder.outlineButton.text = price.payable
                    holder.outlineButton.isEnabled = btnEnabled
                    holder.outlineButton.setOnClickListener {
                        viewModel.inputEnabled.value = false
                        viewModel.planSelected.value = plan
                    }
                }
            }
        }

        override fun getItemCount() = plans.size

        fun setData(plans: List<Plan>) {
            this.plans = plans
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
            holder.icon.setImageResource(R.drawable.ic_done_gray_24dp)
            holder.disclosure.visibility = View.GONE
            holder.text.text = contents[position]
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

