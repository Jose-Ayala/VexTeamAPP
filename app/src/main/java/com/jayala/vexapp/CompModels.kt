package com.jayala.vexapp
import com.google.gson.annotations.SerializedName

// 1. The model for the Dropdown (Isolated)
data class CompEventResponse(
    @SerializedName("data") val data: List<CompEventDetail>
)

data class CompEventDetail(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("start") val start: String? = null
)

// 2. The detail models (Isolated)
data class CompSkillsResponse(
    @SerializedName("data") val data: List<CompSkillData>
)

data class CompSkillData(
    @SerializedName("type") val type: String,
    @SerializedName("score") val score: Int,
    @SerializedName("event") val event: CompEventRef?
)

data class CompRankingsResponse(
    @SerializedName("data") val data: List<CompRankingData>
)

data class CompRankingData(
    @SerializedName("rank") val rank: Int,
    @SerializedName("wins") val wins: Int,
    @SerializedName("losses") val losses: Int,
    @SerializedName("ties") val ties: Int,
    @SerializedName("event") val event: CompEventRef?
)

data class CompAwardResponse(
    @SerializedName("data") val data: List<CompAwardData>
)

data class CompAwardData(
    @SerializedName("title") val title: String,
    @SerializedName("event") val event: CompEventRef?
)

// This is the critical reference used for filtering
data class CompEventRef(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String? = null
)