package com.ft.ftchinese.model.ftcsubs

import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.model.paywall.Paywall
import org.junit.Test

class PaywallTest {
    private val pwData = """
        {
            "banner": {
                "id": 1,
                "heading": "FT中文网会员订阅服务",
                "subHeading": "欢迎您！",
                "coverUrl": "http://www.ftacademy.cn/subscription.jpg",
                "content": "希望全球视野的FT中文网，能够带您站在高海拔的地方俯瞰世界，引发您的思考，从不同的角度看到不一样的事物，见他人之未见！"
            },
            "promo": {
                "id": "promo_wa3UWuiYKEgm",
                "heading": "FT中文网会员订阅",
                "subHeading": "双11限时85折，还送财经词汇！",
                "coverUrl": "http://www.ftacademy.cn/subscription.jpg",
                "content": "前1000名标准会员送价值69元的《英国<金融时报>财经词汇》简装版，高端会员送价值199元的精装版。\n\n未来365天，在原汁原味英文+资深团队译文中，取得语言能力的进步。在国际视野与多元言论里，让你站在高海拔的地方俯瞰世界，深入思考！",
                "terms": "活动截止：2020年11月11日24点（北京时间）\n现有会员（含升级/续订）、实体订阅卡、企业机构会员和Stripe支付会员均不参加本次活动\n赠品配送地址仅限中国大陆，海外及港澳台地区订户可请大陆好友代收\n本次活动的最终解释权归FT中文网所有",
                "startUtc": "2020-11-09T04:00:00Z",
                "endUtc": "2020-11-11T16:00:00Z"
            },
            "products": [
                {
                    "id": "prod_IxN4111S1TIP",
                    "tier": "standard",
                    "heading": "标准会员",
                    "description": "专享订阅内容每日仅需0.72元(或按月订阅每日0.93元)\n精选深度分析\n中英双语内容\n金融英语速读训练\n英语原声电台\n无限浏览7日前所有历史文章（近8万篇）",
                    "smallPrint": null,
                    "plans": [
                        {
                            "id": "plan_ICMPPM0UXcpZ",
                            "productId": "prod_IxN4111S1TIP",
                            "price": 258,
                            "tier": "standard",
                            "cycle": "year",
                            "description": "Standard Yearly Plan",
                            "discount": {
                                "id": "dsc_UQKuPqxAZvmR",
                                "priceOff": 40,
                                "percent": null,
                                "startUtc": "2020-11-09T04:00:00Z",
                                "endUtc": "2020-11-11T16:00:00Z",
                                "description": null
                            }
                        },
                        {
                            "id": "plan_drbwQ2gTmtOK",
                            "productId": "prod_IxN4111S1TIP",
                            "price": 28,
                            "tier": "standard",
                            "cycle": "month",
                            "description": "Standard Monthly Plan",
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
                    "id": "prod_hNUYgnJR62Zt",
                    "tier": "premium",
                    "heading": "高端会员",
                    "description": "专享订阅内容每日仅需5.5元\n享受“标准会员”所有权益\n编辑精选，总编/各版块主编每周五为您推荐本周必读资讯，分享他们的思考与观点\nFT研究院专题报告和行业报告\nFT中文网2021年度论坛门票2张",
                    "smallPrint": "注：所有活动门票不可折算现金、不能转让、不含差旅与食宿",
                    "plans": [
                        {
                            "id": "plan_d6KVqcmEBqjv",
                            "productId": "prod_hNUYgnJR62Zt",
                            "price": 1998,
                            "tier": "premium",
                            "cycle": "year",
                            "description": "Premium Year Edition",
                            "discount": {
                                "id": "dsc_cqgp9zBTwSnY",
                                "priceOff": 300,
                                "percent": null,
                                "startUtc": "2020-11-09T04:00:00Z",
                                "endUtc": "2020-11-11T16:00:00Z",
                                "description": null
                            }
                        }
                    ]
                }
            ]
        }
    """.trimIndent()

    @Test
    fun parse() {
        val pw = json.parse<Paywall>(pwData)

        print(pw)
    }
}
