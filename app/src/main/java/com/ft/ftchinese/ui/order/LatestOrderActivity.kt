package com.ft.ftchinese.ui.order

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityLatestOrderBinding
import com.ft.ftchinese.store.OrderManager
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.member.MemberActivity

/**
 * [LatestOrderActivity] shows the payment result of alipay of wxpay.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class LatestOrderActivity : ScopedAppActivity() {

    lateinit var binding: ActivityLatestOrderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_latest_order)

        setSupportActionBar(binding.toolbar.toolbar)
        supportActionBar?.apply {
//            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        val orderManager = OrderManager.getInstance(this)

        binding.btnSubscriptionDone.setOnClickListener {
            MemberActivity.start(this)
            finish()
        }

        val order = orderManager.load() ?: return

        binding.rvLastOrder.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@LatestOrderActivity)
            adapter = OrderAdapter(listOf(order))
        }
    }

    companion object {
        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, LatestOrderActivity::class.java))
        }
    }
}
