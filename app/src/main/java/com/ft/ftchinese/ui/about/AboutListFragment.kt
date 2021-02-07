package com.ft.ftchinese.ui.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.R
import com.ft.ftchinese.model.legal.legalDocs
import com.ft.ftchinese.ui.lists.TwoLineItemViewHolder

class AboutListFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_recycler, container, false)

        view.findViewById<RecyclerView>(R.id.recycler_view).apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context).apply {
                orientation = LinearLayoutManager.VERTICAL
            }
            adapter = ListAdapter()
        }
        return view
    }

    inner class ListAdapter : RecyclerView.Adapter<TwoLineItemViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TwoLineItemViewHolder {
            return TwoLineItemViewHolder.create(parent)
        }

        override fun onBindViewHolder(holder: TwoLineItemViewHolder, position: Int) {
            holder.setLeadingIcon(null)
            holder.setPrimaryText(legalDocs[position].title)
            holder.setSecondaryText(null)

            holder.itemView.setOnClickListener {
                val action = AboutListFragmentDirections
                    .actionAboutListFragmentToLegalDetailsFragment(index = position)
                holder.itemView.findNavController().navigate(action)
            }
        }

        override fun getItemCount() = legalDocs.size
    }
}
