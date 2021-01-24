package com.ft.ftchinese.ui.about

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.R
import com.ft.ftchinese.model.legal.legalDocs
import com.ft.ftchinese.ui.lists.SingleLineItemViewHolder

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [AboutListFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AboutListFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_about_list, container, false)

        view.findViewById<RecyclerView>(R.id.list_rv).apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context).apply {
                orientation = LinearLayoutManager.VERTICAL
            }

            adapter = ListAdapter()
        }
        return view
    }

    inner class ListAdapter : RecyclerView.Adapter<SingleLineItemViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SingleLineItemViewHolder {
            return SingleLineItemViewHolder.create(parent)
        }

        override fun onBindViewHolder(holder: SingleLineItemViewHolder, position: Int) {
            holder.icon.visibility = View.GONE
            holder.text.text = legalDocs[position].title

            holder.itemView.setOnClickListener {
                val action = AboutListFragmentDirections
                    .actionAboutListFragmentToLegalDetailsFragment(index = position)
                holder.itemView.findNavController().navigate(action)
            }
        }

        override fun getItemCount() = legalDocs.size
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            AboutListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
