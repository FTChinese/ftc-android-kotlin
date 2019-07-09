package com.ft.ftchinese.model

import com.beust.klaxon.Klaxon
import com.beust.klaxon.Parser
import org.junit.Test

class StoryTest {
    private val data = """
{
  "id": "001083510",
  "ftid": "02e79d10-9dda-11e9-b8ce-8b459ed04726",
  "publish_status": "publish",
  "fileupdatetime": "1562518607",
  "cheadline": "坐等特朗普卸任是高风险押注",
  "cskylineheadline": "",
  "cskylinetext": "",
  "clongleadbody": "卢斯：很多特朗普的反对者都持有一种心态：特朗普属于“反常现象”，我们要做的就是等待时钟走完一圈，然后重置它。",
  "cshortleadbody": "",
  "cbyline": "0",
  "cbody": "<p>直到去世那天，芭芭拉•布什(Barbara Bush)的床头柜上一直放着一个钟，用来倒数离唐纳德•特朗普(Donald Trump)任期结束还有多少天。这位前第一夫人的倒计时很乐观：她只设定了一届总统任期。世界上大多数人都跟已故的芭芭拉•布什一样。没有外交倡议被提上日程，从这一点判断，西方国家也在翻着日历，坐等特朗普卸任。非特朗普阵营的美国选民亦是如此。正如民主党参选人阵营领跑者乔•拜登(Joe Biden)不停念叨的，特朗普是一种“反常现象”(aberration)——就像时钟短暂地走错了方向。我们所需要做的就是等待时钟走完一圈，然后重置它。</p>\r\n<p>这种看法存在两个问题。首先，你无法重新找回失去的时间。世界不可能重新回到特朗普上任前的样子。对于沙特阿拉伯、俄罗斯和朝鲜而言，特朗普竞选连任失败将是个坏消息。特朗普批准了沙特的冒险主义，认可了弗拉基米尔•普京(Vladimir Putin)的世界观，并给金正恩(Kim Jong Un)带来一场令外界印象深刻的“亮相派对”。但目前尚不清楚特朗普卸任会对中国造成多大影响。自特朗普上任以来，美国发生的最显著变化是华盛顿迅速着手发起了一场所谓新冷战。特朗普的继任者很可能像他一样把中国视为美国的主要竞争对手。</p>\r\n<p>欧洲、日本、加拿大以及澳大利亚也应保持警惕。虽然特朗普卸任将预示着语调的巨大变化，但实质性问题上依然存在。特朗普的民主党继任者同样不会容忍欧洲较低的国防预算。显示大多数美国人支持自由贸易的民调具有误导性，因为这些民调忽视了自由贸易遭到反对的激烈程度。两大阵营中的决定性力量都强烈反对新协议——无论是与盟国，还是与其他国家签署。过去，民主党总统可以依靠共和党议员让自己的贸易协议获得通过。那些日子可能已经过去。任何一位美国总统遏制中国的合理选择，都将是重新加入巴拉克•奥巴马(Barack Obama)的《跨太平洋伙伴关系协定》(TPP)。然而，即使奥巴马也无法说服民主党人投票支持TPP。如果伊丽莎白•沃伦(Elizabeth Warren)或贺锦丽(Kamala Harris)当选总统，她们甚至都不大可能去尝试。沃伦的外交政策被称为“有人情味的特朗普主义”。</p>\r\n<p>伊朗在这方面是个例外。不久前，伊朗最高领袖阿亚图拉阿里•哈梅内伊(Ayatollah Ali Khamenei)排除了与特朗普谈判的任何可能。这应该意味着，伊朗将一直等到美国选出一个不那么反复无常的总统。然而，伊朗正在无视欧洲让其继续留在核协议内的建议。伊朗计划在本周末突破伊核协议允许其储存浓缩铀的上限（编者注：原文发表于7月4日）。无论美伊爆发冲突的危险多大，形势都不可能回到过去。到下一任美国总统就职时，伊朗要么与美国处于战争状态，要么距成为核国家又近了一年半时间。可能两者兼是。这里的时钟无法重置。</p>\r\n<p>这种“反常”说的第二个问题是，特朗普可能赢得连任。历史数据显示，如果经济保持增长，美国总统便能成功连任。在过去100年间，未获连任的美国总统只有老布什(George H.W. Bush)、吉米•卡特(Jimmy Carter)和赫伯特•胡佛(Herbert Hoover)。三人在任期内都遭遇了经济衰退。特朗普或成为这一规律的例外。或者，美国经济可能急转直下。无论押注于哪一种情况都是鲁莽的。</p>\r\n<p>与此同时，特朗普的对手很可能已经采取了足够古怪的立场，以致特朗普赢得连任的可能性大增。在日前举行的民主党初选辩论中，许多参选人誓言要废除一项将非法越境入刑的法律。美国公众普遍支持移民，但强烈反对开放边界。有三位主要参选人希望废除私人医疗保险。多数参选人还支持一项允许跨性别儿童自行选择卫生间的法案。这些正是特朗普希望其对手所持的立场。</p>\r\n<p>这让那些等待时钟走完的人陷入了进退两难的境地。一方面，直到2025年，世界可能都要和特朗普捆绑在一起，如果真是这样，世界等于浪费了4年时间在押注美国之前犯了个错误。另一方面，特朗普败选也不会再现他们自认记忆犹新的那个世界。古语说，人不能两次踏进同一条河流。无论如何，美国的盟友们应该抱最乐观的态度，但须更加努力做好最坏的打算。</p>\r\n<p>译者/谶龙</p>\r\n",
  "eskylineheadline": "",
  "eskylinetext": "Edward Luce",
  "eheadline": "Running down the clock on Trump is a risky bet",
  "elongleadbody": "",
  "eshortleadbody": "",
  "ebyline": "0",
  "ebody": "<p>Until the day she died, Barbara Bush kept a clock next to her bed that counted the days until Donald Trump was gone. The former first lady’s timekeeping was upbeat: she assumed a one-term presidency. Most of the world is in the late Mrs Bush’s camp. To judge by the absence of diplomatic initiatives, the west is also marking the calendar until Mr Trump leaves office. The same goes for non-Trumpian America. As Joe Biden, the Democratic frontrunner, keeps saying, Mr Trump is an “aberration” — as though time has briefly gone astray. All we need is to wait out the clock then reset it.</p>\r\n<p>There are two problems with this view. The first is that you cannot recapture lost time. The world will not reboot to where it was before Mr Trump took office. A defeat for Mr Trump would be bad news for Saudi Arabia, Russia and North Korea. Mr Trump has licensed Saudi adventurism, validated Vladimir Putin’s world view, and given Kim Jong Un a coming-out party to remember. But it is unclear Mr Trump’s exit would make much difference to China. The most striking change to have happened since he took office is the speed with which Washington has embraced the so-called new cold war. Mr Trump’s successor would be as likely as he to see China as America’s main rival.</p>\r\n<p>Europe, Japan, Canada and Australia should also be wary. Although Mr Trump’s departure would herald a big change in tone, there would be continuities on substance. Mr Trump’s Democratic successor would be just as impatient with Europe’s low defence budgets. Polls that show most Americans in favour of free trade are misleading because they miss the intensity of opposition. Decisive factions on both ends of the spectrum are strongly opposed to new deals — with allies or others. In the past, Democratic presidents could rely on Republican lawmakers to pass their trade deals. Those days are probably over. The logical step for any US president to contain China would be to rejoin Barack Obama’s Trans-Pacific Partnership. Yet even Mr Obama could not persuade Democrats to vote for the TPP. It is doubtful a President Elizabeth Warren or a President Kamala Harris would even try. Ms Warren’s foreign policy has been called “Trumpism with a human face”.</p>\r\n<p>The exception to waiting out the clock is Iran. Last week its supreme leader, Ayatollah Ali Khamenei, dismissed any thought of negotiating with Mr Trump. The implication should have been that Iran would wait until America elected someone less erratic. Yet Iran is ignoring Europe’s advice to stay within the nuclear deal. It plans to start breaching its enrichment limits this weekend. Whatever the dangers of a US-Iran conflict, the situation cannot go back to before. Iran will either be at war with the US when the next president takes office, or a year and a half closer to being a nuclear power. Possibly both. That clock cannot be reset.</p>\r\n<p>The second problem with the “aberration” school is that Mr Trump might win a second term. History says US presidents are re-elected if the economy is growing. The only one-term presidents in the past 100 years were George H.W. Bush, Jimmy Carter and Herbert Hoover. Each was grappling with a recession. Mr Trump may be an exception to that rule. Or the US economy could take a nosedive. Betting on either would be rash.</p>\r\n<p>Meanwhile, the chances are that Mr Trump’s opponent will have taken enough outlandish stances to improve his re-election prospects sharply. In last week’s Democratic debates, many candidates vowed to abolish a law that criminalises illegal border crossings. The US public is generally pro-immigration. But it strongly opposes open borders. Three leading candidates wanted to abolish private health insurance. Most also support a bill that would give transgender bathroom choice for children. These are the positions Mr Trump wants in his opponent.</p>\r\n<p>Which leaves the run-down-the-clock crowd in a quandary. On the one hand, the world might be stuck with him until 2025, in which case it will have wasted four years betting that America had erred. On the other, Mr Trump’s defeat would not recreate the world they think they remember. As the saying goes, you cannot step into the same river twice. Either way, America’s allies should hope for the best but work far harder to prepare for the worst.</p>\r\n",
  "cbyline_description": "",
  "cauthor": "英国《金融时报》专栏作家 爱德华•卢斯 ",
  "cbyline_status": "",
  "ebyline_description": "",
  "eauthor": "Edward Luce",
  "ebyline_status": "",
  "thumblink": null,
  "tag": "特朗普,美国,全球政治",
  "genre": "comment",
  "topic": "politics",
  "industry": "",
  "area": "usa",
  "scoop": "no",
  "original": "no",
  "show_english": "no",
  "column": "",
  "pubdate": "1562515200",
  "suppressad": "0",
  "accessright": "0",
  "adchannel": "",
  "breadcrumb": "0",
  "headlinesize": "0",
  "customlink": "",
  "last_publish_time": "1562525883",
  "priority": "21",
  "returnStoryInfo": "returnStoryInfo",
  "story_pic": {
    "smallbutton": "http://i.ftimg.net/picture/7/000087597_piclink.jpg",
    "other": "http://i.ftimg.net/picture/7/000087597_piclink.jpg"
  },
  "paywall": 0,
  "whitelist": 0,
  "relative_story": [],
  "relative_vstory": ""
}""".trimIndent()

//    @Test fun splitBody() {
//        val arr = story.bodyCN.split("\r\n").toMutableList()
//
//        arr.add(4, AdParser.getAdCode(AdPosition.MIDDLE_ONE))
//
//        println(arr)
//    }

    @Test fun parse() {
        val result = Klaxon().parse<Story>(data)

        println(result)

        Parser
    }

    @Test fun parseStory() {
        val r = Klaxon().parse<Story>(data)

        println(r)
    }

    @Test fun arrayParse() {
        data class Child(val id: Int, val name: String)
        data class Parent(val children: Array<Child>)

        val array = """{
            "children":[
                {"id": 1, "name": "foo"},
                {"id": 2, "name": "bar"}
            ]
         }"""

        val r = Klaxon().parse<Parent>(array)

        println(r)
    }
}
