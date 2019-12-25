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
import com.ft.ftchinese.viewmodel.ReadArticleViewModel
import com.ft.ftchinese.ui.base.ReadArticleAdapter
import kotlinx.android.synthetic.main.fragment_recycler.*
import org.jetbrains.anko.AnkoLogger

@kotlinx.coroutines.ExperimentalCoroutinesApi
class ReadArticleFragment : ScopedFragment(),
        AnkoLogger {

    private lateinit var model: ReadArticleViewModel
    private lateinit var viewAdapter: ReadArticleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        model = ViewModelProvider(this).get(ReadArticleViewModel::class.java)

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


