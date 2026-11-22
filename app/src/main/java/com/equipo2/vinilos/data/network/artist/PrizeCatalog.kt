package com.equipo2.vinilos.data.network.artist

/**
 * Catálogo de premios de la API guardado en memoria y compartido por el detalle del artista (HU04) y Agregar premio
 * (HU14). Así, al abrir Agregar premio desde el detalle, los premios ya están y no se piden otra vez.
 * Cada premio trae sus entregas, por eso se olvida al entregar o crear un premio y al recargar los artistas.
 */
class PrizeCatalog(private val api: ArtistApi) {

    @Volatile private var prizes: List<PrizeDto>? = null

    suspend fun get(): List<PrizeDto> = prizes ?: api.getPrizes().also { prizes = it }

    fun clear() {
        prizes = null
    }
}
