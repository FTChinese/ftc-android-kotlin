package com.ft.ftchinese.ui.base

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.R
import com.ft.ftchinese.viewmodel.ArticleViewModel
import kotlinx.android.synthetic.main.share_item.view.*

private val apps = arrayOf(
        SocialApp(
                appName = "好友",
                icon = R.drawable.wechat,
                itemId = ShareItem.WECHAT_FRIEND
        ),
        SocialApp(
                appName = "朋友圈",
                icon = R.drawable.moments,
                itemId = ShareItem.WECHAT_MOMENTS
        ),
        SocialApp(
                appName = "打开链接",
                icon = R.drawable.chrome,
                itemId = ShareItem.OPEN_IN_BROWSER
        ),
        SocialApp(
                appName = "更多",
                icon = R.drawable.ic_more_horiz_black_24dp,
                itemId = ShareItem.MORE_OPTIONS
        )
)

enum class ShareItem {
    WECHAT_FRIEND,
    WECHAT_MOMENTS,
    OPEN_IN_BROWSER,
    MORE_OPTIONS
}

data class SocialApp(
        val appName: CharSequence,
        val icon: Int,
        val itemId: ShareItem
)

class ShareAdapter(
        private val articleViewModel: ArticleViewModel
) : RecyclerView.Adapter<ShareAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val iconView: ImageView = itemView.share_icon_view
        private val textView: TextView = itemView.share_text_view

        fun bind(app: SocialApp) {
            iconView.setImageResource(app.icon)
            textView.text = app.appName
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.share_item, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount() = apps.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]

        holder.bind(app)

        holder.itemView.setOnClickListener {
            articleViewModel.share(app.itemId)

//            dismiss()
        }
    }
}
