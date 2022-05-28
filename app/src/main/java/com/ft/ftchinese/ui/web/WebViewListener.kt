package com.ft.ftchinese.ui.web

import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.model.content.OpenGraphMeta
import com.ft.ftchinese.ui.base.Paging

interface WebViewListener {
    // After article page executed js to collect open graph meta data
    fun onOpenGraph(openGraph: OpenGraphMeta)
    // When a link in web view point to a channel
    fun onChannelSelected(source: ChannelSource)
    // When pagination link in a channel page is clicked
    fun onPagination(paging: Paging)
}
