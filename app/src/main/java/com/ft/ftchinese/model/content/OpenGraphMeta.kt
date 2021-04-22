package com.ft.ftchinese.model.content

/**
 * Example data
 * description: ""
 * image: "http://i.ftimg.net/images/2018/10/d34dbb88088bb9761dde1732424b7e09.jpg"
 * site_name: "FT中文网"
 * title: "9 月乘用车销量增长不理想，新能源成唯一亮点"
 * type: "article"
 * webUrl: "/interactive/12781"
 */
data class OpenGraphMeta(
        // The title of your object
        val title: String,
        // The type of your object, e.g., article, video, radio, audio.
        val description: String,
        val keywords: String = "",
        var type: String = "",
        var image: String = "",
        // The canonical URL
        var url: String = ""
) {
    fun extractType(): String {
        val seg = url.split("/")
        if (seg.size > 2) {
            return seg[1]
        }

        return url
    }

    fun extractId(): String {
        val seg = url.split("/")
        if (seg.size > 3) {
            return seg[2]
        }

        return url
    }
}
