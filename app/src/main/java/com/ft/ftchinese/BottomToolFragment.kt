package com.ft.ftchinese

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ft.ftchinese.database.StarredArticle
import com.ft.ftchinese.util.formatSQLDateTime
import com.ft.ftchinese.viewmodel.StarArticleViewModel
import kotlinx.android.synthetic.main.fragment_bottom_tool.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.threeten.bp.LocalDateTime

class BottomToolFragment : Fragment(),
        AnkoLogger {

    private var listener: OnItemClickListener? = null

    private lateinit var starModel: StarArticleViewModel

    private var isStarring = false
    private var starredArticle: StarredArticle? = null

    interface OnItemClickListener {
        fun onClickShareButton()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is OnItemClickListener) {
            listener = context
        }

        info("onAttach called")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        starModel = activity?.run {
            ViewModelProviders.of(this).get(StarArticleViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        // Observe article loaded data.
        starModel.starredArticle.observe(this, Observer<StarredArticle> {
            info("Observer for starred article: $it")

            starredArticle = it

            GlobalScope.launch(Dispatchers.Main) {
                val exists = starModel.isStarring(it)
                info("Found starred: $exists, $it")
                updateUIStar(exists)
            }
        })

        info("onCreate called")
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

            GlobalScope.launch(Dispatchers.Main) {
                starredArticle?.starredAt = formatSQLDateTime(LocalDateTime.now())

                info("Star/Unstar an article")

                if (isStarring) {
                    starModel.star(starredArticle)
                } else {
                    starModel.unstar(starredArticle)
                }
            }
        }

        action_share.setOnClickListener {
            listener?.onClickShareButton()
        }

        info("onViewCreated called")
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