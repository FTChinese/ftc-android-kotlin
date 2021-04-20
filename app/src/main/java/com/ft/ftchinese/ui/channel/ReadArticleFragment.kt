package com.ft.ftchinese.ui.channel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.R
import com.ft.ftchinese.database.ReadArticle
import com.ft.ftchinese.ui.article.ArticleActivity
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.lists.CardItemViewHolder
import com.ft.ftchinese.ui.lists.MarginItemDecoration
import com.ft.ftchinese.viewmodel.ReadArticleViewModel
import org.jetbrains.anko.AnkoLogger

@kotlinx.coroutines.ExperimentalCoroutinesApi
class ReadArticleFragment : ScopedFragment(),
        AnkoLogger {

    private lateinit var model: ReadArticleViewModel
    private lateinit var viewAdapter: ListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        model = ViewModelProvider(this).get(ReadArticleViewModel::class.java)

        viewAdapter = ListAdapter(listOf())

        model.getAllRead().observe(this) {
            viewAdapter.setData(it)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = inflater.inflate(R.layout.fragment_recycler, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<RecyclerView>(R.id.recycler_view)?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = viewAdapter
            addItemDecoration(MarginItemDecoration(resources.getDimension(R.dimen.space_8).toInt()))
        }
    }

    inner class ListAdapter(private var articles: List<ReadArticle>) :
        RecyclerView.Adapter<CardItemViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardItemViewHolder {

            return CardItemViewHolder.create(parent)
        }

        override fun getItemCount(): Int {
            return articles.size
        }

        override fun onBindViewHolder(holder: CardItemViewHolder, position: Int) {
            val article = articles[position]

            holder.setPrimaryText(article.title)
            holder.setSecondaryText(article.standfirst)

            holder.itemView.setOnClickListener {
                ArticleActivity.start(
                    context,
                    article.toTeaser()
                )
            }
        }

        fun setData(newData: List<ReadArticle>) {
            this.articles = newData
            notifyDataSetChanged()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = ReadArticleFragment()
    }
}


