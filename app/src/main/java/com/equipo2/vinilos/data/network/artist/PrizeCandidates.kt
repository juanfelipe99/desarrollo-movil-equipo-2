package com.equipo2.vinilos.data.network.artist

import com.equipo2.vinilos.data.model.Prize
import com.equipo2.vinilos.data.model.PrizeCandidate
import com.equipo2.vinilos.data.model.byName
import com.equipo2.vinilos.data.network.fromApiDate
import kotlinx.serialization.Serializable

/** Cuerpo para entregar un premio a un artista. Sin la fecha la API responde 500. */
@Serializable
data class NewPerformerPrizeDto(val premiationDate: String)

internal fun PrizeDto.toPrize() = Prize(id, name, organization)

/**
 * Premios del catálogo en orden alfabético, cada uno con las fechas en que el artista ya lo recibió.
 * [received] son las entregas del artista, que se buscan por su id entre las de cada premio.
 */
internal fun prizeCandidates(prizes: List<PrizeDto>, received: List<PerformerPrizeDto>): List<PrizeCandidate> {
    val receivedIds = received.mapTo(HashSet()) { it.id }
    return prizes
        .map { prize ->
            val dates = prize.performerPrizes.filter { it.id in receivedIds }.map { fromApiDate(it.premiationDate) }
            PrizeCandidate(prize.toPrize(), dates.sorted())
        }
        .sortedWith(byName { it.prize.name })
}
