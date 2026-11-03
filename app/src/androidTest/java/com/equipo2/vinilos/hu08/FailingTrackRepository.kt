package com.equipo2.vinilos.hu08

import com.equipo2.vinilos.data.model.Album
import com.equipo2.vinilos.data.model.NewAlbum
import com.equipo2.vinilos.data.model.NewTrack
import com.equipo2.vinilos.data.model.Track
import com.equipo2.vinilos.data.repository.AlbumRepository
import com.equipo2.vinilos.data.repository.DataError

/** Usa la API real para todo menos agregar tracks, que falla como si no hubiera conexión. */
class FailingTrackRepository(private val delegate: AlbumRepository) : AlbumRepository {

    override suspend fun getAlbums(): Result<List<Album>> = delegate.getAlbums()

    override suspend fun createAlbum(album: NewAlbum): Result<Album> = delegate.createAlbum(album)

    override suspend fun getAlbumById(id: Int): Result<Album> = delegate.getAlbumById(id)

    override suspend fun addTrack(albumId: Int, track: NewTrack): Result<Track> = Result.failure(DataError.Network())
}
