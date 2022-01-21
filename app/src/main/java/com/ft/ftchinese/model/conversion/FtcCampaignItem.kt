package com.ft.ftchinese.model.conversion

data class FtcCampaignItem(
    val id: String,
    val url: String,
    val status: String,
)

data class FtcCampaigns(
    val sections: List<FtcCampaignItem>
)
