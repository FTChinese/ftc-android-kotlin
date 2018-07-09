package com.ft.ftchinese

//val newsChannels = arrayOf("首页", "中国", "全球", "经济", "金融市场", "商业", "创新经济", "教育", "观点", "管理", "生活时尚")
//val englishChannels = arrayOf("英语电台", "金融英语速读", "双语阅读", "原声视频")
//val ftaChannels = arrayOf("商学院观察", "热点观察", "MBA训练营", "互动小测", "深度阅读")
//val videoChannels = arrayOf("最新", "政经", "商业", "秒懂", "金融", "文化", "高端视点", "有色眼镜")

data class Channel(
        val title: String,
        val listUrl: String? = null,
        val webUrl: String? = null
)

val newsChannels = arrayOf(
        Channel("首页", "https://api003.ftmailbox.com/?webview=ftcapp&bodyonly=yes&maxB=1&backupfile=localbackup&showIAP=yes&pagetype=home&001"),
        Channel("中国", "https://api003.ftmailbox.com/channel/china.html?webview=ftcapp&bodyonly=yes"),
        Channel("独家", "https://api003.ftmailbox.com/channel/exclusive.html?webview=ftcapp&bodyonly=yes&ad=no&001"),
        Channel("编辑精选", "https://api003.ftmailbox.com/channel/editorchoice.html?webview=ftcapp&bodyonly=yes&ad=no&showEnglishAudio=yes&018"),
        Channel("全球", "https://api003.ftmailbox.com/channel/world.html?webview=ftcapp&bodyonly=yes&002"),
        Channel("观点", "https://api003.ftmailbox.com/channel/opinion.html?webview=ftcapp&bodyonly=yes&ad=no"),
        Channel("专栏", "https://api003.ftmailbox.com/channel/column.html?webview=ftcapp&bodyonly=yes&ad=no"),
        Channel("金融市场", "https://api003.ftmailbox.com/channel/markets.html?webview=ftcapp&bodyonly=yes"),
        Channel("商业", "https://api003.ftmailbox.com/channel/markets.html?webview=ftcapp&bodyonly=yes"),
        Channel("科技", "https://api003.ftmailbox.com/channel/technology.html?webview=ftcapp&bodyonly=yes&001"),
        Channel("教育", "https://api003.ftmailbox.com/channel/education.html?webview=ftcapp&bodyonly=yes&001"),
        Channel("管理", "https://api003.ftmailbox.com/channel/management.html?webview=ftcapp&bodyonly=yes"),
        Channel("生活时尚", "https://api003.ftmailbox.com/channel/lifestyle.html?webview=ftcapp&bodyonly=yes"),
        Channel("特别报导", "https://api003.ftmailbox.com/channel/special.html?webview=ftcapp&bodyonly=yes&ad=no&001"),
        Channel("热门文章", "https://api003.ftmailbox.com/channel/weekly.html?webview=ftcapp&bodyonly=yes"),
        Channel("数据新闻", "https://api003.ftmailbox.com/channel/datanews.html?webview=ftcapp&bodyonly=yes"),
        Channel("会议活动", webUrl = "http://www.ftchinese.com/m/events/event.html?webview=ftcapp"),
        Channel("FT研究院", webUrl = "http://www.ftchinese.com/m/marketing/intelligence.html?webview=ftcapp&001")
)

val englishChannels = arrayOf(
        Channel("英语电台", "https://api003.ftmailbox.com/channel/radio.html?webview=ftcapp&bodyonly=yes"),
        Channel("金融英语速读", "https://api003.ftmailbox.com/channel/speedread.html?webview=ftcapp&bodyonly=yes"),
        Channel("双语阅读", "https://api003.ftmailbox.com/channel/ce.html?webview=ftcapp&bodyonly=yes"),
        Channel("原声视频", "https://api003.ftmailbox.com/channel/ev.html?webview=ftcapp&bodyonly=yes&001")
)

val ftaChannels = arrayOf(
        Channel("商学院观察", "https://api003.ftmailbox.com/m/corp/preview.html?pageid=mbastory&webview=ftcapp&bodyonly=yes"),
        Channel("热点观察", "https://api003.ftmailbox.com/m/corp/preview.html?pageid=hotcourse&webview=ftcapp&bodyonly=yes"),
        Channel("MBA训练营", "https://api003.ftmailbox.com/channel/mbagym.html?webview=ftcapp&bodyonly=yes"),
        Channel("互动小测", "https://api003.ftmailbox.com/m/corp/preview.html?pageid=quizplus&webview=ftcapp&bodyonly=yes"),
        Channel("深度阅读", "https://api003.ftmailbox.com/m/corp/preview.html?pageid=mbaread&webview=ftcapp&bodyonly=yes")
)

val videoChannels = arrayOf(
        Channel("最新", "https://api003.ftmailbox.com/channel/stream.html?webview=ftcapp&bodyonly=yes&norepeat=yes"),
        Channel("政经", "https://api003.ftmailbox.com/channel/vpolitics.html?webview=ftcapp&bodyonly=yes&norepeat=yes"),
        Channel("商业", "https://api003.ftmailbox.com/channel/vbusiness.html?webview=ftcapp&bodyonly=yes&norepeat=yes"),
        Channel("秒懂", "https://api003.ftmailbox.com/channel/explainer.html?webview=ftcapp&bodyonly=yes&norepeat=yes"),
        Channel("金融", "https://api003.ftmailbox.com/channel/vfinance.html?webview=ftcapp&bodyonly=yes&norepeat=yes"),
        Channel("文化", "https://api003.ftmailbox.com/channel/vculture.html?webview=ftcapp&bodyonly=yes&norepeat=yes"),
        Channel("高端视点", webUrl = "http://www.ftchinese.com/channel/viewtop.html?webview=ftcapp&norepeat=no"),
        Channel("FT看见", "https://api003.ftmailbox.com/channel/vfeatures.html?webview=ftcapp&bodyonly=yes&norepeat=yes"),
        Channel("有色眼镜", "https://api003.ftmailbox.com/channel/videotinted.html?webview=ftcapp&bodyonly=yes&norepeat=yes")
)