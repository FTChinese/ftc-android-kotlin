package com.ft.ftchinese.ui.article

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_social_share.*

private val apps = arrayOf(
        SocialApp(
                appName = "好友",
                icon = R.drawable.wechat,
                id = SocialApp.WECHAT_FRIEND,
                itemId = ShareItem.WECHAT_FRIEND
        ),
        SocialApp(
                appName = "朋友圈",
                icon = R.drawable.moments,
                id = SocialApp.WECHAT_MOMOMENTS,
                itemId = ShareItem.WECHAT_MOMOMENTS
        ),
        SocialApp(
                appName = "打开链接",
                icon = R.drawable.chrome,
                id = SocialApp.OPEN_IN_BROWSER,
                itemId = ShareItem.OPEN_IN_BROWSER
        ),
        SocialApp(
                appName = "更多",
                icon = R.drawable.ic_more_horiz_black_24dp,
                id = SocialApp.MORE_OPTIONS,
                itemId = ShareItem.MORE_OPTIONS
        )
)

enum class ShareItem {
    WECHAT_FRIEND,
    WECHAT_MOMOMENTS,
    OPEN_IN_BROWSER,
    MORE_OPTIONS
}

data class SocialApp(
        val appName: CharSequence,
        val icon: Int,
        val id: Int,
        val itemId: ShareItem
) {
    companion object {
        const val WECHAT_FRIEND = 1
        const val WECHAT_MOMOMENTS = 2
        const val OPEN_IN_BROWSER = 3
        const val MORE_OPTIONS = 4
    }
}

/**
 * Popup for share menu.
 */
class SocialShareFragment :
        BottomSheetDialogFragment() {

    private var listener: OnShareListener? = null

    interface OnShareListener {
        fun onClickShareIcon(item: ShareItem)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is OnShareListener) {
            listener = context
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_social_share, container, false)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        share_rv.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context).apply {
                orientation = LinearLayoutManager.HORIZONTAL
            }

            adapter = ShareAdapter()
        }

    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconView: ImageView = itemView.findViewById(R.id.share_icon_view)
        val textView: TextView = itemView.findViewById(R.id.share_text_view)
    }

    inner class ShareAdapter : RecyclerView.Adapter<ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.list_item_share, parent, false)

            return ViewHolder(view)
        }

        override fun getItemCount(): Int {
            return apps.size
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = apps[position]

            holder.iconView.setImageResource(app.icon)
            holder.textView.text = app.appName

            holder.itemView.setOnClickListener {
                listener?.onClickShareIcon(app.itemId)

                dismiss()
            }
        }
    }
}
