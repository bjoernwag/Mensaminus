package com.bjwag.mensaminus.api

import com.bjwag.mensaminus.model.Canteen
import com.bjwag.mensaminus.model.CanteenDay
import com.bjwag.mensaminus.model.Meal
import retrofit2.http.GET
import retrofit2.http.Path

interface OpenMensaApi {
    @GET("canteens")
    suspend fun getCanteens(): List<Canteen>

    //TODO needed?
    @GET("canteens/{canteenId}/days")
    suspend fun getCanteenDays(
        @Path("canteenId") canteenId: Int
    ): List<CanteenDay>

    @GET("canteens/{canteenId}/days/{date}/meals")
    suspend fun getMeals(
        @Path("canteenId") canteenId: Int,
        @Path("date") date: String
    ): List<Meal>
}