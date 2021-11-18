package com.ft.ftchinese.ui.checkout

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityLatestInvoiceBinding
import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.OrderKind
import com.ft.ftchinese.model.invoice.Invoice
import com.ft.ftchinese.store.InvoiceStore
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.formatter.FormatHelper
import com.ft.ftchinese.ui.lists.TwoColItemViewHolder
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.sdk27.coroutines.onClick

/**
 * [LatestInvoiceActivity] shows the payment result of alipay of wxpay.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class LatestInvoiceActivity : ScopedAppActivity(), AnkoLogger {

    private lateinit var binding: ActivityLatestInvoiceBinding
    private lateinit var listAdapter: ListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_latest_invoice,
        )

        setSupportActionBar(binding.toolbar.toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(true)
        }

        binding.btnSubscriptionDone.onClick {
            BuyerInfoActivity.start(this@LatestInvoiceActivity)
            finish()
        }

        listAdapter = ListAdapter()

        binding.rvLastInvoice.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@LatestInvoiceActivity)
            adapter = listAdapter
        }

        InvoiceStore.getInstance(this).loadInvoices()?.let {
            listAdapter.setData(purchasedInvoiceRows(it.purchased))

            if (it.carriedOver != null) {
                binding.carriedOver = "购买前会员剩余 ${it.carriedOver.totalDays} 天，将在当前会员到期后继续使用"
            }
        }
    }

    private fun purchasedInvoiceRows(inv: Invoice): List<Pair<String, String>> {
        return listOf(
            Pair(
                "订单号",
                inv.orderId ?: "",
            ),
            Pair(
                "订阅方案",
                FormatHelper.formatEdition(this, inv.edition),
            ),
            Pair(
                "支付金额",
                FormatHelper.formatPrice(this, inv.currency, inv.paidAmount),
            ),
            Pair(
                "支付方式",
                inv.payMethod?.let {
                    FormatHelper.getPayMethod(this, it)
                } ?: "",
            ),
            Pair(
                "订阅期限",
                if (inv.orderKind == OrderKind.AddOn) {
                    when {
                        inv.years > 0 -> FormatHelper.getCycleN(this, Cycle.YEAR, inv.years)
                        inv.months > 0 -> FormatHelper.getCycleN(this, Cycle.MONTH, inv.months)
                        else -> FormatHelper.getCycleN(this, inv.cycle, 1)
                    } + "(当前订阅过期后启用)"
                } else {
                    "${inv.startUtc?.toLocalDate()} 至 ${inv.endUtc?.toLocalDate()}"
                }
            )
        )
    }

    inner class ListAdapter : RecyclerView.Adapter<TwoColItemViewHolder>() {
        private var rows: List<Pair<String, String>> = listOf()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TwoColItemViewHolder {
            return TwoColItemViewHolder.create(parent)
        }

        override fun onBindViewHolder(holder: TwoColItemViewHolder, position: Int) {
            val pair = rows[position]
            holder.setLeadingText(pair.first)
            holder.setTrailingText(pair.second)
        }

        override fun getItemCount() = rows.size

        fun setData(pairs: List<Pair<String, String>>) {
            info("Recycle view data $pairs")
            this.rows = pairs
            notifyDataSetChanged()
        }
    }

    companion object {
        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, LatestInvoiceActivity::class.java))
        }
    }
}
