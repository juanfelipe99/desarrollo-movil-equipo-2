package com.equipo2.vinilos.data.network.artist

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
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

    // Si el músico estaba en otra banda, la API lo cambia a esta. La respuesta es la banda, que no se usa
    @POST("bands/{bandId}/musicians/{musicianId}")
    suspend fun addMusicianToBand(@Path("bandId") bandId: Int, @Path("musicianId") musicianId: Int)

    // Músicos y bandas reciben premios en rutas distintas. La respuesta es la entrega, con su id
    @POST("prizes/{prizeId}/musicians/{musicianId}")
    suspend fun addPrizeToMusician(
        @Path("prizeId") prizeId: Int,
        @Path("musicianId") musicianId: Int,
        @Body prize: NewPerformerPrizeDto,
    ): PerformerPrizeDto

    @POST("prizes/{prizeId}/bands/{bandId}")
    suspend fun addPrizeToBand(
        @Path("prizeId") prizeId: Int,
        @Path("bandId") bandId: Int,
        @Body prize: NewPerformerPrizeDto,
    ): PerformerPrizeDto

    // Músicos y bandas reciben álbumes en rutas distintas. La respuesta no se usa: el álbum ya se tiene del catálogo
    @POST("musicians/{musicianId}/albums/{albumId}")
    suspend fun addAlbumToMusician(@Path("musicianId") musicianId: Int, @Path("albumId") albumId: Int)

    @POST("bands/{bandId}/albums/{albumId}")
    suspend fun addAlbumToBand(@Path("bandId") bandId: Int, @Path("albumId") albumId: Int)
}
