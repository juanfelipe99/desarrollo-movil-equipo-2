package com.equipo2.vinilos.data.repository

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
}

class AlbumRepositoryImpl(private val serviceAdapter: AlbumServiceAdapter) : AlbumRepository {

    override suspend fun getAlbums(): Result<List<Album>> = resultOf { serviceAdapter.getAlbums() }

    override suspend fun createAlbum(album: NewAlbum): Result<Album> = resultOf { serviceAdapter.createAlbum(album) }

    override suspend fun getAlbumById(id: Int): Result<Album> = resultOf { serviceAdapter.getAlbumById(id) }

    override suspend fun addTrack(albumId: Int, track: NewTrack): Result<Track> =
        resultOf { serviceAdapter.addTrack(albumId, track) }
}
