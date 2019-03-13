package com.ft.ftchinese

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.database.StarredArticle
import com.ft.ftchinese.viewmodel.StarArticleViewModel
import kotlinx.android.synthetic.main.fragment_recycler.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class StarredArticleFragment : Fragment(), AnkoLogger {

    private lateinit var starViewModel: StarArticleViewModel
    private lateinit var viewAdapter: StarredArticleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        info("onCreate")

        starViewModel = ViewModelProviders.of(this).get(StarArticleViewModel::class.java)

        viewAdapter = StarredArticleAdapter(listOf())

        starViewModel.getAllStarred().observe(this, Observer {
            viewAdapter.setData(it)
        })

        info("onCreate called")
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_recycler, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recycler_view.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = viewAdapter
        }

        info("onCreateView")
    }

    companion object {
        @JvmStatic
        fun newInstance() = StarredArticleFragment()
    }
}

class StarredArticleAdapter(private var articles: List<StarredArticle>) :
        RecyclerView.Adapter<StarredArticleAdapter.ViewHolder>() {

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
//            StoryActivity.start()
            if (article.isWebpage) {
                ArticleActivity.startWeb(holder.itemView.context, article.toChannelItem())
            } else {
                ArticleActivity.start(holder.itemView.context, article.toChannelItem())
            }
        }
    }

    fun setData(newData: List<StarredArticle>) {
        this.articles = newData
        notifyDataSetChanged()
    }
}
