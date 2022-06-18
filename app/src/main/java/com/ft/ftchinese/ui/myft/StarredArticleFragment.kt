package com.ft.ftchinese.ui.myft

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.ui.article.ArticleActivity
import com.ft.ftchinese.ui.components.ArticleItem
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OTheme

@Deprecated("")
class StarredArticleFragment : Fragment() {

    private lateinit var starViewModel: StarArticleViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        starViewModel = ViewModelProvider(this).get(StarArticleViewModel::class.java)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                OTheme {
                    StarredArticleScreen(starredViewModel = starViewModel)
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = StarredArticleFragment()
    }
}

@Composable
private fun StarredArticleScreen(
    starredViewModel: StarArticleViewModel
) {
    val context = LocalContext.current

    val listItems by starredViewModel.getAllStarred().observeAsState(listOf())

    LazyColumn(
        modifier = Modifier.padding(Dimens.dp8),
    ) {
        items(listItems) { teaser ->
            ArticleItem(
                heading = teaser.title,
                subHeading = teaser.standfirst,
                onClick = {
                    ArticleActivity.start(context, teaser.toTeaser())
                }
            )
            Spacer(modifier = Modifier.height(Dimens.dp8))
        }
    }
}
