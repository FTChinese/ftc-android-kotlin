package com.ft.ftchinese

import android.support.v4.app.Fragment
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.ft.ftchinese.models.Following
import com.ft.ftchinese.models.FollowingManager
import com.ft.ftchinese.models.PagerTab
import kotlinx.android.synthetic.main.fragment_recycler.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class FollowingFragment : Fragment(), AnkoLogger {

    private var mAdapter: Adapter? = null
    private var mFollowingManager: FollowingManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            mFollowingManager = FollowingManager.getInstance(requireContext())
        } catch (e: Exception) {
            info("Cannot initiate FollowingManager")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_recycler, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recycler_view.layoutManager = GridLayoutManager(context, 3)
        updateUI()
    }

    private fun updateUI() {
        val follows = mFollowingManager?.load() ?: return

        if (mAdapter == null) {
            mAdapter = Adapter(follows)
            recycler_view.adapter = mAdapter
        } else {
            mAdapter?.setFollows(follows)
            mAdapter?.notifyDataSetChanged()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = FollowingFragment()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val primaryText: TextView = itemView.findViewById(R.id.primary_text_view)
        val secondaryText: TextView = itemView.findViewById(R.id.secondary_text_view)
    }

    inner class Adapter(var mFollows: List<Following>) : RecyclerView.Adapter<ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.card_primary_secondary, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount(): Int {
            return mFollows.size
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = mFollows[position]

            holder.primaryText?.text = item.tag
            holder.secondaryText?.visibility = View.GONE

            holder.itemView.setOnClickListener {
                val channelMeta = PagerTab(
                        title = item.tag,
                        name = "${item.type}_${item.tag}",
                        contentUrl = item.bodyUrl,
                        htmlType = PagerTab.HTML_TYPE_FRAGMENT
                )

                ChannelActivity.start(context, channelMeta)
            }
        }

        fun setFollows(items: List<Following>) {
            mFollows = items
        }
    }
}