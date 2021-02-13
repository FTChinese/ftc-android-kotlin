package com.ft.ftchinese.ui.member

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentMySubsBinding

/**
 * A simple [Fragment] subclass.
 * Use the [MySubsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MySubsFragment : Fragment() {

    private lateinit var binding: FragmentMySubsBinding
    private lateinit var viewModel: SubsStatusViewModel

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

        viewModel = activity?.run {
            ViewModelProvider(this).get(SubsStatusViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        viewModel.statusChanged.observe(viewLifecycleOwner) {
            binding.status = SubsStatus.newInstance(
                requireContext(),
                it,
            )
        }

        binding.activateAutoRenew.setOnClickListener {
            viewModel.autoRenewWanted.value = true
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
