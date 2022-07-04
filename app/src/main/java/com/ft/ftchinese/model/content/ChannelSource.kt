package com.ft.ftchinese.model.content

import android.os.Parcelable
import com.ft.ftchinese.model.reader.Permission
import kotlinx.parcelize.Parcelize

const val HTML_TYPE_FRAGMENT = 1
const val HTML_TYPE_COMPLETE = 2

/**
 * ChannelSource specifies how to display a channel page and where to fetch its data.
 */
@Parcelize
data class ChannelSource (
    val title: String, // A Tab's title
        // name is used to cache files.
        // If empty, do not cache it, nor should you try to
        // find cache.
    val name: String,  // Cache filename used by this tab
    val path: String,
    val query: String,
    val htmlType: Int, // Flag used to tell whether the webUrl should be loaded directly
    val permission: Permission? = null // A predefined permission that overrides individual Teaser's permission.

) : Parcelable {

    val isFragment: Boolean
        get() = htmlType == HTML_TYPE_FRAGMENT

    fun withParentPerm(p: Permission?): ChannelSource {
        if (p == null) {
            return this
        }

        return ChannelSource(
            title = title,
            name = name,
            path = path,
            query = query,
            htmlType = htmlType,
            permission = p
        )
    }

    /**
     * Returns a new instance for a pagination link.
     * Example:
     * If current page for a list of articles are retrieved from:
     * https://api003.ftmailbox.com/channel/china.html?webview=ftcapp&bodyonly=yes
     * This page has pagination link at the bottom, which is a relative page `china.html?page=2`.
     * What we need to do is to extract query parameter
     * `page` and append it to current links, generating a link like:
     * https://api003.ftmailbox.com/channel/china.html?webview=ftcapp&bodyonly=yes&page=2
     *
     * We also need to cureate a new value for `name` field
     * based on `page=<number>`:
     * For `news_china`, the second page should be `new_china_2`.
     * For `news_china_3`, the next page should be `new_china_4`.
     */
    fun withPagination(pageKey: String, pageNumber: String): ChannelSource {
        val qs = "$pageKey=$pageNumber"

        return ChannelSource(
            title = title,
            name = "${name}_$pageNumber",
            path = path,
            query = qs,
            htmlType = htmlType
        )
    }

    // Somehow there's a problem on the the web page's pagination:
    // the number on of the current page is never disabled
    // so user could click the page 2 even if it is no page2.
    // TO handle such situation, we just ignore it.
    fun isSamePage(other: ChannelSource): Boolean {
        return name == other.name
    }

    companion object {
        @JvmStatic
        fun ofFollowing(f: Following): ChannelSource {
            return ChannelSource(
                title = f.tag,
                name = "${f.type}_${f.tag}",
                path = "/${f.type}/${f.tag}",
                query = "",
                htmlType = HTML_TYPE_FRAGMENT
            )
        }

        /**
         * Turns a Teaser to a Column.
         * Used when a channel page contains a list of columns
         * rather than articles.
         */
        @JvmStatic
        fun fromTeaser(teaser: Teaser): ChannelSource {
            return ChannelSource(
                title = teaser.title,
                name = "${teaser.type}_${teaser.id}",
                path = "/${teaser.type}/${teaser.id}",
                query = "",
                htmlType = HTML_TYPE_FRAGMENT
            )
        }
    }
}

/**
 * Those links need to start a ChannelActivity.
 * Other links include:
 * /tag/汽车未来
 */
val pathToTitle = mapOf(
    // /m/marketing/intelligence.html?webview=ftcapp
    "intelligence.html" to "FT研究院",
    // /m/marketing/businesscase.html
    "businesscase.html" to "中国商业案例精选",
    // /channel/editorchoice-issue.html?issue=EditorChoice-20181029
    "editorchoice-issue.html" to "编辑精选",
    // /channel/chinabusinesswatch.html
    "chinabusinesswatch.html" to "宝珀·中国商业观察",
    // /m/corp/preview.html?pageid=huawei2018
    "huawei2018" to "+智能 见未来 重塑商业力量",
    // /channel/tradewar.html
    "tradewar.html" to "中美贸易战",
    "viewtop.html" to "高端视点",
    "Emotech2017.html" to "2018·预见人工智能",
    "antfinancial.html" to "“新四大发明”背后的中国浪潮",
    "teawithft.html" to "与FT共进下午茶",
    "creditease.html" to "未来生活 未来金融",
    "markets.html" to "金融市场",
    "hxxf2016.html" to "透视中国PPP模式",
    "money.html" to "理财",
    "whoisstealingmydata" to "大数据时代 数据也在偷偷看你",
    "travel2018" to "跟着欧洲玩家小众游",
    "ebooklxkyzygbygr" to "留学可以怎样改变一个人",
    "ebooksfqkljstjypm" to "是非区块链",
    "ebookygtomzdslhssb" to "英国脱欧 II",
    "ebooknjnxb30nzcgcl5" to "30年职场观察录",
    "ebooknjnxb30nzcgcl4" to "",
    "ebooknjnxb30nzcgcl3" to "",
    "ebooknjnxb30nzcgcl2" to "",
    "ebooknjnxb30nzcgcl1" to "",
    "ebookmdtlptzzgjjhzxhf" to "面对特朗普挑战 中国经济走向何方",
    "ebookmgrwsmzctlp" to "美国人为什么支持特朗普",
    "ebookxzaqdym" to "寻找安全的疫苗",
    "ebookylwjywzgmtrdrbgc" to "日本观察: 以邻为鉴",
    "ebookszcnxyydzd" to "职场女性: 优雅地战斗",
    "ebookcfbj" to "拆分北京",
    "ebooklszh" to "楼市之惑",
    "yearbook2018" to "2018: 中美博弈之年",
    "yearbook2017" to "2017: 自信下的焦虑",
    "ebook-english-1" to "读FT学英语",
    "2018lunchwiththeft1" to "与FT共进午餐"

)

