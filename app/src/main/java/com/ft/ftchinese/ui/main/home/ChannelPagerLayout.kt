package com.ft.ftchinese.ui.main.home

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OColor
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.pagerTabIndicatorOffset
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class)
@Composable
fun ChannelPagerLayout(
    pages: List<ChannelSource>,
    onTabSelected: (Int) -> Unit = {},
    content: @Composable (ChannelSource) -> Unit,
) {

    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState()

    LaunchedEffect(key1 = pagerState.currentPage) {
        Log.i("Channel", "Current tab changed ${pagerState.currentPage}")
        onTabSelected(pagerState.currentPage)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            backgroundColor = OColor.wheat,
            edgePadding = Dimens.zero,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.pagerTabIndicatorOffset(pagerState, tabPositions)
                )
            }
        ) {

            pages.forEachIndexed { index, tab ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        scope.launch {
                            pagerState.scrollToPage(index)
                        }
                    },
                    // Equivalent of fun getPageTitle(position: Int): CharSequence
                    text = {
                        Text(text = tab.title)
                    }
                )
            }
        }

        HorizontalPager(
            count = pages.size,
            state = pagerState
        ) { tabIndex ->

            Log.i("Channel", "HorizontalPager display $tabIndex")
            // Equivalent of fun getItem(position: Int): Fragment
            val currentPage = pages[tabIndex]

            content(currentPage)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMyArticleTabs() {

}
