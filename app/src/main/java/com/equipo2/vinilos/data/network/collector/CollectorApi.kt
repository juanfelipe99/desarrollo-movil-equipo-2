package com.equipo2.vinilos.data.network.collector

import retrofit2.http.GET

interface CollectorApi {

    @GET("collectors")
    suspend fun getCollectors(): List<CollectorDto>
}
