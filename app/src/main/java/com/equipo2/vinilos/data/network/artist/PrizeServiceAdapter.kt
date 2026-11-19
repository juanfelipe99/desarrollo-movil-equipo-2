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

class PrizeServiceAdapter(apiClient: ApiClient) {

    private val api = apiClient.create(ArtistApi::class.java)

    // Los premios y el artista se piden a la vez, y si falla cualquiera de los dos falla el listado
    suspend fun getPrizeCandidates(artistId: Int, type: ArtistType): List<PrizeCandidate> = coroutineScope {
        val prizes = async { api.getPrizes() }
        val received = async {
            when (type) {
                ArtistType.MUSICIAN -> api.getMusician(artistId).performerPrizes
                ArtistType.BAND -> api.getBand(artistId).performerPrizes
            }
        }
        prizeCandidates(prizes.await(), received.await())
    }

    suspend fun createPrize(prize: NewPrize): Prize = api.createPrize(prize.toRequest()).toPrize()

    /** Entrega el premio al artista en esa fecha. La API responde con la entrega, de la que se usa el id. */
    suspend fun addPrize(artistId: Int, type: ArtistType, prize: Prize, date: LocalDate): ArtistPrize {
        val body = NewPerformerPrizeDto(toApiDate(date))
        val delivery = when (type) {
            ArtistType.MUSICIAN -> api.addPrizeToMusician(prize.id, artistId, body)
            ArtistType.BAND -> api.addPrizeToBand(prize.id, artistId, body)
        }
        return ArtistPrize(delivery.id, prize.name, prize.organization, date)
    }
}
