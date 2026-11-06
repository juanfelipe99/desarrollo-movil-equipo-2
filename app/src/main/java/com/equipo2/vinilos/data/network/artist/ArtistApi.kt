package com.equipo2.vinilos.data.network.artist

import retrofit2.http.GET
import retrofit2.http.Path

interface ArtistApi {

    @GET("musicians")
    suspend fun getMusicians(): List<MusicianDto>

    @GET("bands")
    suspend fun getBands(): List<BandDto>

    @GET("musicians/{id}")
    suspend fun getMusician(@Path("id") id: Int): MusicianDetailDto

    @GET("bands/{id}")
    suspend fun getBand(@Path("id") id: Int): BandDetailDto

    @GET("prizes")
    suspend fun getPrizes(): List<PrizeDto>
}
