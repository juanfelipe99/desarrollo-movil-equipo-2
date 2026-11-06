package com.equipo2.vinilos.data.network.artist

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ArtistApi {

    @GET("musicians")
    suspend fun getMusicians(): List<MusicianDto>

    @GET("bands")
    suspend fun getBands(): List<BandDto>

    // Si el músico estaba en otra banda, la API lo cambia a esta. La respuesta es la banda, que no se usa
    @POST("bands/{bandId}/musicians/{musicianId}")
    suspend fun addMusicianToBand(@Path("bandId") bandId: Int, @Path("musicianId") musicianId: Int)
}
