package com.ft.ftchinese.ui.channel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.ui.article.ArticleActivity
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.database.ReadArticle
import com.ft.ftchinese.ui.article.ReadArticleViewModel
import kotlinx.android.synthetic.main.fragment_recycler.*
import org.jetbrains.anko.AnkoLogger

@kotlinx.coroutines.ExperimentalCoroutinesApi
class ReadArticleFragment : ScopedFragment(),
        AnkoLogger {

    private lateinit var model: ReadArticleViewModel
    private lateinit var viewAdapter: ReadArticleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        model = ViewModelProviders.of(this).get(ReadArticleViewModel::class.java)

        viewAdapter = ReadArticleAdapter(listOf())

        model.getAllRead().observe(this, Observer {
            viewAdapter.setData(it)
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = inflater.inflate(R.layout.fragment_recycler, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recycler_view.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = viewAdapter
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = ReadArticleFragment()
    }
}

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
