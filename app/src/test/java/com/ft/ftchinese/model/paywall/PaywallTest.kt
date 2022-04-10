package com.ft.ftchinese.model.paywall

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Test

class PaywallTest {
    val data = """
{
    "id": 10,
    "banner": {
        "id": "banner_1njoG8AbUxeH",
        "heading": "FT中文网会员订阅test",
        "subHeading": "欢迎您！",
        "coverUrl": "http://www.ftacademy.cn/subscription.jpg",
        "content": "希望全球视野的FT中文网，能够带您站在高海拔的地方俯瞰世界，引发您的思考，从不同的角度看到不一样的事物，见他人之未见！",
        "terms": null,
        "startUtc": null,
        "endUtc": null
    },
    "promo": {
        "id": "",
        "heading": "",
        "subHeading": null,
        "coverUrl": null,
        "content": null,
        "terms": null,
        "startUtc": null,
        "endUtc": null
    },
    "liveMode": false,
    "createdUtc": "2021-12-06T02:52:46Z",
    "products": [
        {
            "id": "prod_9xrJdHFq0wmq",
            "active": false,
            "liveMode": false,
            "createdBy": "weiguo.ni",
            "description": "专享订阅内容每日仅需{{dailyAverageOfYear}}元(或按月订阅每日{{dailyAverageOfMonth}}元)\n精选深度分析\n中英双语内容\n金融英语速读训练\n英语原声电台\n无限浏览2日前所有历史文章（近9万篇）",
            "heading": "标准会员",
            "smallPrint": "test",
            "tier": "standard",
            "introductory": {
                "stripePriceId": null
            },
            "createdUtc": "2021-01-25T01:51:09Z",
            "updatedUtc": "2021-01-25T01:51:09Z",
            "prices": [
                {
                    "id": "price_WHc5ssjh6pqw",
                    "tier": "standard",
                    "cycle": "year",
                    "active": true,
                    "archived": false,
                    "currency": "cny",
                    "description": "testtest",
                    "liveMode": false,
                    "nickname": null,
                    "productId": "prod_9xrJdHFq0wmq",
                    "unitAmount": 298,
                    "createdUtc": "2021-09-10T09:27:28Z",
                    "createdBy": "Mosciski7791",
                    "stripePriceId": "price_1IM2nFBzTK0hABgJiIDeDIox",
                    "offers": [
                        {
                            "id": "dsc_cHpm6qlKip8y",
                            "liveMode": false,
                            "status": "active",
                            "description": "特惠一分钱",
                            "kind": "promotion",
                            "overridePeriod": {
                                "years": 0,
                                "months": 0,
                                "days": 0
                            },
                            "percent": null,
                            "priceOff": 297.9,
                            "priceId": "price_WHc5ssjh6pqw",
                            "recurring": false,
                            "startUtc": "2021-12-01T16:00:00Z",
                            "endUtc": "2021-12-30T16:00:00Z",
                            "createdBy": "weiguo.ni",
                            "createdUtc": "2021-12-03T03:37:25Z"
                        },
                        {
                            "id": "dsc_iirQArMFjBfs",
                            "liveMode": false,
                            "status": "active",
                            "description": "到期前续订享75折",
                            "kind": "retention",
                            "overridePeriod": {
                                "years": 0,
                                "months": 0,
                                "days": 0
                            },
                            "percent": null,
                            "priceOff": 80,
                            "priceId": "price_WHc5ssjh6pqw",
                            "recurring": true,
                            "startUtc": null,
                            "endUtc": null,
                            "createdBy": "weiguo.ni",
                            "createdUtc": "2021-09-17T03:41:12Z"
                        },
                        {
                            "id": "dsc_Vn7686x357KY",
                            "liveMode": false,
                            "status": "active",
                            "description": "再次购买享八五折优惠",
                            "kind": "win_back",
                            "overridePeriod": {
                                "years": 0,
                                "months": 0,
                                "days": 0
                            },
                            "percent": null,
                            "priceOff": 40,
                            "priceId": "price_WHc5ssjh6pqw",
                            "recurring": true,
                            "startUtc": null,
                            "endUtc": null,
                            "createdBy": "weiguo.ni",
                            "createdUtc": "2021-12-01T08:00:08Z"
                        }
                    ]
                },
                {
                    "id": "price_v5E2WSqJymxe",
                    "tier": "standard",
                    "cycle": "month",
                    "active": true,
                    "archived": false,
                    "currency": "cny",
                    "description": "Standard Monthly Edition Test",
                    "liveMode": false,
                    "nickname": null,
                    "productId": "prod_9xrJdHFq0wmq",
                    "unitAmount": 35,
                    "createdUtc": "2021-09-16T09:19:44Z",
                    "createdBy": "weiguo.ni",
                    "stripePriceId": "price_1IM2mgBzTK0hABgJVH8o9Sjm",
                    "offers": [
                        {
                            "id": "dsc_N45gIqVrUB2r",
                            "liveMode": false,
                            "status": "active",
                            "description": "新会员订阅首月仅1元",
                            "kind": "introductory",
                            "overridePeriod": {
                                "years": 0,
                                "months": 0,
                                "days": 0
                            },
                            "percent": null,
                            "priceOff": 34,
                            "priceId": "price_v5E2WSqJymxe",
                            "recurring": false,
                            "startUtc": "2021-11-16T16:00:00Z",
                            "endUtc": "2021-12-30T16:00:00Z",
                            "createdBy": "weiguo.ni",
                            "createdUtc": "2021-11-17T06:20:11Z"
                        }
                    ]
                }
            ]
        },
        {
            "id": "prod_zSgOTS6DWLmu",
            "active": false,
            "liveMode": false,
            "createdBy": "weiguo.ni",
            "description": "专享订阅内容每日仅需5.5元\n享受“标准会员”所有权益\n编辑精选，总编/各版块主编每周五为您推荐本周必读资讯，分享他们的思考与观点\nFT商学院高端专享\nFT中文网2021年度论坛门票2张",
            "heading": "高端会员",
            "smallPrint": "注：所有活动门票不可折算现金、不能转让、不含差旅与食宿",
            "tier": "premium",
            "introductory": {
                "stripePriceId": null
            },
            "createdUtc": "2021-01-25T01:48:34Z",
            "updatedUtc": "2021-01-25T01:48:34Z",
            "prices": [
                {
                    "id": "price_zsTj2TQ1h3jB",
                    "tier": "premium",
                    "cycle": "year",
                    "active": true,
                    "archived": false,
                    "currency": "cny",
                    "description": "This is a test",
                    "liveMode": false,
                    "nickname": null,
                    "productId": "prod_zSgOTS6DWLmu",
                    "unitAmount": 1998,
                    "createdUtc": "2021-09-16T09:44:53Z",
                    "createdBy": "weiguo.ni",
                    "stripePriceId": "plan_FOde0uAr0V4WmT",
                    "offers": [
                        {
                            "id": "dsc_7CTjgetPlvjK",
                            "liveMode": false,
                            "status": "active",
                            "description": "This is a test",
                            "kind": "promotion",
                            "overridePeriod": {
                                "years": 0,
                                "months": 0,
                                "days": 0
                            },
                            "percent": null,
                            "priceOff": 1997.99,
                            "priceId": "price_zsTj2TQ1h3jB",
                            "recurring": false,
                            "startUtc": null,
                            "endUtc": null,
                            "createdBy": "weiguo.ni",
                            "createdUtc": "2021-09-16T09:45:24Z"
                        }
                    ]
                }
            ]
        }
    ]
}        
    """.trimIndent()
    @Test fun parsePaywallJSON() {
        try {
            val pw = Json.decodeFromString<Paywall>(data)
            println(pw)
        } catch (e: Exception) {
            println(e)
        }
    }
}
