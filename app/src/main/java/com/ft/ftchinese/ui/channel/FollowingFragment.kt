package com.ft.ftchinese.ui.channel

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.R
import com.ft.ftchinese.model.content.*
import org.jetbrains.anko.AnkoLogger

@kotlinx.coroutines.ExperimentalCoroutinesApi
class FollowingFragment : Fragment(), AnkoLogger {

    private var adapter: Adapter? = null
    private lateinit var followingManager: FollowingManager
    private var rv: RecyclerView? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        followingManager = FollowingManager.getInstance(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_recycler, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rv = view.findViewById(R.id.recycler_view)
        rv?.layoutManager = GridLayoutManager(context, 3)
        updateUI()
    }

    private fun updateUI() {
        val follows = followingManager.load()

        if (adapter == null) {
            adapter = Adapter(follows)
            rv?.adapter = adapter
        } else {
            adapter?.setFollows(follows)
            adapter?.notifyDataSetChanged()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = FollowingFragment()
    }

    inner class ViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        val primaryText: TextView = itemView.findViewById(R.id.primary_text_view)
        val secondaryText: TextView = itemView.findViewById(R.id.secondary_text_view)
    }

    inner class Adapter(var mFollows: List<Following>) : androidx.recyclerview.widget.RecyclerView.Adapter<ViewHolder>() {
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

            holder.primaryText.text = item.tag
            holder.secondaryText.visibility = View.GONE

            holder.itemView.setOnClickListener {

                ChannelActivity.start(context, buildFollowChannel(item))
            }
        }

        fun setFollows(items: List<Following>) {
            mFollows = items
        }
    }
}
