
package com.equipo2.vinilos.hu02

import com.equipo2.vinilos.data.model.Album
import com.equipo2.vinilos.data.model.NewAlbum
import com.equipo2.vinilos.data.model.NewTrack
import com.equipo2.vinilos.data.model.Track
import com.equipo2.vinilos.data.repository.AlbumRepository
import com.equipo2.vinilos.data.repository.DataError

class FailingAlbumDetailRepository(
    private val delegate: AlbumRepository
) : AlbumRepository {

    var detailRequests = 0
        private set

    override suspend fun getAlbums(): Result<List<Album>> =
        delegate.getAlbums()

    override suspend fun createAlbum(album: NewAlbum): Result<Album> =
        delegate.createAlbum(album)

    override suspend fun addTrack(albumId: Int, track: NewTrack): Result<Track> =
        delegate.addTrack(albumId, track)

    override suspend fun getAlbumById(id: Int): Result<Album> {
        detailRequests++
        return Result.failure(DataError.Network())
    }
}
