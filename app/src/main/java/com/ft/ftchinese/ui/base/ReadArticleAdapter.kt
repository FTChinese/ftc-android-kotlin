package com.ft.ftchinese.ui.base

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.R
import com.ft.ftchinese.database.ReadArticle
import com.ft.ftchinese.ui.article.ArticleActivity

@kotlinx.coroutines.ExperimentalCoroutinesApi
class ReadArticleAdapter(private var articles: List<ReadArticle>) :
        RecyclerView.Adapter<ReadArticleAdapter.ViewHolder>() {

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val primaryTextView: TextView = itemView.findViewById(R.id.primary_text_view)
        val secondaryTextView: TextView = itemView.findViewById(R.id.secondary_text_view)
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
            if (article.isWebpage) {
                ArticleActivity.startWeb(holder.itemView.context, article.toChannelItem())
            } else {
                ArticleActivity.start(holder.itemView.context, article.toChannelItem())
            }
        }
    }

    fun setData(newData: List<ReadArticle>) {
        this.articles = newData
        notifyDataSetChanged()
    }
}
