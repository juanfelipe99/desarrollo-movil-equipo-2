package com.equipo2.vinilos.data.repository

import com.equipo2.vinilos.data.model.Artist
import com.equipo2.vinilos.data.model.ArtistDetail
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.data.network.artist.ArtistServiceAdapter

/** Si algo falla, el Result trae un [DataError] que dice qué pasó. */
interface ArtistRepository {
    suspend fun getArtists(): Result<List<Artist>>

    /** Músicos y bandas se consultan en rutas distintas, por eso se pide con su tipo. Si no existe, falla con [DataError.NotFound]. */
    suspend fun getArtistDetail(id: Int, type: ArtistType): Result<ArtistDetail>
}

class ArtistRepositoryImpl(private val serviceAdapter: ArtistServiceAdapter) : ArtistRepository {

    override suspend fun getArtists(): Result<List<Artist>> = resultOf { serviceAdapter.getArtists() }

    override suspend fun getArtistDetail(id: Int, type: ArtistType): Result<ArtistDetail> =
        resultOf { serviceAdapter.getArtistDetail(id, type) }
}
