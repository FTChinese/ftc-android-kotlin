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
import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.model.content.Following
import com.ft.ftchinese.model.content.FollowingManager
import com.ft.ftchinese.ui.lists.CardItemViewHolder
import com.ft.ftchinese.ui.lists.MarginGridDecoration
import org.jetbrains.anko.AnkoLogger

@kotlinx.coroutines.ExperimentalCoroutinesApi
class FollowingFragment : Fragment(), AnkoLogger {

    private lateinit var viewAdapter: Adapter
    private lateinit var followingManager: FollowingManager

    override fun onAttach(context: Context) {
        super.onAttach(context)

        followingManager = FollowingManager.getInstance(context)
        viewAdapter = Adapter(listOf())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_recycler, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<RecyclerView>(R.id.recycler_view)?.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = viewAdapter
            addItemDecoration(MarginGridDecoration(resources.getDimension(R.dimen.space_16).toInt(), 2))
        }

        val follows = followingManager.load()
        viewAdapter.setData(follows)
    }

    companion object {
        @JvmStatic
        fun newInstance() = FollowingFragment()
    }

    inner class Adapter(var mFollows: List<Following>) : RecyclerView.Adapter<CardItemViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardItemViewHolder {
            return CardItemViewHolder.create(parent)
        }

        override fun getItemCount() = mFollows.size

        override fun onBindViewHolder(holder: CardItemViewHolder, position: Int) {
            val item = mFollows[position]

            holder.setPrimaryText(item.tag)
            holder.setSecondaryText(null)

            holder.itemView.setOnClickListener {
                ChannelActivity.start(context, ChannelSource.ofFollowing(item))
            }
        }

        fun setData(items: List<Following>) {
            mFollows = items
            notifyDataSetChanged()
        }
    }
}
