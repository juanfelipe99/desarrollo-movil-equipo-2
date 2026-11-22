package com.equipo2.vinilos.data.network.collector

import retrofit2.http.GET
import retrofit2.http.Path

interface CollectorApi {

    @GET("collectors")
    suspend fun getCollectors(): List<CollectorDto>

    @GET("collectors/{id}")
    suspend fun getCollector(@Path("id") id: Int): CollectorDetailDto

    /** Los álbumes de la colección con su nombre, que collectors/{id} no trae. */
    @GET("collectors/{id}/albums")
    suspend fun getCollectorAlbums(@Path("id") id: Int): List<CollectorAlbumDetailDto>

    /** La API no dice en el comentario de qué álbum es, eso solo aparece en los comentarios de cada álbum. */
    @GET("albums")
    suspend fun getCommentedAlbums(): List<CommentedAlbumDto>
}
