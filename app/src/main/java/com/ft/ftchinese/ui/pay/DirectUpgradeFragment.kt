package com.ft.ftchinese.ui.pay

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders

import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedFragment
import kotlinx.android.synthetic.main.fragment_upgrade.*
import org.jetbrains.anko.AnkoLogger

/**
 * This is used to handle upgrading without payment,
 * e.g. user has enough balance left in account.
 * This is rarely used since there are nearly no cases that
 * balance could exactly cover the premium's price.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class DirectUpgradeFragment : ScopedFragment(),
        AnkoLogger {

    private lateinit var viewModel: CheckOutViewModel

    private fun allowInput(value: Boolean) {
        upgrade_btn?.isEnabled = value
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_upgrade, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProviders.of(this).get(CheckOutViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        // Enable or disable button
        viewModel.inputEnabled.observe(this, Observer<Boolean> {
            allowInput(it)
        })

        // Tell view model upgrading started.
//        upgrade_btn.setOnClickListener {
//            viewModel.startUpgrading(true)
//        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment DirectUpgradeFragment.
         */
        @JvmStatic
        fun newInstance() = DirectUpgradeFragment()
    }
}
