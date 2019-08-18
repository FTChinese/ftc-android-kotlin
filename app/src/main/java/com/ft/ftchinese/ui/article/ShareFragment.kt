package com.ft.ftchinese.ui.article

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.base.ShareAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_social_share.*

/**
 * Popup for share menu.
 */
class SocialShareFragment :
        BottomSheetDialogFragment() {

    private lateinit var articleViewModel: ArticleViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_social_share, container, false)


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        articleViewModel = activity?.run {
            ViewModelProvider(this)
                    .get(ArticleViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        share_rv.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context).apply {
                orientation = LinearLayoutManager.HORIZONTAL
            }

            adapter = ShareAdapter(articleViewModel)
        }
    }
}


