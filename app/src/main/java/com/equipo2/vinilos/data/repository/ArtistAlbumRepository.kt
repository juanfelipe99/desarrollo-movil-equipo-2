package com.equipo2.vinilos.data.repository

import com.equipo2.vinilos.data.model.AlbumCandidate
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.data.network.artist.ArtistAlbumServiceAdapter

/** Si algo falla, el Result trae un [DataError] que dice qué pasó. */
interface ArtistAlbumRepository {

    /**
     * Álbumes del catálogo para agregar al artista, marcando los que ya tiene. Músicos y bandas se consultan en rutas
     * distintas, por eso se pide con su tipo. Si el artista no existe, falla con [DataError.NotFound].
     */
    suspend fun getAlbumCandidates(artistId: Int, type: ArtistType): Result<List<AlbumCandidate>>

    /** Agrega el álbum al artista por la ruta de su tipo. Si alguno no existe, falla con [DataError.NotFound]. */
    suspend fun addAlbum(artistId: Int, type: ArtistType, albumId: Int): Result<Unit>
}

/**
 * [onAlbumsChanged] avisa que un artista recibió un álbum, para que quien guarde artistas o álbumes los olvide:
 * cambian los álbumes del artista y los intérpretes del álbum.
 */
class ArtistAlbumRepositoryImpl(
    private val serviceAdapter: ArtistAlbumServiceAdapter,
    private val onAlbumsChanged: () -> Unit = {},
) : ArtistAlbumRepository {

    override suspend fun getAlbumCandidates(artistId: Int, type: ArtistType): Result<List<AlbumCandidate>> =
        resultOf { serviceAdapter.getAlbumCandidates(artistId, type) }

    override suspend fun addAlbum(artistId: Int, type: ArtistType, albumId: Int): Result<Unit> =
        resultOf { serviceAdapter.addAlbum(artistId, type, albumId) }.onSuccess { onAlbumsChanged() }
}
