package com.equipo2.vinilos.data.network.artist

import com.equipo2.vinilos.data.model.Artist
import com.equipo2.vinilos.data.model.ArtistDetail
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.data.network.ApiClient
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** [prizeCatalog] se comparte con Agregar premio. Sin él, el adaptador guarda su propio catálogo. */
class ArtistServiceAdapter(apiClient: ApiClient, prizeCatalog: PrizeCatalog? = null) {

    private val api = apiClient.create(ArtistApi::class.java)
    private val prizeCatalog = prizeCatalog ?: PrizeCatalog(api)

    // Cada detalle necesita las bandas o los premios completos. Se guardan para no pedirlos otra vez en cada artista
    @Volatile private var bands: List<BandDto>? = null

    // Músicos y bandas se piden a la vez, y si falla cualquiera de los dos falla el listado
    suspend fun getArtists(): List<Artist> = coroutineScope {
        val musicians = async { api.getMusicians() }
        val bands = async { api.getBands().also { bands = it } }
        musicians.await().map { it.toArtist() } + bands.await().map { it.toArtist() }
    }

    /**
     * Detalle de un músico o de una banda, con los premios ya nombrados.
     *
     * Las peticiones van a la vez y, si falla cualquiera, falla el detalle: mostrar «sin banda»
     * o «sin premios» por un error sería mostrar algo falso.
     */
    suspend fun getArtistDetail(id: Int, type: ArtistType): ArtistDetail = when (type) {
        ArtistType.MUSICIAN -> getMusicianDetail(id)
        ArtistType.BAND -> getBandDetail(id)
    }

    // La API no dice en el músico a qué banda pertenece, eso solo aparece en los integrantes de cada banda
    private suspend fun getMusicianDetail(id: Int): ArtistDetail = coroutineScope {
        val musician = async { api.getMusician(id) }
        val bands = async { bands ?: api.getBands().also { bands = it } }
        val prizes = async { prizes() }

        val band = bands.await().firstOrNull { band -> band.musicians.any { it.id == id } }
        musician.await().toArtistDetail(band?.toArtist(), prizes.await())
    }

    private suspend fun getBandDetail(id: Int): ArtistDetail = coroutineScope {
        val band = async { api.getBand(id) }
        val prizes = async { prizes() }
        band.await().toArtistDetail(prizes.await())
    }

    private suspend fun prizes(): Map<Int, PrizeDto> = prizeCatalog.get().byPerformerPrize()

    /** Olvida las bandas y los premios guardados, para que el próximo detalle los pida a la API. */
    fun clearCache() {
        bands = null
        prizeCatalog.clear()
    }
}
