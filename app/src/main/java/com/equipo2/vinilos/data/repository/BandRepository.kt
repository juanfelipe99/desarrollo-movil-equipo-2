package com.equipo2.vinilos.data.repository

import com.equipo2.vinilos.data.model.MusicianCandidate
import com.equipo2.vinilos.data.network.artist.BandServiceAdapter

/** Si algo falla, el Result trae un [DataError] que dice qué pasó. */
interface BandRepository {

    /** Músicos que se pueden agregar a la banda, con su estado. Si la banda no existe, falla con [DataError.NotFound]. */
    suspend fun getMusicianCandidates(bandId: Int): Result<List<MusicianCandidate>>

    /** Agrega el músico a la banda, sacándolo de la suya si tenía. Si alguno no existe, falla con [DataError.NotFound]. */
    suspend fun addMusician(bandId: Int, musicianId: Int): Result<Unit>
}

/**
 * [onMembersChanged] avisa que una banda cambió de integrantes, para que quien guarde detalles de artistas los olvide.
 */
class BandRepositoryImpl(
    private val serviceAdapter: BandServiceAdapter,
    private val onMembersChanged: () -> Unit = {},
) : BandRepository {

    override suspend fun getMusicianCandidates(bandId: Int): Result<List<MusicianCandidate>> =
        resultOf { serviceAdapter.getMusicianCandidates(bandId) }

    override suspend fun addMusician(bandId: Int, musicianId: Int): Result<Unit> =
        resultOf { serviceAdapter.addMusician(bandId, musicianId) }.onSuccess { onMembersChanged() }
}
