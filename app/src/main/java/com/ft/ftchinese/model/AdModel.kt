package com.ft.ftchinese.model

data class AdModel(
        val imageString: String,
        val link: String,
        val video: String,
        val impression: Array<Impression>,
        val headline: String,
        val adName: String,
        val bgColor: String,
        val lead: String,
        val adCode: String
)

data class Impression(
        val urlString: String,
        val adName: String
)
enum class AdPosition(val position: String) {
    TOP_BANNER("Top-Banner"),
    MIDDLE_ONE("Middle-1"),
    MIDDLE_TWO("Middle-2"),
    RIGHT_ONE("Right-1"),
    RIGHT_TWO("Right-2"),
    BOTTOM_BANNER("Bottom-Banner")
}

object AdParser {
    fun getAdCode(position: AdPosition): String {

        val adCode: String = when (position) {
            AdPosition.TOP_BANNER -> """
                    <div class="o-ads" data-o-ads-name="banner1"
                    data-o-ads-center="true"
                    data-o-ads-formats-default="false"
                    data-o-ads-formats-small="FtcMobileBanner"
                    data-o-ads-formats-medium="false"
                    data-o-ads-formats-large="FtcLeaderboard"
                    data-o-ads-formats-extra="FtcLeaderboard"
                    data-o-ads-targeting="cnpos=top1;">
                    </div>
                """.trimIndent()

            AdPosition.MIDDLE_ONE -> """
                    <div  data-o-ads-name="mpu-middle1" class="o-ads" data-o-ads-formats-default="false"  data-o-ads-formats-small="FtcMobileMpu"  data-o-ads-formats-medium="false" data-o-ads-formats-large="FtcMobileMpu" data-o-ads-formats-extra="FtcMobileMpu" data-o-ads-targeting="cnpos=middle1;"></div>
                """.trimIndent()

            AdPosition.MIDDLE_TWO -> """
                    <div data-o-ads-name="mpu-middle2" class="o-ads" data-o-ads-formats-default="false"  data-o-ads-formats-small="FtcMobileMpu"  data-o-ads-formats-medium="false" data-o-ads-formats-large="FtcMobileMpu" data-o-ads-formats-extra="FtcMobileMpu" data-o-ads-targeting="cnpos=middle2;"></div>
                """.trimIndent()

            AdPosition.RIGHT_ONE -> """
                    <div
                data-o-ads-name="mpu-right1"
                class="o-ads"
                data-o-ads-formats-default="false"
                data-o-ads-formats-small="false"
                data-o-ads-formats-medium="FtcMobileMpu"
                data-o-ads-formats-large="FtcMobileMpu"
                data-o-ads-targeting="cnpos=right1;">
                </div>
                """.trimIndent()

            AdPosition.RIGHT_TWO -> """
                    <div
                data-o-ads-name="mpu-right2"
                class="o-ads"
                data-o-ads-formats-default="false"
                data-o-ads-formats-small="false"
                data-o-ads-formats-medium="FtcMobileMpu"
                data-o-ads-formats-large="FtcMobileMpu"
                data-o-ads-targeting="cnpos=right2;">
                </div>
                """.trimIndent()

            AdPosition.BOTTOM_BANNER -> """
                    <div class="o-ads" data-o-ads-name="banner2"
                data-o-ads-center="true"
                data-o-ads-formats-default="false"
                data-o-ads-formats-small="FtcMobileBanner"
                data-o-ads-formats-medium="false"
                data-o-ads-formats-large="FtcBanner"
                data-o-ads-formats-extra="FtcBanner"
                data-o-ads-targeting="cnpos=top2;">
                </div>
                """.trimIndent()
        }


        return adCode.replace(Regex("[\n\r]+"), "")
    }

    fun updateAdCode(html: String, hideAd: Boolean): String {
        var newText = html
        enumValues<AdPosition>().forEach {
            val adCode =  if (hideAd) "" else getAdCode(it)
            newText = newText.replace("{${it.position}}", adCode)
        }
        return newText
    }
}

data class AdSwitch(
        val forceNewAdTags: Array<String>,
        val forceOldAdTags: Array<String>,
        val grayReleaseTarget: String
)

object AdLayout {
    val greyReleaseTargetKey = "Grey Release Target key"
    val forceNewAdTagsKey = "Force New Ad Tags Key"
    val forceOldAdTagsKey = "Force Old Ad Tags Key"

}

data class Sponsor(
        val tag: String,
        val title: String,
        val adid: String,
        val zone: String,
        val channel: String,
        // Example: 经济,贸易,股市,股指
        val storyKeyWords: String,
        val cntopic: String,
        val hideAd: String = ""
)

object SponsorManager {
    var sponsors: List<Sponsor> = listOf()
}

object Keywords {
    val reserved = arrayOf("去广告", "单页", "透明", "置顶", "白底", "靠右", "沉底", "资料", "突发", "插图", "高清")
    val removeAd = "去广告"
}

object JSCodes {
    val googletagservices = """
        <script async src="https://www.ft.com/__origami/service/build/v2/bundles/js?modules=o-ads@8.3.0"></script>
        <script async src="https://www.googletagservices.com/tag/js/gpt.js"></script>
    """.trimIndent()

    fun getInlineVideo(storyHTML: String): String {
        return if (storyHTML.contains("inlinevideo")) {
            storyHTML.replace(Regex("</div>"), "</div>\n")
                    .replace(Regex("<div class=[\"]*inlinevideo[\"]* id=[\"]([^\"]*)[\"]* auto[sS]tart=[\"]*([a-zA-Z]+)[\"]* title=\"(.*)\" image=\"([^\"]*)\" vid=\"([^\"]*)\" vsource=\"([^\"]*)\"></div>"), "<div class='o-responsive-video-container'><div class='o-responsive-video-wrapper-outer'><div class='o-responsive-video-wrapper-inner'><script src='http://union.bokecc.com/player?vid=$1&siteid=922662811F1A49E9&autoStart=$2&width=100%&height=100%&playerid=3571A3BF2AEC8829&playertype=1'></script></div></div><a class='o-responsive-video-caption' href='/video/$5' target='_blank'>$3</a></div>")
        } else {
            storyHTML
        }
    }

    fun getCleanHTML(storyHTML: String): String {
        return if (storyHTML.contains("<div class=\"story-theme\"><a target=\"_blank\" href=\"/tag/\"></a><button class=\"myft-follow plus\" data-tag=\"\" data-type=\"tag\">关注</button></div>")) {
            storyHTML.replace("\"<div class=\"story-theme\"><a target=\"_blank\" href=\"/tag/\"></a><button class=\"myft-follow plus\" data-tag=\"\" data-type=\"tag\">关注</button></div>\"", "")
                    .replace("<div class=\"story-image image\" style=\"margin-bottom:0;\"><figure data-webUrl=\"\" class=\"loading\"></figure></div>", "")
                    .replace("<div class=\"story-box last-child\" ><h2 class=\"box-title\"><a>相关话题</a></h2><ul class=\"top10\"><li class=\"story-theme mp1\"><a target=\"_blank\" href=\"/tag/\"></a><div class=\"icon-right\"><button class=\"myft-follow plus\" data-tag=\"\" data-type=\"tag\">关注</button></div></li></ul></div>", "")
        } else {
            storyHTML
        }
    }
}
