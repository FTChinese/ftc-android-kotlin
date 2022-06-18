package com.ft.ftchinese.ui.main.home

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import com.ft.ftchinese.model.content.ChannelSource

@Composable
fun ChannelPagerScreen(
    scaffoldState: ScaffoldState,
    channelSources: List<ChannelSource>,
    onTabSelected: (ChannelSource) -> Unit
) {

    ChannelPagerLayout(
        pages = channelSources,
        onTabSelected = {
            channelSources.getOrNull(it)?.let(onTabSelected)
        }
    ) {
        ChannelTabScreen(
            scaffoldState = scaffoldState,
            channelSource = it,
        )
    }
}
