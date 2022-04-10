package com.ft.ftchinese.model.conversion

import kotlinx.serialization.Serializable

@Serializable
data class FtcCampaignItem(
    val id: String,
    val url: String,
    val status: String,
)

@Serializable
data class FtcCampaigns(
    val sections: List<FtcCampaignItem>
)
