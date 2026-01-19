package com.ft.ftchinese.ui.subs.paywall

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.model.paywall.Banner
import com.ft.ftchinese.ui.components.MarkdownText
import com.ft.ftchinese.ui.theme.Dimens
import org.threeten.bp.ZonedDateTime

@Composable
fun PromoBox(
    banner: Banner,
) {
    Card {
        Column {
            Text(
                text = banner.heading,
                style = MaterialTheme.typography.h5
            )

            Spacer(modifier = Modifier.height(Dimens.dp8))

            banner.subHeading?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.subtitle1
                )
            }

            Spacer(modifier = Modifier.height(Dimens.dp16))

            banner.terms?.let {
                MarkdownText(
                    markdown = it
                )
            }

            Spacer(modifier = Modifier.height(Dimens.dp16))
        }
    }
}

@Preview
@Composable
fun PreviewPromoBox() {
    PromoBox(banner = Banner(
        id = "promo_SK08kxOWRpNU",
        heading = "福牛迎春，会员85折，最高立省1206元 | 涨价前最后一次促销",
        subHeading = "新订高端会员，还送“2020年度FT商业图书榜单礼包”，价值906元！",
        content = "百亿基金一日售罄；南下资金风潮汹涌；港股美股比特币，越来越多的市场与产品受到普通投资者的追捧。\n此时，你更需要客观、权威、国际化的商业财经新闻、深度分析与评论，为你护城。\n2021年，除了中国本土报道，我们还将重点关注拜登政府下的中美关系；脱欧后英国与欧盟的平衡点在哪；全球各市场走势分析；全球气候问题；全球科技趋势与泡沫、全球大公司深度报道......\n130余年的英国《金融时报》，正在为你追踪全球经济与金融脉动，助你理清逻辑，顺势而为。遍布全球600多位专家型记者，提供多元视角的报道，为你护航！\n牛年牛市，即日到2021年2月7日，FT中文网订阅会员85折促销，助你做出更好的决策，占得先机！新订高端会员，还送“2020年度FT商业图书榜单礼包”，价值906元！\n### 谁可以参加 + 如何参加？\n非会员，均可以参加；支付成功后，高端会员按提示填写物流信息（配送地址仅限中国大陆）\n### 为什么现在订阅？\n1. 涨价前最后一次促销，春节后会员价格即将调整\n2. 高端会员更是立省1206元：专享赠品 “2020年度FT商业图书榜单礼包”，是当下最有影响力的10本商业图书，它们对现代商业问题，比如管理、金融和经济学，提供扣人心弦和引人入胜的洞见。同时，从这些入选书籍中，我们也能够看到，每个人的成功都不是偶然，都是他们在各自领域里有独特的见解和不同的坚持。",
        terms = "##### 注意事项\n1. 活动截止：2021年2月7日24点（北京时间）\n2. 现有会员（含升级/续订）、月度标准会员、实体订阅卡、企业机构会员和信用卡/借记卡支付会员均不参加本次活动\n3. 高端会员礼包赠品配送地址仅限中国大陆，海外及港澳台地区订户可请大陆好友代收；春节后顺丰发出；\n4. 安卓系统手机用户请通过华为市场更新应用至最新版本；iOS（iPhone、iPad）系统用户请通过苹果App Store应用市场更新应用至最新版本\n5. 通过iOS内购支付的会员，次年自动续订为原价\n6. 更多会员专属服务，请关注系统推送欢迎邮件\n7. 联系客服：subscriber.service@ftchinese.com 电话/微信： 18501972480；（工作日9:30-17:30）\n7. 付费订阅为虚拟内容服务，一经订阅成功概不退款，请谨慎购买\n8. 本次活动的最终解释权归FT中文网所有",
        startUtc = ZonedDateTime.now().minusDays(1),
        endUtc = ZonedDateTime.now().plusDays(1)
    ))
}
