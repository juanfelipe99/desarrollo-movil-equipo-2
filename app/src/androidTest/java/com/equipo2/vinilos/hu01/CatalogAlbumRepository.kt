package com.equipo2.vinilos.hu01

import com.equipo2.vinilos.data.model.Album
import com.equipo2.vinilos.data.model.NewAlbum
import com.equipo2.vinilos.data.model.NewTrack
import com.equipo2.vinilos.data.model.Track
import com.equipo2.vinilos.data.repository.AlbumRepository
import kotlinx.coroutines.awaitCancellation

/** Repositorio en memoria que da las respuestas del catálogo en orden y repite la última. */
class CatalogAlbumRepository(responses: List<Result<List<Album>>> = emptyList()) : AlbumRepository {

    constructor(response: Result<List<Album>>) : this(listOf(response))

    private val pending = ArrayDeque(responses)
    private var last: Result<List<Album>> = Result.success(emptyList())

    /** Con true el catálogo se queda cargando para siempre. */
    var neverAnswer = false

    override suspend fun getAlbums(): Result<List<Album>> {
        if (neverAnswer) awaitCancellation()
        last = pending.removeFirstOrNull() ?: last
        return last
    }

    override suspend fun getAlbumById(id: Int): Result<Album> = last.mapCatching { albums -> albums.first { it.id == id } }

    override suspend fun createAlbum(album: NewAlbum): Result<Album> = Result.failure(UnsupportedOperationException())

    override suspend fun addTrack(albumId: Int, track: NewTrack): Result<Track> = Result.failure(UnsupportedOperationException())
}
