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
import com.ft.ftchinese.database.ReadArticle
import com.ft.ftchinese.ui.article.ArticleActivity
import com.ft.ftchinese.ui.components.ArticleItem
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OTheme

@Deprecated("")
class ReadArticleFragment : Fragment() {

    private lateinit var model: ReadArticleViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        model = ViewModelProvider(this)[ReadArticleViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                OTheme {
                    ReadArticleScreen(readViewModel = model)
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = ReadArticleFragment()
    }
}

@Composable
private fun ReadArticleScreen(
    readViewModel: ReadArticleViewModel
) {
    val context = LocalContext.current

    val listItems by readViewModel
        .getAllRead()
        .observeAsState(listOf())

    TeaserList(
        teasers = listItems,
        onClick = {
            ArticleActivity.start(
                context,
                it.toTeaser()
            )
        }
    )
}

@Composable
private fun TeaserList(
    teasers: List<ReadArticle>,
    onClick: (ReadArticle) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.padding(Dimens.dp8),
    ) {
        items(teasers) { teaser ->
            ArticleItem(
                heading = teaser.title,
                subHeading = teaser.standfirst,
                onClick = {
                    onClick(teaser)
                }
            )
            Spacer(modifier = Modifier.height(Dimens.dp8))
        }
    }
}

