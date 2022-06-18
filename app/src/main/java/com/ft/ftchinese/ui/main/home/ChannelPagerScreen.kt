package com.ft.ftchinese.ui.main.home

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import com.ft.ftchinese.model.content.ChannelSource

@Composable
fun ChannelPagerScreen(
    scaffoldState: ScaffoldState,
    channelSource: List<ChannelSource>
) {
    ChannelPagerLayout(
        pages = channelSource
    ) {
        ChannelTabScreen(
            scaffoldState = scaffoldState,
            channelSource = it,

        )
    }
}
