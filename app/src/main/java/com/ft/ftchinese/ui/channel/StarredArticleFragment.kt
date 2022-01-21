package com.ft.ftchinese.ui.channel

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.R
import com.ft.ftchinese.database.StarredArticle
import com.ft.ftchinese.ui.article.ArticleActivity
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.lists.CardItemViewHolder
import com.ft.ftchinese.ui.lists.MarginItemDecoration
import com.ft.ftchinese.viewmodel.StarArticleViewModel

@kotlinx.coroutines.ExperimentalCoroutinesApi
class StarredArticleFragment : ScopedFragment() {

    private lateinit var starViewModel: StarArticleViewModel
    private lateinit var viewAdapter: ListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate")

        starViewModel = ViewModelProvider(this).get(StarArticleViewModel::class.java)

        viewAdapter = ListAdapter(mutableListOf(
            StarredArticle(
                id = "banner",
                type = "notice",
                title = "关于我的收藏",
                standfirst = """用户您好。目前版本的"收藏文章"数据保存在您的本机，暂时无法与服务器同步，我们会在后续版本中开发远程同步功能。如果你需要查看在FT中文网网站或者旧版app中收藏的文章，可以点击此处打开网页，在FT中文网用户中心"收藏的文章"一栏下查看。"""
            )
        ))

        starViewModel.getAllStarred().observe(this) {
            viewAdapter.addData(it)
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_recycler, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<RecyclerView>(R.id.recycler_view)?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = viewAdapter
            addItemDecoration(MarginItemDecoration(resources.getDimension(R.dimen.space_8).toInt()))
        }
    }

    inner class ListAdapter(private var articles: MutableList<StarredArticle>) :
        RecyclerView.Adapter<CardItemViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardItemViewHolder {
            return CardItemViewHolder.create(parent)
        }

        override fun getItemCount() = articles.size

        override fun onBindViewHolder(holder: CardItemViewHolder, position: Int) {
            val article = articles[position]

            holder.setPrimaryText(article.title)
            holder.setSecondaryText(article.standfirst)

            holder.itemView.setOnClickListener {
                when (article.id) {
                    "banner" -> {
                        val webpage: Uri = Uri.parse("http://users.ftchinese.com/starred")
                        val intent = Intent(Intent.ACTION_VIEW, webpage)

                        if (intent.resolveActivity(holder.itemView.context.packageManager) != null) {
                            holder.itemView.context.startActivity(intent)
                        }
                    }
                    else -> {
                        ArticleActivity.start(holder.itemView.context, article.toTeaser())
                    }
                }
            }
        }

        fun addData(newData: List<StarredArticle>) {
            this.articles.addAll(newData)
            notifyDataSetChanged()
        }
    }

    companion object {
        private const val TAG = "StarredArticleFragment"

        @JvmStatic
        fun newInstance() = StarredArticleFragment()
    }
}


