package com.jayala.vexapp

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RobotEventsService {

    @GET("teams")
    suspend fun getTeamInfo(
        @Query("number[]") teamNumber: String
    ): Response<TeamResponse>

    @GET("teams/{id}/skills")
    suspend fun getTeamSkills(
        @Path("id") teamId: Int,
        @Query("season[]") seasons: List<Int>,
        @Query("per_page") perPage: Int = 250
    ): Response<SkillsResponse>

    @GET("teams/{id}/awards")
    suspend fun getTeamAwards(
        @Path("id") teamId: Int,
        @Query("season[]") seasons: List<Int>,
        @Query("per_page") perPage: Int = 250
    ): Response<AwardsResponse>

    @GET("teams/{id}/events")
    suspend fun getTeamEvents(
        @Path("id") teamId: Int,
        @Query("start") startDate: String? = null,
        @Query("per_page") perPage: Int = 250
    ): Response<EventsResponse>

    @GET("teams/{id}/skills")
    suspend fun getCompSkills(
        @Path("id") teamId: Int,
        @Query("season[]") seasons: List<Int>,
        @Query("per_page") perPage: Int = 250
    ): Response<CompSkillsResponse>

    @GET("teams/{id}/rankings")
    suspend fun getCompRankings(
        @Path("id") teamId: Int,
        @Query("season[]") seasons: List<Int>
    ): Response<CompRankingsResponse>

    @GET("teams/{id}/awards")
    suspend fun getCompAwards(
        @Path("id") teamId: Int,
        @Query("season[]") seasons: List<Int>
    ): Response<CompAwardResponse>

    @GET("teams/{id}/events")
    suspend fun getCompEvents(
        @Path("id") teamId: Int,
        @Query("per_page") perPage: Int = 250
    ): Response<CompEventResponse>

    @GET("seasons")
    suspend fun getSeasons(
        @Query("active") active: Boolean = true
    ): Response<SeasonResponse>

    @GET("events/{id}/skills")
    suspend fun getEventSkills(
        @Path("id") eventId: Int,
        @Query("per_page") perPage: Int = 250
    ): Response<CompSkillsResponse>

    @GET("events/{id}/divisions/{div}/rankings")
    suspend fun getEventRankings(
        @Path("id") eventId: Int,
        @Path("div") divisionId: Int,
        @Query("per_page") perPage: Int = 250
    ): Response<CompResponse<CompRankingData>>

    @GET("events/{id}")
    suspend fun getEventDetails(
        @Path("id") eventId: Int
    ): Response<CompEventDetail>

    @GET("events/{id}/awards")
    suspend fun getEventAwards(
        @Path("id") eventId: Int
    ): Response<CompAwardResponse>

    @GET("events/{id}/teams")
    suspend fun getEventTeams(
        @Path("id") eventId: Int,
        @Query("per_page") perPage: Int = 250
    ): Response<EventTeamsResponse>
}