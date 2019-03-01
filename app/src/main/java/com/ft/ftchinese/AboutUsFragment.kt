package com.ft.ftchinese

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.ft.ftchinese.user.SingleFragmentActivity
import kotlinx.android.synthetic.main.fragment_recycler.*

class AboutUsActivity : SingleFragmentActivity() {
    override fun createFragment(): androidx.fragment.app.Fragment {
        return AboutUsFragment.newInstance()
    }

    companion object {
        fun start(context: Context?) {
            val intent = Intent(context, AboutUsActivity::class.java)
            context?.startActivity(intent)
        }
    }
}

class AboutUsFragment : androidx.fragment.app.Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_recycler, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recycler_view.apply {
            setHasFixedSize(true)
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            adapter = Adapter()
        }
    }

    inner class ViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        val primaryText: TextView? = view.findViewById(R.id.primary_text)
        val secondaryText: TextView? = view.findViewById(R.id.secondary_text)
    }

    inner class Adapter : androidx.recyclerview.widget.RecyclerView.Adapter<ViewHolder>() {
        private val items = arrayOf(
                TextItem(getString(R.string.about_title1), getString(R.string.about_text1)),
                TextItem(getString(R.string.about_title2), getString(R.string.about_text2))
        )

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.list_item_title_body, parent, false)

            return ViewHolder(view)
        }

        override fun getItemCount(): Int {
            return items.size
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.primaryText?.text = item.primary
            holder.secondaryText?.text = item.secondary
        }

    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment AboutUsFragment.
         */
        @JvmStatic
        fun newInstance() = AboutUsFragment()
    }
}


data class TextItem(
        val primary: String,
        val secondary: String
)