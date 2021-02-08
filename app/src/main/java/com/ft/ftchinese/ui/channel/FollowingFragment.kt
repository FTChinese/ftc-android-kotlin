package com.ft.ftchinese.ui.channel

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.R
import com.ft.ftchinese.model.content.Following
import com.ft.ftchinese.model.content.FollowingManager
import com.ft.ftchinese.model.content.buildFollowChannel
import com.ft.ftchinese.ui.lists.CardItemViewHolder
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

    inner class Adapter(var mFollows: List<Following>) : RecyclerView.Adapter<CardItemViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardItemViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.list_item_card, parent, false)
            return CardItemViewHolder.create(parent)
        }

        override fun getItemCount() = mFollows.size

        override fun onBindViewHolder(holder: CardItemViewHolder, position: Int) {
            val item = mFollows[position]

            holder.setPrimaryText(item.tag)
            holder.setSecondaryText(null)

            holder.itemView.setOnClickListener {
                ChannelActivity.start(context, buildFollowChannel(item))
            }
        }

        fun setFollows(items: List<Following>) {
            mFollows = items
        }
    }
}
