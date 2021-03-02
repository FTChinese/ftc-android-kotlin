package com.ft.ftchinese.ui.member

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentMySubsBinding
import com.ft.ftchinese.ui.lists.TwoColItemViewHolder
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

/**
 * A simple [Fragment] subclass.
 * Use the [MySubsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MySubsFragment : Fragment(), AnkoLogger {

    private lateinit var binding: FragmentMySubsBinding
    private lateinit var viewModel: SubsStatusViewModel
    private lateinit var subsStatusAdapter: ListAdapter
    private lateinit var addOnDetailAdapter: ListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_my_subs,
            container,
            false,
        )
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        subsStatusAdapter = ListAdapter()
        addOnDetailAdapter = ListAdapter()

        viewModel = activity?.run {
            ViewModelProvider(this).get(SubsStatusViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        binding.subsDetails.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = subsStatusAdapter
        }

        binding.addonDetails.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = addOnDetailAdapter
        }

        viewModel.statusChanged.observe(viewLifecycleOwner) {
            val status = SubsStatus.newInstance(
                requireContext(),
                it,
            )
            info(status)
            binding.status = status

            // Set data to recycler view.
            subsStatusAdapter.setData(status.details)
            addOnDetailAdapter.setData(status.addOns)
        }

        binding.reactivateStripe.setOnClickListener {
            viewModel.autoRenewWanted.value = true
        }
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
            this.rows = pairs
            notifyDataSetChanged()
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         */
        @JvmStatic
        fun newInstance() =
                MySubsFragment()
    }
}
