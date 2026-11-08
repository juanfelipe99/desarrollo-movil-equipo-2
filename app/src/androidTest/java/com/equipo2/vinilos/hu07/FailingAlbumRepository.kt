package com.equipo2.vinilos.hu07

import com.equipo2.vinilos.data.model.Album
import com.equipo2.vinilos.data.model.NewAlbum
import com.equipo2.vinilos.data.model.NewTrack
import com.equipo2.vinilos.data.model.Track
import com.equipo2.vinilos.data.repository.AlbumRepository
import com.equipo2.vinilos.data.repository.DataError

/** Repositorio que siempre falla como si no hubiera conexión. */
class FailingAlbumRepository : AlbumRepository {

    override suspend fun getAlbums(): Result<List<Album>> = Result.failure(DataError.Network())

    override suspend fun createAlbum(album: NewAlbum): Result<Album> = Result.failure(DataError.Network())

    override suspend fun getAlbumById(id: Int): Result<Album> = Result.failure(DataError.Network())

    override suspend fun addTrack(albumId: Int, track: NewTrack): Result<Track> = Result.failure(DataError.Network())
}
