package com.ft.ftchinese.ui.share

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Popup for share menu.
 */
class SocialShareFragment :
        BottomSheetDialogFragment() {

    private lateinit var viewModel: SocialShareViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_social_share, container, false)

        view.findViewById<RecyclerView>(R.id.share_rv).apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context).apply {
                orientation = LinearLayoutManager.HORIZONTAL
            }
            adapter = SocialShareAdapter()
        }

        return view
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProvider(this)
                    .get(SocialShareViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

    }

    inner class SocialShareAdapter : RecyclerView.Adapter<SocialShareViewHolder>() {

        private val socialApps = arrayOf(
            SocialApp(
                name = "好友",
                icon = R.drawable.wechat,
                id = SocialAppId.WECHAT_FRIEND
            ),
            SocialApp(
                name = "朋友圈",
                icon = R.drawable.moments,
                id = SocialAppId.WECHAT_MOMENTS
            ),
            SocialApp(
                name = "打开链接",
                icon = R.drawable.chrome,
                id = SocialAppId.OPEN_IN_BROWSER
            ),
            SocialApp(
                name = "更多",
                icon = R.drawable.ic_more_horiz_black_24dp,
                id = SocialAppId.MORE_OPTIONS
            )
        )

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SocialShareViewHolder {
            return SocialShareViewHolder.create(parent)
        }

        override fun onBindViewHolder(holder: SocialShareViewHolder, position: Int) {
            val app = socialApps[position]

            holder.icon.setImageResource(app.icon)
            holder.text.text = app.name

            holder.itemView.setOnClickListener {
                viewModel.select(app)
            }
        }

        override fun getItemCount() = socialApps.size
    }
}


