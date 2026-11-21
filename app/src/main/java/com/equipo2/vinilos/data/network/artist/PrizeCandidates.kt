package com.equipo2.vinilos.data.network.artist

import com.equipo2.vinilos.data.model.NewPrize
import com.equipo2.vinilos.data.model.Prize
import com.equipo2.vinilos.data.model.PrizeCandidate
import com.equipo2.vinilos.data.model.byName
import com.equipo2.vinilos.data.network.fromApiDate
import kotlinx.serialization.Serializable

/** Cuerpo para entregar un premio a un artista. Sin la fecha la API responde 500. */
@Serializable
data class NewPerformerPrizeDto(val premiationDate: String)

/** Cuerpo para crear un premio. La API valida que los tres sean texto, pero acepta uno con solo espacios. */
@Serializable
data class NewPrizeDto(val name: String, val organization: String, val description: String)

internal fun NewPrize.toRequest() = NewPrizeDto(name, organization, description)

internal fun PrizeDto.toPrize() = Prize(id, name, organization)

/**
 * Premios del catálogo en orden alfabético, cada uno con las fechas en que el artista ya lo recibió.
 * [received] son las entregas del artista, que se buscan por su id entre las de cada premio.
 */
internal fun prizeCandidates(prizes: List<PrizeDto>, received: List<PerformerPrizeDto>): List<PrizeCandidate> =
    prizeCandidates(prizes, received.mapTo(HashSet()) { it.id })

/** Lo mismo con los id de las entregas del artista, por ejemplo los del detalle que la app ya tiene. */
internal fun prizeCandidates(prizes: List<PrizeDto>, receivedIds: Set<Int>): List<PrizeCandidate> {
    return prizes
        .map { prize ->
            val dates = prize.performerPrizes.filter { it.id in receivedIds }.map { fromApiDate(it.premiationDate) }
            PrizeCandidate(prize.toPrize(), dates.sorted())
        }
        .sortedWith(byName { it.prize.name })
}
