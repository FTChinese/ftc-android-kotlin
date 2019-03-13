package com.ft.ftchinese.models

import com.beust.klaxon.Klaxon
import com.beust.klaxon.Parser
import org.junit.Test

class StoryTest {
    private val data = """
        {
  "id": "001081781",
  "ftid": "",
  "publish_status": "publish",
  "fileupdatetime": "1552056856",
  "cheadline": "华为起诉美国：让法律的归法律",
  "cskylineheadline": "",
  "cskylinetext": "",
  "clongleadbody": "刘波：尽管这次起诉胜算不大，但这应被视为一次理性的维权之举，而不应被过度地置于中美冲突的语境中看待。",
  "cshortleadbody": "",
  "cbyline": "0",
  "cbody": "<p>华为与美国政府的法律博弈再揭新篇章。3月7日，华为宣布，已在美国德克萨斯州普莱诺的联邦地区法院针对美国《2019财年国防授权法》第889条的合宪性提起诉讼，请求法院判定这一针对华为的销售限制条款违宪，并判令永久禁止该限制条款的实施。</p>\r\n<p>华为的起诉书认为，第889条在没有经过任何行政或司法程序的情况下，禁止所有美国政府机构从华为购买设备和服务，还禁止美国政府机构与华为的客户签署合同或向其提供资助和贷款，这违背了美国宪法中的剥夺公权法案条款和正当法律程序条款；同时，国会不仅立法，还试图执法和裁决有无违法行为，违背了美国宪法中规定的三权分立原则。</p>\r\n",
  "eheadline": "",
  "elongleadbody": "",
  "eshortleadbody": "",
  "ebyline": "0",
  "ebody": "",
  "cbyline_description": "",
  "cauthor": "FT中文网公共政策主编 刘波 ",
  "cbyline_status": "",
  "ebyline_description": "",
  "eauthor": "",
  "ebyline_status": "",
  "thumblink": null,
  "tag": "华为,法律,诉讼,中美关系,中美贸易战,5G",
  "genre": "comment",
  "topic": "politics,business",
  "industry": "technology",
  "area": "china,usa",
  "scoop": "no",
  "original": "no",
  "show_english": "no",
  "last_publish_time": "1551999133",
  "story_pic": {
    "smallbutton": "http://i.ftimg.net/picture/0/000083380_piclink.jpg",
    "other": "http://i.ftimg.net/picture/0/000083380_piclink.jpg"
  },
  "relative_story": [
    {
      "id": "001081793",
      "cheadline": "原拟于3月底举行的“特习会”将推迟",
      "eheadline": "Trump-Xi summit pushed back from end-March as details hammered out",
      "last_publish_time": "1552037665"
    },
    {
      "id": "001081789",
      "cheadline": "2月份中国出口同比下降21%",
      "eheadline": "Chinese exports drop 21% as slowdown worsens",
      "last_publish_time": "1552026129"
    }
  ]
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