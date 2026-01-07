package com.jayala.vexapp

import com.google.gson.annotations.SerializedName

data class AwardsResponse(
    @SerializedName("data") val data: List<AwardData>
)

data class AwardData(
    @SerializedName("title") val title: String,
    @SerializedName("event") val event: AwardEvent?, // Add ?
    @SerializedName("season") val season: AwardSeason? // Add ?
)

data class AwardEvent(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String?,
    @SerializedName("start") val start: String?
)

data class AwardSeason(
    @SerializedName("name") val name: String?
)

data class AwardUiModel(
    val title: String,
    val eventName: String,
    val displayDate: String,
    val sortableDate: String
)