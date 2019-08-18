package com.ft.ftchinese.ui.channel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.article.StarArticleViewModel
import com.ft.ftchinese.ui.base.StarredArticleAdapter
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

        starViewModel = ViewModelProvider(this).get(StarArticleViewModel::class.java)

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


