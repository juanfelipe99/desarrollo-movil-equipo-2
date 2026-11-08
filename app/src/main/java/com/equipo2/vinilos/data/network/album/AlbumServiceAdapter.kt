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

    // Los autores son la lista completa de coleccionistas, la misma para todos los álbumes. Se guarda para no pedirla en cada uno
    @Volatile private var authors: Map<Int, String>? = null

    suspend fun getAlbums(): List<Album> = api.getAlbums().map { it.toAlbum() }

    suspend fun createAlbum(album: NewAlbum): Album = api.createAlbum(album.toRequest()).toAlbum()

    suspend fun addTrack(albumId: Int, track: NewTrack): Track = api.addTrack(albumId, track.toRequest()).toTrack()

    suspend fun getAlbumById(id: Int): Album = coroutineScope {
        val album = async { api.getAlbumById(id) }
        val authors = async { commentAuthors() }
        album.await().toAlbum(authors.await())
    }

    // Sin los autores el álbum se muestra igual, con un nombre genérico en cada comentario.
    // Solo se guarda una respuesta buena, así un fallo no deja los nombres genéricos para siempre
    private suspend fun commentAuthors(): Map<Int, String> = authors ?: try {
        api.getCommentAuthors().flatMap { author -> author.comments.map { it.id to author.name } }.toMap().also { authors = it }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        emptyMap()
    }

    /** Olvida los autores guardados, para que el próximo álbum los pida a la API. */
    fun clearCache() {
        authors = null
    }
}
