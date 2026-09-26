package com.bjwag.mensaminus.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://api.studentenwerk-dresden.de/openmensa/v2/"
    //don't really know where else to put this const, maybe centralise?

    val api: OpenMensaApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenMensaApi::class.java)
    }
}