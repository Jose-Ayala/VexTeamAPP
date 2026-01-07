package com.jayala.vexapp

data class SkillsResponse(
    val data: List<SkillsData>
)

data class SkillsData(
    val rank: Int,
    val score: Int,
    val attempts: Int,
    val type: String,
    val event: SkillsEvent,
    val season: SkillsSeason
)

data class SkillsSeason(
    val id: Int,
    val name: String
)

data class SkillsEvent(
    val id: Int,
    val name: String,
    val start: String?
)