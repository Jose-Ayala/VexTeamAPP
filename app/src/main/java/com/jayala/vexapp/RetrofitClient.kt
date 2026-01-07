package com.jayala.vexapp

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.jayala.vexapp.BuildConfig

object RetrofitClient {
    private const val BASE_URL = "https://www.robotevents.com/api/v2/"

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    private val client = OkHttpClient.Builder().addInterceptor { chain ->
        val original = chain.request()

        val request = original.newBuilder()
            .header("Authorization", BuildConfig.ROBOT_EVENTS_TOKEN)
            .header("User-Agent", "VexTeamApp/1.0")
            .build()

        chain.proceed(request)
    }.build()

    val service: RobotEventsService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(RobotEventsService::class.java)
    }
}