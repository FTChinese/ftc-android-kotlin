package com.ft.ftchinese.tracking
import com.ft.ftchinese.R
import kotlinx.serialization.Serializable
import android.content.Context
import android.util.Log
import com.ft.ftchinese.App


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

@Serializable
data class Sponsor(
    // If tag or title appeares in Story#keywords
    // then hideAdd determines whether ad should be visible.
    val tag: String,
    val title: String,
    val adid: String,
    val zone: String,
    val channel: String,
    // Example: 经济,贸易,股市,股指
    val storyKeyWords: String,
    val cntopic: String,
    val hideAd: String = ""
) {
    fun normalizeZone(): String {
        return if (zone.contains("/")) {
            zone
        } else {
            "home/special/${zone}"
        }
    }

    fun isKeywordsIn(text: String): Boolean {
        if (storyKeyWords.isBlank()) {
            return false
        }

       return Regex(storyKeyWords.replace(Regex(", *"), "|")).containsMatchIn(text)
    }
}

/**
 * [{
 * "tag":"后疫情时代的独角兽之路",
 * "title":"后疫情时代的独角兽之路",
 * "adid":"",
 * "zone":"unicorn",
 * "channel":"",
 * "storyKeyWords":"",
 * "cntopic":"",
 * "hideAd":"no"
 * },{
 * "tag":"高端物业",
 * "title":"高端物业",
 * "adid":"",
 * "zone":"property",
 * "channel":"",
 * "storyKeyWords":"",
 * "cntopic":"",
 * "hideAd":"no"
 * },{
 * "tag":"换脑",
 * "title":"换脑 - ReWired 跨年对话",
 * "adid":"",
 * "zone":"rewired",
 * "channel":"",
 * "storyKeyWords":"",
 * "cntopic":"",
 * "hideAd":"no"
 * },{
 * "tag":"16周年好文精选",
 * "title":"FT中文网16周年好文精选",
 * "adid":"",
 * "zone":"16years",
 * "channel":"",
 * "storyKeyWords":"",
 * "cntopic":"",
 * "hideAd":"no"
 * },{
 * "tag":"",
 * "title":"奔驰维权",
 * "adid":"",
 * "zone":"",
 * "channel":"",
 * "storyKeyWords":"奔驰维权,消费者权益,西安车主",
 * "cntopic":"benzprotest",
 * "hideAd":"no"
 * },{
 * "tag":"",
 * "title":"恐怖袭击",
 * "adid":"5048",
 * "zone":"disaster",
 * "channel":"",
 * "storyKeyWords":"恐怖袭击",
 * "cntopic":"",
 * "hideAd":"no"
 * },{
 * "tag":"讣告",
 * "title":"讣告",
 * "adid":"5048",
 * "zone":"disaster",
 * "channel":"",
 * "storyKeyWords":"",
 * "cntopic":"",
 * "hideAd":"no"
 * },{
 * "tag":"灾难",
 * "title":"灾难",
 * "adid":"5048",
 * "zone":"disaster",
 * "channel":"",
 * "storyKeyWords":"",
 * "cntopic":"",
 * "hideAd":"no"
 * },{
 * "tag":"空难",
 * "title":"空难",
 * "adid":"5048",
 * "zone":"disaster",
 * "channel":"",
 * "storyKeyWords":"",
 * "cntopic":"",
 * "hideAd":"no"
 * },{
 * "tag":"自然灾害",
 * "title":"自然灾害",
 * "adid":"5048",
 * "zone":"disaster",
 * "channel":"",
 * "storyKeyWords":"",
 * "cntopic":"",
 * "hideAd":"no"
 * },{
 * "tag":"车祸",
 * "title":"车祸",
 * "adid":"5048",
 * "zone":"disaster",
 * "channel":"",
 * "storyKeyWords":"",
 * "cntopic":"",
 * "hideAd":"no"
 * },{
 * "tag":"地震",
 * "title":"地震",
 * "adid":"5048",
 * "zone":"disaster",
 * "channel":"",
 * "storyKeyWords":"",
 * "cntopic":"",
 * "hideAd":"no"
 * }]
 */
object SponsorManager {
    var sponsors: List<Sponsor> = listOf()

    fun findMatchIn(kw: String): Sponsor? {
        return sponsors.find {
            it.tag.isNotBlank() && Regex("${it.tag}|${it.title}").containsMatchIn(kw)
        }
    }
}

object Keywords {
    val reserved = arrayOf("去广告", "单页", "透明", "置顶", "白底", "靠右", "沉底", "资料", "突发", "插图", "高清")
    const val removeAd = "去广告"
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
        Log.d("getCleanHTML", "Function called with input length: ${storyHTML.length}")

        // Access the application context from the App singleton
        val context = App.instance.applicationContext

        // Retrieve localized strings
        val followText = context.getString(R.string.follow)
        val followedText = context.getString(R.string.followed)

        // Replace placeholders
        val cleanedHTML = storyHTML
            .replace("{{follow}}", followText)
            .replace("{{followed}}", followedText)

        Log.d("getCleanHTML", "Replacements completed. Output length: ${cleanedHTML.length}")
        return cleanedHTML
    }


}
