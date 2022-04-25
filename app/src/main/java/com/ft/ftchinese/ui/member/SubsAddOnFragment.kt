package com.ft.ftchinese.ui.member

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentSubsAddOnBinding


/**
 * Used to show addon details inside [MemberActivity]
 */
class SubsAddOnFragment : Fragment() {

    private lateinit var binding: FragmentSubsAddOnBinding
    private lateinit var viewModel: SubsStatusViewModel
    private lateinit var listAdapter: SubsDetailListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_subs_add_on,
            container,
            false
        )
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = activity?.run {
            ViewModelProvider(this)[SubsStatusViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        initUI()
        setupViewModel()
    }

    private fun initUI() {
        listAdapter = SubsDetailListAdapter()

        binding.subsAddonDetails.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = listAdapter
        }
    }

    private fun setupViewModel() {
        viewModel.statusChanged.observe(viewLifecycleOwner) {
            listAdapter.setData(it.addOns)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = SubsAddOnFragment()
    }
}
