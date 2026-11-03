package com.equipo2.vinilos.data.network.album

import com.equipo2.vinilos.data.model.Album
import com.equipo2.vinilos.data.model.NewAlbum
import com.equipo2.vinilos.data.model.NewTrack
import com.equipo2.vinilos.data.model.Track
import com.equipo2.vinilos.data.network.ApiClient
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.cancellation.CancellationException

class AlbumServiceAdapter(apiClient: ApiClient) {

    private val api = apiClient.create(AlbumApi::class.java)

    suspend fun getAlbums(): List<Album> = api.getAlbums().map { it.toAlbum() }

    suspend fun createAlbum(album: NewAlbum): Album = api.createAlbum(album.toRequest()).toAlbum()

    suspend fun addTrack(albumId: Int, track: NewTrack): Track = api.addTrack(albumId, track.toRequest()).toTrack()

    suspend fun getAlbumById(id: Int): Album = coroutineScope {
        val album = async { api.getAlbumById(id) }
        val authors = async { commentAuthors() }
        album.await().toAlbum(authors.await())
    }

    // Sin los autores el álbum se muestra igual, con un nombre genérico en cada comentario
    private suspend fun commentAuthors(): Map<Int, String> = try {
        api.getCommentAuthors().flatMap { author -> author.comments.map { it.id to author.name } }.toMap()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        emptyMap()
    }
}
