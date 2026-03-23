package com.vrumsync.app.service

import com.vrumsync.app.database.dao.UserLocationDao
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class GeoLocationInterceptor(private val userLocationDao: UserLocationDao): Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val userLastLocation = runBlocking {
            // apenas mais uma maneira de fazer a busca de um método suspend (NÃO É O JEITO CORRETO)
            userLocationDao.getLastLocation()
        }

        val originalRequest = chain.request()
        val newRequest = userLastLocation?.let {
            originalRequest.newBuilder()
                .addHeader("x-data-latitude", userLastLocation.latitude.toString())
                .addHeader("x-data-longitude", userLastLocation.longitude.toString())
                .build()
        } ?: originalRequest
        return chain.proceed(newRequest)
    }
}