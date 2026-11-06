package com.equipo2.vinilos.support

import com.equipo2.vinilos.data.model.MusicianCandidate
import com.equipo2.vinilos.data.model.MusicianStatus
import com.equipo2.vinilos.data.repository.BandRepository
import kotlinx.coroutines.CompletableDeferred

/** Guarda lo que recibe y responde lo que la prueba le indique. */
class FakeBandRepository : BandRepository {

    /** Bandas cuyo listado se pidió, en orden. */
    val candidateRequests = mutableListOf<Int>()

    /** Respuesta del listado, que puede esperar un CompletableDeferred para probar la carga. */
    var candidates: suspend () -> Result<List<MusicianCandidate>> = { Result.success(emptyList()) }

    override suspend fun getMusicianCandidates(bandId: Int): Result<List<MusicianCandidate>> {
        candidateRequests += bandId
        return candidates()
    }

    /** Envíos con el id de la banda y del músico, en orden. */
    val added = mutableListOf<Pair<Int, Int>>()

    /** Respuesta del próximo envío, que la prueba completa cuando quiera. */
    var addResponse = CompletableDeferred<Result<Unit>>()

    override suspend fun addMusician(bandId: Int, musicianId: Int): Result<Unit> {
        added += bandId to musicianId
        return addResponse.await()
    }
}

fun candidate(id: Int, name: String, status: MusicianStatus = MusicianStatus.NoBand) =
    MusicianCandidate(musician(id, name), status)
