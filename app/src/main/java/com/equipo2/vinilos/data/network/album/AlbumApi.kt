package com.equipo2.vinilos.data.network.album

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AlbumApi {

    @GET("albums")
    suspend fun getAlbums(): List<AlbumDto>

    @POST("albums")
    suspend fun createAlbum(@Body request: CreateAlbumRequest): AlbumDto

    @GET("albums/{id}")
    suspend fun getAlbumById(@Path("id") id: Int): AlbumDto

    @POST("albums/{id}/tracks")
    suspend fun addTrack(@Path("id") albumId: Int, @Body request: AddTrackRequest): TrackDto

    @GET("collectors")
    suspend fun getCommentAuthors(): List<CommentAuthorDto>
}
