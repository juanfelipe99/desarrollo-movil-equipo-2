package com.equipo2.vinilos.data.network.artist

import com.equipo2.vinilos.data.model.ArtistPrize
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.data.model.NewPrize
import com.equipo2.vinilos.data.model.Prize
import com.equipo2.vinilos.data.model.PrizeCandidate
import com.equipo2.vinilos.data.network.ApiClient
import com.equipo2.vinilos.data.network.toApiDate
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate

/**
 * [prizeCatalog] es el mismo del detalle del artista, así abrir Agregar premio desde el detalle no lo pide otra vez.
 * [receivedDeliveries] da los id de las entregas que ya recibió el artista. La app los toma del detalle que ya tiene
 * en memoria. Sin él, se pide el artista a la API.
 */
class PrizeServiceAdapter(
    apiClient: ApiClient,
    prizeCatalog: PrizeCatalog? = null,
    receivedDeliveries: (suspend (Int, ArtistType) -> Set<Int>)? = null,
) {

    private val api = apiClient.create(ArtistApi::class.java)
    private val prizeCatalog = prizeCatalog ?: PrizeCatalog(api)
    private val receivedDeliveries = receivedDeliveries ?: { artistId, type ->
        when (type) {
            ArtistType.MUSICIAN -> api.getMusician(artistId).performerPrizes
            ArtistType.BAND -> api.getBand(artistId).performerPrizes
        }.mapTo(HashSet()) { it.id }
    }

    // Los premios y el artista se piden a la vez, y si falla cualquiera de los dos falla el listado
    suspend fun getPrizeCandidates(artistId: Int, type: ArtistType): List<PrizeCandidate> = coroutineScope {
        val prizes = async { prizeCatalog.get() }
        val received = async { receivedDeliveries(artistId, type) }
        prizeCandidates(prizes.await(), received.await())
    }

    // El premio nuevo debe salir en la lista al volver a Agregar premio
    suspend fun createPrize(prize: NewPrize): Prize = api.createPrize(prize.toRequest()).toPrize().also { prizeCatalog.clear() }

    /** Entrega el premio al artista en esa fecha. La API responde con la entrega, de la que se usa el id. */
    suspend fun addPrize(artistId: Int, type: ArtistType, prize: Prize, date: LocalDate): ArtistPrize {
        val body = NewPerformerPrizeDto(toApiDate(date))
        val delivery = when (type) {
            ArtistType.MUSICIAN -> api.addPrizeToMusician(prize.id, artistId, body)
            ArtistType.BAND -> api.addPrizeToBand(prize.id, artistId, body)
        }
        // La entrega nueva cambia las fechas que muestra Agregar premio
        prizeCatalog.clear()
        return ArtistPrize(delivery.id, prize.name, prize.organization, date)
    }
}
