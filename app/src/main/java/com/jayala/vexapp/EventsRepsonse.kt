package com.jayala.vexapp

import com.google.gson.annotations.SerializedName

data class EventsResponse(
    @SerializedName("data") val data: List<EventDetail>
)

data class EventDetail(
    @SerializedName("id") val id: Int,
    @SerializedName("start") val start: String?
)