package com.vrumsync.app.service

import com.vrumsync.app.model.Car
import retrofit2.http.Body
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ItemApiService {

    @GET("car")
    suspend fun getItems(): List<Car>

    @GET("car/{id}")
    suspend fun getItem(@Path("id") id: String): Car

//    @DELETE("car/{id}")
//    suspend fun deleteItem(@Path("id") id: String)

    @DELETE("car/{id}")
    suspend fun deleteCar(@Path("id") id: String): Response<Unit>

    @PATCH("car/{id}")
    suspend fun updateItem(
        @Path("id") id: String,
        @Body item: Car
    ): Car

    @POST("car")
    suspend fun addItem(
        @Body item: Car
    ): Car
}