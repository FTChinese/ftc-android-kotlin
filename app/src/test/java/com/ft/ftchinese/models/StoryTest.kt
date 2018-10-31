package com.ft.ftchinese.models

import org.junit.Test

class StoryTest {
    private val story = Story(
            id = "001080024",
            fileupdatetime = "1540939198",
            cheadline = "中国解禁医用虎骨、犀牛角",
            clongleadbody = "中国宣布，符合条件的处方医师将获准使用虎骨和犀牛角。保护人士称，此举将给全球濒危物种带来“毁灭性后果”。",
            cbyline_description = "",
            cauthor = "",
            cbyline_status = "",
            cbody = "<p>中国解除了长达25年的禁止将虎骨与犀牛角用于科研及医疗用途的禁令，保护人士称，此举将在全球范围内给濒危物种带来“毁灭性后果”。</p>\r\n<p>中国国务院表示，将批准符合条件的处方医师使用老虎与犀牛制品（老虎和犀牛身上的某些部分被某些医疗从业者视为有疗效的成分），以及用于未经界定的“文化交流”。</p>\r\n<p>在国家主席习近平的领导下，中国寻求打造对环境更加友好的国际形象，通过了更严苛的保护野生动物与自然资源的法律法规。2016年，为减少偷猎大象的行为，中国禁止了象牙出售。</p>\r\n<p>但这些更严格的条例也与“中药外交”（利用传统中药在全球扩大中国软实力的努力）发生了冲突。</p>\r\n<p>中国媒体将撤销这一禁令描述为加强监管濒危动物制品地下贸易的一种手段，尽管国内管控严格，该行业仍十分猖獗。</p>\r\n<p>根据新规，只有中国中医药局确定的医院和医师，才能从除动物园以外的圈养犀牛与老虎身上采集虎骨与犀牛角。</p>\r\n<p>但批评人士称，此举将令濒危动物黑市死灰复燃，使数十年的保护工作受挫。据世界自然基金会(World Wide Fund for Nature)估计，目前全球仅存3万头野生犀牛与3900只野生老虎。</p>\r\n<p>“令人深感担忧的是，中国撤销了25年的虎骨与犀牛角禁令，解禁了一项将给全球带来毁灭性后果的贸易。”世界自然基金会(WWF)野生动物负责人玛格丽特•金奈德(Margaret Kinnaird)说。</p>\r\n<p>老虎与犀牛制品的使用在主流中医界一直存在争议，许多从业者称其为非主流做法。</p>\r\n<p>译者/何黎</p>\r\n",
            eheadline = "China reverses ban on medical use of rhino and tiger parts",
            ebyline_description = "替换中..........",
            eauthor = "",
            ebyline_status = "",
            elongleadbody = "",
            ebyline = "",
            ebody = "<p>China has lifted a quarter-century ban on the scientific and medical use of tiger bones and rhinoceros horn, in a move that conservationists said would have “devastating consequences globally” for the endangered species.</p>\r\n<p>China’s State Council said it would grant permits for the use of tiger and rhino parts — considered by some medical practitioners as potent healing ingredients — by licensed doctors as well as for unspecified “cultural exchanges”.</p>\r\n<p>Under President Xi Jinping, China has sought to portray itself as more environmentally friendly, passing stricter protections over wildlife and natural resources. In 2016, it banned the sale of ivory to reduce poaching of elephants.</p>\r\n<p>But the more stringent regulations have also come into conflict with “Chinese medicine diplomacy”, an effort to use traditional Chinese medicine (TCM) to expand the country’s soft power globally.</p>\r\n<p>China’s media characterised the ban’s reversal as a way of strengthening oversight of an underground trade of endangered animal parts — an industry that has thrived despite domestic controls.</p>\r\n<p>According to the new rules, only hospitals and doctors certified by a state organisation for Chinese medicine will be able to collect bones or horns from rhinos and tigers raised in captivity, excluding zoo animals.</p>\r\n<p>But critics say the move will revitalise the black market for the endangered animals, setting back decades of conservation efforts. An estimated 30,000 rhinos and 3,900 tigers remain in the wild globally, according to the World Wide Fund for Nature.</p>\r\n<p>“It is deeply concerning that China has reversed its 25-year-old tiger bone and rhino horn ban, allowing a trade that will have devastating consequences globally,” said Margaret Kinnaird, head of wildlife at WWF.</p>\r\n<p>The use of tiger and rhino parts has long been controversial within the mainstream TCM world, with many practitioners calling it a fringe practice.</p>\r\n",
            tag = "中医,中国,监管,医药业,虎骨,犀牛角\"",
            genre = "news",
            topic = "business",
            area = "china",
            last_publish_time = "1540939699",
            story_pic = StoryPic(
                    smallbutton = "http://i.ftimg.net/picture/2/000081452_piclink.jpg",
                    other = "http://i.ftimg.net/picture/2/000081452_piclink.jpg"
            ),
            relative_story = arrayOf(
                    RelatedStory(
                            id = "001074596",
                            cheadline = "中国新药审批将接受境外临床试验数据",
                            eheadline = "China hastens drug approval with embrace of foreign data",
                            last_publish_time = "1507587858"
                    )
            )
    )

    @Test fun splitBody() {
        val arr = story.cbody.split("\r\n").toMutableList()

        arr.add(4, AdParser.getAdCode(AdPosition.MIDDLE_ONE))

        println(arr)
    }
}