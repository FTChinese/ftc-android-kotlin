package com.ft.ftchinese.ui.web

import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.model.content.Following
import com.ft.ftchinese.model.content.Teaser

interface JsEventListener {
    fun onClosePage()
    fun onProgress(loading: Boolean)
    fun onAlert(message: String)
    fun onTeasers(teasers: List<Teaser>)
    fun onClickTeaser(teaser: Teaser)
    fun onClickChannel(source: ChannelSource)
    fun onFollowTopic(following: Following)
}


