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
import kotlinx.android.synthetic.main.fragment_bottom_tool.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class BottomToolFragment : Fragment(),
        AnkoLogger {

    private var listener: OnItemClickListener? = null

    private lateinit var starModel: StarArticleViewModel

    private var isStarring = false

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
            ViewModelProvider(this).get(StarArticleViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        starModel.starred.observe(this, Observer {
            isStarring = it
            updateUIStar(it)
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

            starModel.setStarState(isStarring)
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
