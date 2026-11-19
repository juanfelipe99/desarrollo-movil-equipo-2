package com.equipo2.vinilos.data.repository

import com.equipo2.vinilos.data.model.ArtistPrize
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.data.model.NewPrize
import com.equipo2.vinilos.data.model.Prize
import com.equipo2.vinilos.data.model.PrizeCandidate
import com.equipo2.vinilos.data.network.artist.PrizeServiceAdapter
import java.time.LocalDate

/** Si algo falla, el Result trae un [DataError] que dice qué pasó. */
interface PrizeRepository {

    /** Premios que se pueden agregar al artista, con las fechas en que ya los recibió. Si el artista no existe, falla con [DataError.NotFound]. */
    suspend fun getPrizeCandidates(artistId: Int, type: ArtistType): Result<List<PrizeCandidate>>

    /**
     * Entrega el premio al artista en esa fecha y devuelve la entrega. Si alguno no existe, falla con [DataError.NotFound],
     * y si la API no acepta la fecha, con [DataError.Request].
     */
    suspend fun addPrize(artistId: Int, type: ArtistType, prize: Prize, date: LocalDate): Result<ArtistPrize>

    /** Crea el premio en el catálogo y lo devuelve con su id. Si la API no acepta los datos, falla con [DataError.Request]. */
    suspend fun createPrize(prize: NewPrize): Result<Prize>
}

/**
 * [onPrizesChanged] avisa que un artista recibió un premio, para que quien guarde detalles de artistas los olvide.
 */
class PrizeRepositoryImpl(
    private val serviceAdapter: PrizeServiceAdapter,
    private val onPrizesChanged: () -> Unit = {},
) : PrizeRepository {

    override suspend fun getPrizeCandidates(artistId: Int, type: ArtistType): Result<List<PrizeCandidate>> =
        resultOf { serviceAdapter.getPrizeCandidates(artistId, type) }

    override suspend fun addPrize(artistId: Int, type: ArtistType, prize: Prize, date: LocalDate): Result<ArtistPrize> =
        resultOf { serviceAdapter.addPrize(artistId, type, prize, date) }.onSuccess { onPrizesChanged() }

    // Un premio recién creado no se ha entregado a nadie, así que ningún detalle de artista guardado cambia
    override suspend fun createPrize(prize: NewPrize): Result<Prize> =
        resultOf { serviceAdapter.createPrize(prize) }
}
