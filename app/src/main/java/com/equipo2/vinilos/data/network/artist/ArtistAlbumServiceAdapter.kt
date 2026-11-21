package com.equipo2.vinilos.data.network.artist

import com.equipo2.vinilos.data.model.Album
import com.equipo2.vinilos.data.model.AlbumCandidate
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.data.network.ApiClient
import com.equipo2.vinilos.data.network.album.AlbumApi
import com.equipo2.vinilos.data.network.album.toAlbum
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * [catalog] da el catálogo de álbumes. La app le pasa el del repositorio de álbumes, que ya lo tiene en memoria desde
 * que se abrió el catálogo, así Agregar álbum no lo pide otra vez. Sin él, se pide a la API.
 */
class ArtistAlbumServiceAdapter(apiClient: ApiClient, catalog: (suspend () -> List<Album>)? = null) {

    private val artistApi = apiClient.create(ArtistApi::class.java)
    private val albumApi = apiClient.create(AlbumApi::class.java)
    private val catalog = catalog ?: { albumApi.getAlbums().map { it.toAlbum() } }

    // El catálogo y el artista se piden a la vez, y si falla cualquiera de los dos falla el listado.
    // Se pide el artista, y no solo el catálogo, para que uno que ya no existe falle con NotFound
    suspend fun getAlbumCandidates(artistId: Int, type: ArtistType): List<AlbumCandidate> = coroutineScope {
        val albums = async { catalog() }
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
