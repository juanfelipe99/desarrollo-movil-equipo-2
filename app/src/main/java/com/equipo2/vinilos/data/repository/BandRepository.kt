package com.equipo2.vinilos.data.repository

import com.equipo2.vinilos.data.model.MusicianCandidate
import com.equipo2.vinilos.data.network.artist.BandServiceAdapter

/** Si algo falla, el Result trae un [DataError] que dice qué pasó. */
interface BandRepository {

    /** Músicos que se pueden agregar a la banda, con su estado. Si la banda no existe, falla con [DataError.NotFound]. */
    suspend fun getMusicianCandidates(bandId: Int): Result<List<MusicianCandidate>>
}

class BandRepositoryImpl(private val serviceAdapter: BandServiceAdapter) : BandRepository {

    override suspend fun getMusicianCandidates(bandId: Int): Result<List<MusicianCandidate>> =
        resultOf { serviceAdapter.getMusicianCandidates(bandId) }
}
