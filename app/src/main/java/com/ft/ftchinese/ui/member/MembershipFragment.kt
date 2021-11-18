package com.ft.ftchinese.ui.member

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentMembershipBinding
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

/**
 * UI to show current membership's status.
 */
class MembershipFragment : Fragment(), AnkoLogger {

    private lateinit var binding: FragmentMembershipBinding
    private lateinit var viewModel: SubsStatusViewModel
    private lateinit var listAdapter: SubsDetailListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_membership,
            container,
            false,
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = activity?.run {
            ViewModelProvider(this).get(SubsStatusViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        initUI()
        setupViewModel()
    }

    private fun initUI() {
        listAdapter = SubsDetailListAdapter()
        binding.subsDetails.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = listAdapter
        }

        // Reactivate a scheduled Stripe cancellation.
        binding.reactivateStripe.setOnClickListener {
            viewModel.reactivateStripeRequired.value = true
        }
    }

    private fun setupViewModel() {
        viewModel.statusChanged.observe(viewLifecycleOwner) {

            info(it)
            binding.status = it

            // Set data to recycler view.
            listAdapter.setData(it.details)
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         */
        @JvmStatic
        fun newInstance() = MembershipFragment()
    }
}
