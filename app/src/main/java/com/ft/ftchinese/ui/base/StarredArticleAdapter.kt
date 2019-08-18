package com.ft.ftchinese.ui.base

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.R
import com.ft.ftchinese.database.StarredArticle
import com.ft.ftchinese.ui.article.ArticleActivity
import kotlinx.android.synthetic.main.card_primary_secondary.view.*

@kotlinx.coroutines.ExperimentalCoroutinesApi
class StarredArticleAdapter(private val context: Context?) :
        RecyclerView.Adapter<StarredArticleAdapter.ViewHolder>() {

    private var articles = mutableListOf(StarredArticle(
            id = "banner",
            type = "notice",
            title = "关于我的收藏",
            standfirst = """用户您好。目前版本的"收藏文章"数据保存在您的本机，暂时无法与服务器同步，我们会在后续版本中开发远程同步功能。如果你需要查看在FT中文网网站或者旧版app中收藏的文章，可以点击此处打开网页，在FT中文网用户中心"收藏的文章"一栏下查看。"""
    ))

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val primaryTextView: TextView = itemView.primary_text_view
        val secondaryTextView: TextView = itemView.secondary_text_view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(
                        R.layout.card_primary_secondary,
                        parent,
                        false)

        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return articles.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val article = articles[position]

        holder.primaryTextView.text = article.title
        holder.secondaryTextView.text = article.standfirst

        holder.itemView.setOnClickListener {
            when {
                article.id == "banner" -> {
                    val webpage: Uri = Uri.parse("http://users.ftchinese.com/starred")
                    val intent = Intent(Intent.ACTION_VIEW, webpage)

                    if (context != null) {
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        }
                    }

//                    val customTabsInt = CustomTabsIntent.Builder().build()
//                    customTabsInt.launchUrl(context, Uri.parse("http://next.ftchinese.com/user/starred"))
                }
                article.isWebpage -> {
                    ArticleActivity.startWeb(holder.itemView.context, article.toChannelItem())
                }
                else -> {
                    ArticleActivity.start(holder.itemView.context, article.toChannelItem())
                }
            }
        }
    }

    fun setData(newData: List<StarredArticle>) {
        this.articles.addAll(newData)
        notifyDataSetChanged()
    }
}
