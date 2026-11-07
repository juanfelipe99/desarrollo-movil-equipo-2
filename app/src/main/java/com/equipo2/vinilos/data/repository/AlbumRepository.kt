package com.equipo2.vinilos.data.repository

import androidx.collection.LruCache
import com.equipo2.vinilos.data.model.Album
import com.equipo2.vinilos.data.model.NewAlbum
import com.equipo2.vinilos.data.model.NewTrack
import com.equipo2.vinilos.data.model.Track
import com.equipo2.vinilos.data.network.album.AlbumServiceAdapter

/** Si algo falla, el Result trae un [DataError] que dice qué pasó. */
interface AlbumRepository {
    suspend fun getAlbums(): Result<List<Album>>
    suspend fun createAlbum(album: NewAlbum): Result<Album>
    suspend fun getAlbumById(id: Int): Result<Album>
    suspend fun addTrack(albumId: Int, track: NewTrack): Result<Track>

    /** Olvida lo guardado en memoria, para que la próxima consulta vaya a la API. Sin caché no hace nada. */
    fun clearCache() {}
}

/**
 * Guarda en memoria el catálogo y los últimos álbumes abiertos, así volver a una pantalla no repite la petición.
 * Deslizar para recargar llama a [clearCache], y crear un álbum o agregar un track actualizan lo guardado.
 */
class AlbumRepositoryImpl(private val serviceAdapter: AlbumServiceAdapter) : AlbumRepository {

    private var catalog: List<Album>? = null
    private val details = LruCache<Int, Album>(MAX_CACHED_ALBUMS)

    override suspend fun getAlbums(): Result<List<Album>> =
        catalog?.let { Result.success(it) } ?: resultOf { serviceAdapter.getAlbums() }.onSuccess { catalog = it }

    override suspend fun createAlbum(album: NewAlbum): Result<Album> =
        resultOf { serviceAdapter.createAlbum(album) }.onSuccess { catalog = null }

    override suspend fun getAlbumById(id: Int): Result<Album> =
        details[id]?.let { Result.success(it) } ?: resultOf { serviceAdapter.getAlbumById(id) }.onSuccess { details.put(id, it) }

    override suspend fun addTrack(albumId: Int, track: NewTrack): Result<Track> =
        resultOf { serviceAdapter.addTrack(albumId, track) }.onSuccess { added ->
            details[albumId]?.let { details.put(albumId, it.copy(tracks = it.tracks + added)) }
            catalog = null
        }

    override fun clearCache() {
        catalog = null
        details.evictAll()
    }

    private companion object {
        /** Suficientes para ir y volver entre varios álbumes sin guardar el catálogo entero dos veces. */
        const val MAX_CACHED_ALBUMS = 20
    }
}
