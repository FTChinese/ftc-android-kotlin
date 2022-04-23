package com.ft.ftchinese.ui.webpage

import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.model.content.OpenGraphMeta
import com.ft.ftchinese.ui.base.Paging

sealed class WVEvent {
    // Event specific to article page
    data class OpenGraph(val og: OpenGraphMeta): WVEvent()

    data class ChannelPage(val source: ChannelSource): WVEvent()
    // Event specific to
    data class Pagination(val paging: Paging): WVEvent()
}
