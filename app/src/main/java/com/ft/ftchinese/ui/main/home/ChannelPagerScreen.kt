package com.ft.ftchinese.ui.main.home

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.viewmodel.UserViewModel

@Composable
fun ChannelPagerScreen(
    userViewModel: UserViewModel,
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
            userViewModel = userViewModel,
            scaffoldState = scaffoldState,
            channelSource = it,
        )
    }
}
