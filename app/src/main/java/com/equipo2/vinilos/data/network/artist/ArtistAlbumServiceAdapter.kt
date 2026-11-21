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
 * que se abrió el catálogo, así Agregar álbum no lo pide otra vez. [artistAlbums] da los id de los álbumes que ya
 * tiene el artista, y la app los toma del detalle que acaba de mostrar. Sin ellos, los dos se piden a la API.
 */
class ArtistAlbumServiceAdapter(
    apiClient: ApiClient,
    catalog: (suspend () -> List<Album>)? = null,
    artistAlbums: (suspend (Int, ArtistType) -> Set<Int>)? = null,
) {

    private val artistApi = apiClient.create(ArtistApi::class.java)
    private val albumApi = apiClient.create(AlbumApi::class.java)
    private val catalog = catalog ?: { albumApi.getAlbums().map { it.toAlbum() } }
    private val artistAlbums = artistAlbums ?: { artistId, type ->
        when (type) {
            ArtistType.MUSICIAN -> artistApi.getMusician(artistId).albums
            ArtistType.BAND -> artistApi.getBand(artistId).albums
        }.mapTo(HashSet()) { it.id }
    }

    // El catálogo y los álbumes del artista se buscan a la vez, y si falla cualquiera de los dos falla el listado.
    // Se busca el artista, y no solo el catálogo, para que uno que ya no existe falle con NotFound
    suspend fun getAlbumCandidates(artistId: Int, type: ArtistType): List<AlbumCandidate> = coroutineScope {
        val albums = async { catalog() }
        val owned = async { artistAlbums(artistId, type) }
        albumCandidates(albums.await(), owned.await())
    }

    suspend fun addAlbum(artistId: Int, type: ArtistType, albumId: Int) = when (type) {
        ArtistType.MUSICIAN -> artistApi.addAlbumToMusician(artistId, albumId)
        ArtistType.BAND -> artistApi.addAlbumToBand(artistId, albumId)
    }
}
