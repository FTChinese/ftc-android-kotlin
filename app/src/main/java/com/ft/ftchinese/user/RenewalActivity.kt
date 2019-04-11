package com.ft.ftchinese.user

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.ft.ftchinese.R
import com.ft.ftchinese.models.Cycle
import com.ft.ftchinese.models.Tier
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.AnkoLogger

@kotlinx.coroutines.ExperimentalCoroutinesApi
class RenewalActivity :
        AppCompatActivity(),
        OnProgressListener,
        AnkoLogger {

    override fun onProgress(show: Boolean) {
        if (show) {
            progress_bar.visibility = View.VISIBLE
        } else {
            progress_bar.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment_single)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        val tierStr = intent.getStringExtra(EXTRA_MEMBER_TIER)
        val cycleStr = intent.getStringExtra(EXTRA_BILLING_CYCLE)

        val fragment = CheckOutFragment.newInstance(tierStr, cycleStr)

        supportFragmentManager.beginTransaction()
                .replace(R.id.single_frag_holder, fragment)
                .commit()
    }

    companion object {
        fun startForResult(activity: Activity?, requestCode: Int, tier: Tier, cycle: Cycle) {
            val intent = Intent(activity, RenewalActivity::class.java).apply {
                putExtra(EXTRA_MEMBER_TIER, tier.string())
                putExtra(EXTRA_BILLING_CYCLE, cycle.string())
            }

            activity?.startActivityForResult(intent, requestCode)
        }
    }
}
