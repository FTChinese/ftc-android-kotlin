package com.ft.ftchinese.model.content

import com.ft.ftchinese.model.fetch.json
import org.junit.Assert.*
import org.junit.Test

class StoryTest {
    val data = """
        {
          "id": "001016849",
          "ftid": "",
          "publish_status": "publish",
          "fileupdatetime": "1641267155",
          "cheadline": "海归风险投资中国",
          "cskylineheadline": "",
          "cskylinetext": "",
          "clongleadbody": "王辉耀新书《当代中国海归》选载：留学人员回国创业，给中国带回大批风险投资。这种全新的融资方式，极大地催化了中小企业的成长。但他们给中国企业带来的只是风险投资吗？",
          "cshortleadbody": "",
          "cbyline": "0",
          "cbody": "<p>大批的留学人员回国创业，给国内带回了大批风险投资。这种全新的融资方式，极大地催化了中小企业的成长。</p>\r\n<p>目前，国内几乎所有国际风险投资公司的掌门人都是清一色的海归，其中不少人还是我所熟悉的欧美同学会商会会员。大部分风险投资都是通过海归或海归工作的外企带进国内的。这些投资促进了国内对创业的热情，促进了一大批海归企业和国内中小企业的发展，同时也带动了国内风险投资行业的进步。</p>\r\n<p>为了更好地研究海归风险投资在中国的特点和贡献，《海归推动中国》丛书中采访了IDG资深合伙人熊晓鸽、鼎晖国际创投基金董事长吴尚志、赛富亚洲投资基金首席合伙人阎焱、红杉基金中国合伙人张帆、金沙江创业投资董事总经理丁健、美国中经合集团董事总经理张颖、今日资本创始合伙人徐新、启明创投创始人及董事总经理邝子平、美国凯雷投资集团董事总经理何欣、德克萨斯太平洋集团合伙人王兟等10几位掌管各类风险投资基金的海归人士。</p>\r\n<p>近年创业成功后转做创业投资的海归人士，很多人我也很熟悉，经常有机会和他们交流。比如，中国宽带基金主席田溯宁，北极光创业投资创始合伙人邓锋，赛伯乐投资公司董事长朱敏，信中利投资有限公司董事长汪潮涌，红杉中国基金合伙人沈南鹏，蓝山中国资本创始合伙人唐越，华登国际投资集团董事总经理曾之杰等。</p>\r\n<p>1.带回新的创业机制</p>\r\n<p>在研究这些海归风险投资家的交流中我们发现，风险投资不仅是一种在新形势下吸引外资的新方式，更重要的是这种引进还带来了新的创业管理机制和新的管理团队，是一种更为有效的、积极的资金投入。比如说我们以前创业不知道搞创业计划书，不懂得团队的重要性，我觉得这些VC（Venture Capitalist的简称）都给中国带进来了。</p>\r\n<p>2007年我们举办了一场创业投资的专场午餐会座谈会，会上中国留学人才发展基金会理事长陆宇澄说，我接触VC是在1986年，那时候我在北京市当副市长。为了发展北京的高科技，我就专门到硅谷去寻找美国VC，找了一圈以后，我挺失望。事隔多年以后，今天看到这么多年轻的海归朋友都成为VC的创立者，这个现象给我的启发很大。我有几点体会，一是VC的发展对中国的发展至关重要，尤其是我们要建设一个创新型的国家，可能在很大的程度上取决于这个行业的规模、质量、水平；如果有国际一流的VC产业在中国兴起，我相信对创新型国家的建设，包括现在讲到经营模式的转变，减少污染等都是非常重要的。第二，VC这个产业归根到底决定于人才，其中领头人至关重要，再次是团队。第三是投资思路。第四是国家的政策法律环境。</p>\r\n<p>IDG高级副总裁、亚洲公司总裁熊晓鸽是我很熟悉的海归风险投资家，是最早在中国引入高科技产业风险投资的海归之一。早在1993年他代表IDG集团成立了中国第一家合资技术风险公司，1998年又代表IDG集团策划与科技部建立科技风险投资基金，在以后七年内向中国的技术产业提供了10亿美元的创业基金。多年来，IDG在中国投资的成功案例包括亚信、搜狐、当当、金蝶、苏达、百度、携程网等。</p>\r\n<p>对于1990年代初的中国人来说，“VC”、“风险投资”、“创业投资”，这些新名词非常陌生。熊晓鸽和他的伙伴周全像个布道者，见人就向人家传授VC是怎么一回事：用美元投资中国企业，只占少部分股权，不控股，作创业者的教练、朋友甚至保姆，帮助企业做大做强，一起赚钱……大部分人听着，看着这两个一脸诚恳的海归书生，倒不觉得他们像皮包公司的骗子，却觉得他们像是两个“白给你钱、无偿帮助你搞企业、甘当小股东”的傻子。那时，创立不久的中国股市正流行一个新词：圈钱！这股恶风到处刮，相当多的人也因此琢磨开了：能不能从“傻子VC”那里圈点钱呢？尽管熊晓鸽和周全小心翼翼，如履薄冰，钱是没被圈走，但风险投资做得也实在是并不顺利。</p>\r\n<p>作为中国第一家以有限合伙制运营的私募股权基金，鼎晖国际因其投资的专业性以及私募投资行事的低调并不为大众所知晓。鼎晖国际创始人、董事长吴尚志是MIT工程博士和管理硕士。2002年4月，中金公司直接投资部董事总经理吴尚志、副总经理焦震等五名管理层成员与中国经济技术投资担保有限公司共同出资成立了深圳市鼎晖创业投资管理有限公司，作为普通合伙人主要负责CDH的管理运作。管理的基金有两支：一支是总额为1亿美元的海外基金和一支1.35亿元的人民币基金。</p>\r\n<p>吴尚志告诉我们，“鼎晖创业是从很高的平台上开始的，投资人的信誉、市场的信誉已经很成熟了。”鼎晖的优势何在？不在资本量，也不在国际背景，吴尚志最自豪的是他的一支特别能战斗，特别本地化的团队：总裁焦震、合伙人王霖、胡晓玲、王振宇等。面对国内不容乐观的投资环境，鼎晖的策略何在？以自己的优势和良好的人脉赢得摩根斯坦利、高盛等国际大投行的青睐，结伴而行，共进共出，“两头在外”，打造了一条专属于自己的海外IPO专线，从而奠定了自己在这一领域的江湖地位。</p>\r\n<p>2.催生中小企业成长</p>\r\n<p>进入新世纪后，中国经济的快速发展给风险投资（Venture Capital，简称VC或创投）带来了巨大的运作空间，中国创业投资从市场到人才都已日渐成熟。不仅一大批老牌VC焕发新春，更有不少成功创业的海归人士，纷纷成立新的创投基金或孵化基金，催化大批富有活力的中小企业快速成长。</p>\r\n<p>在采访中，我们发现北极光创投基金创始合伙人邓锋的故事很有代表性。邓锋1990年出国，1997年创办一家网络安全公司。2001年这家公司在纳斯达克上市，是9•11以后第一个上市的高科技企业；2002年初，他以40亿美元的价格把公司卖给另外一个在硅谷的公司，引起巨大轰动。这家公司是当时华人留学生在硅谷创建的最大企业。</p>\r\n<p>现在，邓锋开始从企业家向风险投资家转型，为什么要开始这第二次创业？他告诉我们，“为什么转到VC？一个企业家无论企业运作得多么成功，其影响力也是在一个企业当中。做VC则不同，其影响力可以覆盖不同的领域，乃至对整个经济都有影响。其次，在中国要做风险投资就不能没有平台，VC是一个很好的平台。2005年下半年我创办了北极光，主要是投IT业，投比较早期的企业。有人说你要做VC，为什么不加入国际上的机构而要自己做。确实有很多机会加入国际上很有品牌的风险投资，为什么自己做？主要是自己无论做什么都希望创业，北极光就是我第二次创业，重新开始做。一般来说，风险投资都是做一两件事，但是我做了五件事：一是重新了解中国，二是重新建立人脉关系，三是组建自己的团队，四是融资，五是找风险投资。我感觉到风险投资从我自己来讲，是可以做20年甚至30年的事，是一个马拉松，要优化整个过程。”北极光目前在中国投资了20多个中小企业。</p>\r\n<p>红杉资本中国基金创始合伙人张帆也在采访中给我们介绍说，中国创投正进入腾飞期。中国政府从90年代后期开始推动风险投资，创建了很多地方风险基金和法律法规，到现在终于产生一些硕果。我们觉得中国市场开始进入一个新的发展周期。2006年中国风险投资将近20亿美金，已经达到美国硅谷每年投资的1/4。在A股市场全流通之后，中国股票市场蕴含非常大的前景，越来越多的创业型企业可以不去海外，而是扎根在国内上市。我觉得这些都是在非常复杂的环境下，一些很正面的因素。我们也在积极酝酿以本土货币来投入。从长远来看，一个国家的发展，风险投资行业健康发展，一定要把这个国家本身的财富充分运用起来。作为风险投资，我们的长远愿望是让中国的钱能够有效用起来，促进更多企业成长。</p>\r\n<p>目前，很多创投基金的合伙人，同时也是成功创业者。这批新型投资基金管理人，对催化大批中小企业快速成长作用很大。</p>\r\n<p>从亚信日常管理淡出数年后，因为发起成立金沙江创业投资基金并在短短7个月内完成两轮融资，丁健这个昔日IT业内的风云人物再次引起国人的瞩目。在接受我们采访时，丁建说，创业很难，如果每一个人，每一个小公司都要从头慢慢挣扎出来是很痛苦的。我为什么挑选VC这样一个行业，因为我看到周边的很多朋友、同行或者晚一些的年轻人都在走创业路，但是我不希望看到他们再走我走过的弯路。可能我犯的错误、失败的经验比别人多，也是一种竞争力。没有一个秘方会告诉你，哪一个企业是必然成功的。成功是偶然的，失败是必然的。过早的评价一个企业成功与否是很难的。尤其你不能因为一个企业很成功，就认为它做的很多事情都是对的，因为它隐藏的一些毛病出来了，就会变成很大的问题。ＶＣ是一个重要的黏合剂，能把我们拥有的资源很快地给那些好的想法、好的团队注入进去。钱只是一个工具，一个载体，资本本身价值是非常有限的。VC真正的作用是把资源、环境给被投资公司搭建好，能让它们很快地顺利成长起来，这才是非常重要的。</p>\r\n<p>3.重视团队文化</p>\r\n<p>风险投资在投资国内中小企业时最看重什么，能给这些企业带来哪些改变呢？<br>美国中经合集团董事总经理、中国区首席代表张颖（David Zhang）在接受访问时强调，不管是在投资项目还是在吸收团队成员时，虽然经验、经历也重要，我们最看重的还是人品。</p>\r\n<p>ＩＤＧ资深合伙人熊晓鸽说，“投资一个项目，除了看市场环境和产品技术外，最重要的就是看人与管理团队。创业投资，说白了就是投资人和创业人之间的判断和认可。选择投资项目，与创业者交流，就像是记者采访，是听、说和看的过程。一个优秀的记者（投资人），与被采访人（创业者）交流时，首先要问出精彩的问题，并学会倾听，还要根据被采访者（创业者）的回答，及时做出分析和判断，并继续把话题引入深入和未知领域，顺便还要观察被采访人（创业者）本人及周边，以获取更多的材料。这样，一个优秀的记者（投资人）就可以收获很多，回来就可以写稿（签投资协议）了。”</p>\r\n<p>留美归来的朱敏是纳斯达克上市公司WebEx （Nasdaq： WEBX）的创始人，和我们交流了好几次。2007年春天，以32亿美元将公司出售之后，朱敏开始全身心打理他与NEA合资成立的赛伯乐（中国）创业投资管理有限公司。不过，与一般的投资公司不同，赛伯乐是一个孵化器，不仅会对企业进行投资，而且更重要的是，“要带领企业成功”，要培养出一个团队。</p>\r\n<p>在国内，对科技企业而言，有名目繁多的各种基金，它们在扶持新兴技术、企业和产业的发展方面作出了巨大贡献。但这一切，在朱敏眼里却存在一个不小的缺陷。“它们对企业而言，是一种单亲妈妈的身份。”朱敏这样形容，“如果把初创型的企业比喻成婴儿，那目前的各种科技基金、产业园区的扶持基金可以给婴儿喂奶换尿布，但之后的教育却鲜有人涉足。” 朱敏戏言，“赛伯乐努力告诉企业长大后要选择文科还是理科，甚至上哪所大学。赛伯乐不仅相马，而且喂马，同时还帮助马快跑。哥们帮你搭好模型，法律结构，老兄你来运营”。</p>\r\n<p>蓝山资本合伙人唐越在采访中回忆道，创业投资给中国企业带来的不仅是资金，更重要的是给创业团队带来良好的公司治理结构和现代管理理念。有一次巴菲特告诉他，投资就像打球一样，你可以在球场中这样打，你也可以选择在球场的边线打。但唐越告诉我们，对他来说，要么在中间打，要么就不打。由于历史原因，中国第一代企业家必须做很多在球场边线打球的事情。经过20年的发展，中国今天已经到了一个新阶段，我们的创业者也已经过了纯粹为了赚钱而赚钱的阶段了，因此，我们今天必须要在一定的规则下去赚取利润，去积累财富，去建立优秀的公司。这些其实是最根本的一些道理，但是由于现在市场上竞争的无序化，使得很多企业很难面对这样的问题。</p>\r\n<p>唐越说，在投资的时候，我们就明确对被投资人讲，“你到今天已经成功了，你要想获得更大的成功，我们之间合作就必须共享同等的价值观，这个价值观包括你必须严格按照国家的法律交每一个税，不能有任何偷税漏税的行为；你不能通过不正当的手段获取合同、项目；你对人的尊重以及在知识产权方面的尊重等等。从我们的角度，希望能够向企业家灌输这些价值观。我觉得这一点可能是更为重要的。”</p>\r\n<p>北极光创投创始合伙人邓锋说，每一个VC带给企业的不仅是钱，还包括一些机会、资源和团队整合。作为整个VC行业，我们带给中国企业的是什么？我觉得这里面除了钱以外，还有我们的使命，这个使命包括三点。第一，就是我们能够帮助创建一些世界级的中国企业，第二就是能够帮助培育一些世界级的中国企业家，第三，希望我们所有的VC管理人在中国能够成为世界级的风险投资家。</p>\r\n<p><em>选自《当代中国海归》，由中国发展出版社出版，作者王辉耀。此书作者系欧美同学会副会长和商会会长及2005委员会理事长。</em><br></p>\r\n<p><sTRONG><a href=\"http://www.ftchinese.com/sc/specialreport.jsp?id=005000094\">《FT商学院》</a></sTRONG></p>\r\n",
          "eskylineheadline": "",
          "eskylinetext": "",
          "eheadline": "wang hui yao book",
          "elongleadbody": "",
          "eshortleadbody": "",
          "ebyline": "0",
          "ebody": "",
          "cbyline_description": "",
          "cauthor": "",
          "cbyline_status": "",
          "ebyline_description": "",
          "eauthor": "",
          "ebyline_status": "",
          "thumblink": "000004152",
          "tag": "",
          "genre": "feature",
          "topic": "management,,leadership",
          "industry": "",
          "area": "china,",
          "scoop": "no",
          "original": "no",
          "show_english": "no",
          "column": "",
          "pubdate": "1484928000",
          "suppressad": "0",
          "accessright": "0",
          "adchannel": "008000005",
          "breadcrumb": "0",
          "headlinesize": "0",
          "customlink": "",
          "last_publish_time": "1484928000",
          "priority": "99",
          "story_pic": {
            "Getty": "[other][smallbutton]",
            "other": "http://creatives.ftacademy.cn/picture/5/000133965_piclink.jpeg",
            "smallbutton": "http://creatives.ftacademy.cn/picture/5/000133965_piclink.jpeg"
          },
          "story_audio": {
            "ai_audio_e": "",
            "ai_audio_c": "",
            "interactive_id": ""
          },
          "paywall": 0,
          "timefororder": 1.4849280001641e+19,
          "tag_code": "",
          "author_code": "",
          "item_type": "story",
          "whitelist": 1,
          "relative_story": [
            {
              "id": "001063172",
              "cheadline": "亚洲风险投资规模急剧增加",
              "eheadline": "Asia picks up bigger share of venture capital",
              "last_publish_time": "1437692076"
            },
            {
              "id": "001022613",
              "cheadline": "明日次级投资危机？",
              "eheadline": "Cui Xinsheng",
              "last_publish_time": "1224604800"
            },
            {
              "id": "001018952",
              "cheadline": "美国硅谷的风险投资者",
              "eheadline": "SILICON VALLEY INVESTORS DISCOVER LA'S STAR APPEAL",
              "last_publish_time": "1209398400"
            },
            {
              "id": "001018160",
              "cheadline": "欧洲风投3I放弃对初创企业早期投资",
              "eheadline": "EX-VENTURE CAPITAL POWERHOUSE 3I QUITS EARLY-STAGE INVESTMENTS",
              "last_publish_time": "1206374400"
            },
            {
              "id": "001017474",
              "cheadline": "中国海归从政",
              "eheadline": "wang hui yao",
              "last_publish_time": "1203868800"
            },
            {
              "id": "001017318",
              "cheadline": "海归企业上市华尔街",
              "eheadline": "Wang Huiyao’s Sea-turtle 3",
              "last_publish_time": "1203264000"
            },
            {
              "id": "001017316",
              "cheadline": "申请MBA的经济周期",
              "eheadline": "(MBA) Courses may boom despite slump",
              "last_publish_time": "1203264000"
            },
            {
              "id": "001017315",
              "cheadline": "不再效仿美国商学院?",
              "eheadline": "(MBA INTRO) DEMAND FOR MBAS SURGES IN ASIA",
              "last_publish_time": "1203264000"
            },
            {
              "id": "001017314",
              "cheadline": "市场动荡令商学院担忧",
              "eheadline": "(MBA) MARKETS CLOUD OUTLOOK",
              "last_publish_time": "1203264000"
            },
            {
              "id": "001016847",
              "cheadline": "是心动，还是股市动？",
              "eheadline": "How a state of mind abets market instability",
              "last_publish_time": "1200844800"
            },
            {
              "id": "001013924",
              "cheadline": "中国为首批本土风险资本基金注资",
              "eheadline": "CHINA ACTS TO DEVELOP DOMESTIC VC FUNDS",
              "last_publish_time": "1188921600"
            },
            {
              "id": "001013824",
              "cheadline": "美国风险资本投资额上半年大幅增长",
              "eheadline": "Large groups go back into VC",
              "last_publish_time": "1188489600"
            },
            {
              "id": "001013638",
              "cheadline": "中国成为第二大风险投资目的国",
              "eheadline": "UK TRAILS CHINA FOR START-UP FUNDING VENTURE CAPITAL 2006 STATISTICS",
              "last_publish_time": "1187798400"
            },
            {
              "id": "001013195",
              "cheadline": "美国风险投资扩张在华业务",
              "eheadline": "Highland Capital confident move east will pay off",
              "last_publish_time": "1186070400"
            },
            {
              "id": "001012061",
              "cheadline": "欧洲私人股本投资吸引更多资金",
              "eheadline": "SHARP RISE IN EUROPE'S CASH FOR START-UPS",
              "last_publish_time": "1181836800"
            },
            {
              "id": "001009842",
              "cheadline": "低工资导致中国人口大流动",
              "eheadline": "readers letter 6",
              "last_publish_time": "1173024000"
            },
            {
              "id": "001009649",
              "cheadline": "英国风投交易额名列欧洲第一",
              "eheadline": "UK TOPS EUROPEAN VENTURE CAPITAL DEALS",
              "last_publish_time": "1171468800"
            },
            {
              "id": "001008441",
              "cheadline": "如何看待外企的国内地位不可儿戏",
              "eheadline": "readers letter 4",
              "last_publish_time": "1166025600"
            },
            {
              "id": "001008376",
              "cheadline": "全球风险投资总额将突破320亿美元",
              "eheadline": "VENTURE CAPITAL RISES TO LEVELS NOT SEEN SINCE DOTCOM BUBBLE",
              "last_publish_time": "1165766400"
            },
            {
              "id": "001007739",
              "cheadline": "风险资金再次流向硅谷",
              "eheadline": "NEW VENTURE CASH FLOODS INTO SILICON VALLEY",
              "last_publish_time": "1162828800"
            },
            {
              "id": "001007585",
              "cheadline": "雷曼兄弟联手IBM在华推出1.8亿美元风投基金",
              "eheadline": "Lehman and IBM in China fund link",
              "last_publish_time": "1162137600"
            },
            {
              "id": "001007181",
              "cheadline": "创业案例：与美国风投打交道",
              "eheadline": "TWO NEW VOICES IN THE VALLEY",
              "last_publish_time": "1160323200"
            }
          ],
          "relative_vstory": [
            
          ]
        }
    """.trimIndent()
    @Test
    fun parseStory() {
        val story = json.parse<Story>(data)

        println(story)
    }
}
