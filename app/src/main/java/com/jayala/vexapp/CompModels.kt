package com.jayala.vexapp
import com.google.gson.annotations.SerializedName

data class SeasonsResponse(
    @SerializedName("data") val data: List<Season>
)

data class Season(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("program") val program: ProgramRef
)

data class ProgramRef(
    @SerializedName("id") val id: Int
)

data class Location(
    @SerializedName("venue") val venue: String?,
    @SerializedName("address_1") val address_1: String?,
    @SerializedName("city") val city: String?,
    @SerializedName("region") val region: String?
)

data class CompEventResponse(
    @SerializedName("data") val data: List<CompEventDetail>
)

data class CompEventDetail(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("start") val start: String? = null,
    @SerializedName("program") val program: ProgramRef,
    @SerializedName("season") val season: SeasonRef? = null,
    @SerializedName("location") val location: Location? = null,
    @SerializedName("divisions") val divisions: List<Division>? = null
)

data class SeasonRef(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String? = null
)

data class TeamRef(
    val id: Int,
    val name: String?,
    val code: String?
)

sealed class MatchRankItem {
    data class Header(val divisionName: String) : MatchRankItem()
    data class Rank(val data: CompRankingData) : MatchRankItem()
}

data class AwardWinner(
    @SerializedName("team") val team: TeamRef?
)

data class CompSkillsResponse(
    @SerializedName("data") val data: List<CompSkillData>
)

data class CompSkillData(
    @SerializedName("type") val type: String,
    @SerializedName("score") val score: Int,
    @SerializedName("rank") val rank: Int,
    @SerializedName("event") val event: CompEventRef?,
    @SerializedName("team") val team: TeamRef?
)

data class CompRankingsResponse(
    @SerializedName("data") val data: List<CompRankingData>
)
data class CompRankingData(
    @SerializedName("rank") val rank: Int,
    @SerializedName("team") val team: TeamRef?,
    @SerializedName("wins") val wins: Int,
    @SerializedName("losses") val losses: Int,
    @SerializedName("ties") val ties: Int,
    @SerializedName("wp") val wp: Int,
    @SerializedName("ap") val ap: Int,
    @SerializedName("sp") val sp: Int,
    @SerializedName("event") val event: CompEventRef?
)

data class CompAwardResponse(
    @SerializedName("data") val data: List<CompAwardData>
)

data class CompAwardData(
    @SerializedName("title") val title: String,
    @SerializedName("event") val event: CompEventRef?,
    @SerializedName("teamWinners") val teamWinners: List<AwardWinner>?
)

data class CompEventRef(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String? = null,
    @SerializedName("program") val program: ProgramRef? = null
)

data class CompResponse<T>(
    @SerializedName("data") val data: List<T>,
    @SerializedName("meta") val meta: Map<String, Any>? = null
)

data class Division(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("order") val order: Int
)

data class EventTeamsResponse(
    @SerializedName("data") val data: List<EventTeamData>
)

data class EventTeamData(
    @SerializedName("id") val id: Int,
    @SerializedName("number") val number: String,
    @SerializedName("team_name") val teamName: String?
)

