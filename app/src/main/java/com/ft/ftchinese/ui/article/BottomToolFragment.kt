package com.ft.ftchinese.ui.article

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.database.StarredArticle
import kotlinx.android.synthetic.main.fragment_bottom_tool.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class BottomToolFragment : Fragment(),
        AnkoLogger {

    private var listener: OnItemClickListener? = null

    private var article: StarredArticle? = null
    private var isStarring = false

    private lateinit var starViewModel: StarArticleViewModel
    private lateinit var articleViewModel: ArticleViewModel

    interface OnItemClickListener {
        fun onClickShareButton()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is OnItemClickListener) {
            listener = context
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_bottom_tool, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        action_favourite.setOnClickListener {
            isStarring = !isStarring
            updateUIStar(isStarring)

            if (isStarring) {
                starViewModel.star(article)
            } else {
                starViewModel.unstar(article)
            }
        }

        action_share.setOnClickListener {
            listener?.onClickShareButton()
        }

        info("onViewCreated called")
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        starViewModel = activity?.run {
            ViewModelProvider(this).get(StarArticleViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        articleViewModel = activity?.run {
            ViewModelProvider(this)
                    .get(ArticleViewModel::class.java)
        } ?: throw Exception("Invalid Activity")


        articleViewModel.articleLoaded.observe(this, Observer {
            article = it
            starViewModel.isStarring(it)
        })

        starViewModel.starred.observe(this, Observer {
            isStarring = it
            updateUIStar(it)
        })
    }

    private fun updateUIStar(star: Boolean) {
        info("updateUIStar called")
        if (star) {
            action_favourite.setImageResource(R.drawable.ic_favorite_teal_24dp)
        } else {
            action_favourite.setImageResource(R.drawable.ic_favorite_border_teal_24dp)
        }
    }

    companion object {
        fun newInstance() = BottomToolFragment()
    }
}
