package com.equipo2.vinilos.support

import com.equipo2.vinilos.data.model.ArtistPrize
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.data.model.Prize
import com.equipo2.vinilos.data.model.PrizeCandidate
import com.equipo2.vinilos.data.repository.PrizeRepository
import kotlinx.coroutines.CompletableDeferred
import java.time.LocalDate

/** Guarda lo que recibe y responde lo que la prueba le indique. */
class FakePrizeRepository : PrizeRepository {

    /** Artistas cuyo listado de premios se pidió, con su tipo, en orden. */
    val candidateRequests = mutableListOf<Pair<Int, ArtistType>>()

    /** Respuesta del listado, que puede esperar un CompletableDeferred para probar la carga. */
    var candidates: suspend () -> Result<List<PrizeCandidate>> = { Result.success(emptyList()) }

    override suspend fun getPrizeCandidates(artistId: Int, type: ArtistType): Result<List<PrizeCandidate>> {
        candidateRequests += artistId to type
        return candidates()
    }

    /** Un envío: a quién, qué premio y con qué fecha. */
    data class Added(val artistId: Int, val type: ArtistType, val prize: Prize, val date: LocalDate)

    /** Envíos, en orden. */
    val added = mutableListOf<Added>()

    /** Respuesta del próximo envío, que la prueba completa cuando quiera. */
    var addResponse = CompletableDeferred<Result<ArtistPrize>>()

    override suspend fun addPrize(artistId: Int, type: ArtistType, prize: Prize, date: LocalDate): Result<ArtistPrize> {
        added += Added(artistId, type, prize, date)
        return addResponse.await()
    }
}

fun prizeCandidate(id: Int, name: String, organization: String = "Organización", vararg received: LocalDate) =
    PrizeCandidate(Prize(id, name, organization), received.toList())
