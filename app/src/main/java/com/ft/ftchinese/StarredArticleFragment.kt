package com.ft.ftchinese

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.base.ScopedFragment
import com.ft.ftchinese.database.StarredArticle
import com.ft.ftchinese.viewmodel.StarArticleViewModel
import kotlinx.android.synthetic.main.card_primary_secondary.view.*
import kotlinx.android.synthetic.main.fragment_recycler.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

@kotlinx.coroutines.ExperimentalCoroutinesApi
class StarredArticleFragment : ScopedFragment(),
        AnkoLogger {

    private lateinit var starViewModel: StarArticleViewModel
    private lateinit var viewAdapter: StarredArticleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        info("onCreate")

        starViewModel = ViewModelProviders.of(this).get(StarArticleViewModel::class.java)

        viewAdapter = StarredArticleAdapter(context)

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
                    val customTabsInt = CustomTabsIntent.Builder().build()
                    customTabsInt.launchUrl(context, Uri.parse("http://next.ftchinese.com/user/starred"))
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
