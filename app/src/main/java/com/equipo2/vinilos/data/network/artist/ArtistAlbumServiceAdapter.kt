package com.equipo2.vinilos.data.network.artist

import com.equipo2.vinilos.data.model.AlbumCandidate
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.data.network.ApiClient
import com.equipo2.vinilos.data.network.album.AlbumApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class ArtistAlbumServiceAdapter(apiClient: ApiClient) {

    private val artistApi = apiClient.create(ArtistApi::class.java)
    private val albumApi = apiClient.create(AlbumApi::class.java)

    // El catálogo y el artista se piden a la vez, y si falla cualquiera de los dos falla el listado.
    // Se pide el artista, y no solo el catálogo, para que uno que ya no existe falle con NotFound
    suspend fun getAlbumCandidates(artistId: Int, type: ArtistType): List<AlbumCandidate> = coroutineScope {
        val albums = async { albumApi.getAlbums() }
        val artistAlbums = async {
            when (type) {
                ArtistType.MUSICIAN -> artistApi.getMusician(artistId).albums
                ArtistType.BAND -> artistApi.getBand(artistId).albums
            }
        }
        albumCandidates(albums.await(), artistAlbums.await().mapTo(HashSet()) { it.id })
    }

    suspend fun addAlbum(artistId: Int, type: ArtistType, albumId: Int) = when (type) {
        ArtistType.MUSICIAN -> artistApi.addAlbumToMusician(artistId, albumId)
        ArtistType.BAND -> artistApi.addAlbumToBand(artistId, albumId)
    }
}
