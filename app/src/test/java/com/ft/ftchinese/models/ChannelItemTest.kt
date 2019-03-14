package com.ft.ftchinese.models

import org.junit.Test

import org.junit.Assert.*

class ChannelItemTest {

    private val item = ChannelItem(
            id="13139",
            type="interactive",
            subType=null,
            title="2019 年 1 月中国市场汽车销量继续下降",
            audioUrl=null,
            radioUrl=null,
            publishedAt=null,
            tag="FT研究院,报告,置顶,去广告,会员专享",
            webUrl="",
            isWebpage=true)

    @Test
    fun requireStandard() {
        println(item.requireStandard())
    }

    @Test
    fun requirePremium() {
    }
}