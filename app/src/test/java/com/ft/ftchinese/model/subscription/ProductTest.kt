package com.ft.ftchinese.model.subscription

import com.ft.ftchinese.util.json
import org.junit.Assert.*
import org.junit.Test

private const val products = """
[
  {
    "id": "prod_IxN4111S1TIP",
    "tier": "standard",
    "heading": "标准会员",
    "description": "专享订阅内容每日仅需{{dailyAverageOfYear}}元(或按月订阅每日{{dailyAverageOfMonth}}元)\n精选深度分析\n中英双语内容\n金融英语速读训练\n英语原声电台\n无限浏览7日前所有历史文章（近8万篇）",
    "smallPrint": null,
    "plans": [
      {
        "id": "plan_ICMPPM0UXcpZ",
        "productId": "prod_IxN4111S1TIP",
        "price": 258,
        "tier": "standard",
        "cycle": "year",
        "description": "Standardy Yearly Plan",
        "discount": {
          "id": "dsc_PqU34aIHEErX",
          "priceOff": 130,
          "percent": null,
          "startUtc": "2020-08-18T16:00:00Z",
          "endUtc": "2020-09-02T16:00:00Z",
          "description": null
        }
      },
      {
        "id": "plan_drbwQ2gTmtOK",
        "productId": "prod_IxN4111S1TIP",
        "price": 28,
        "tier": "standard",
        "cycle": "month",
        "description": "Standardy Monthly Plan",
        "discount": {
          "id": null,
          "priceOff": null,
          "percent": null,
          "startUtc": null,
          "endUtc": null,
          "description": null
        }
      }
    ]
  },
  {
    "id": "prod_dcHBCHaBTn3w",
    "tier": "premium",
    "heading": "高端会员",
    "description": "专享订阅内容每日仅需{{dailyAverageOfYear}}元\n享受“标准会员”所有权益\n编辑精选，总编/各版块主编每周五为您推荐本周必读资讯，分享他们的思考与观点\nFT中文网2018年度论坛门票2张，价值3999元/张 （不含差旅与食宿）",
    "smallPrint": "注：所有活动门票不可折算现金、不能转让、不含差旅与食宿",
    "plans": [
      {
        "id": "plan_5iIonqaehig4",
        "productId": "prod_dcHBCHaBTn3w",
        "price": 1998,
        "tier": "premium",
        "cycle": "year",
        "description": "Premium Yearly Plan",
        "discount": {
          "id": "dsc_hwvNK0Cfyiny",
          "priceOff": 1000,
          "percent": null,
          "startUtc": "2020-08-18T16:00:00Z",
          "endUtc": "2020-09-02T16:00:00Z",
          "description": null
        }
      }
    ]
  }
]    
"""

class ProductTest {
    @Test
    fun parseProducts() {
        val products = json.parseArray<Product>(products)

        assertNotNull(products)
    }
}
