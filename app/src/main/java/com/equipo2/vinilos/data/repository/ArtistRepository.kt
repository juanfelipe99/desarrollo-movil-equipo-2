package com.equipo2.vinilos.data.repository

import androidx.collection.LruCache
import com.equipo2.vinilos.data.model.Artist
import com.equipo2.vinilos.data.model.ArtistDetail
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.data.network.artist.ArtistServiceAdapter

/** Si algo falla, el Result trae un [DataError] que dice qué pasó. */
interface ArtistRepository {
    suspend fun getArtists(): Result<List<Artist>>

    /** Músicos y bandas se consultan en rutas distintas, por eso se pide con su tipo. Si no existe, falla con [DataError.NotFound]. */
    suspend fun getArtistDetail(id: Int, type: ArtistType): Result<ArtistDetail>

    /** Olvida lo guardado en memoria, para que la próxima consulta vaya a la API. Sin caché no hace nada. */
    fun clearCache() {}
}

/**
 * Guarda en memoria el último listado y los últimos artistas abiertos, así volver a una pantalla o reintentar no repite la petición.
 * Deslizar para recargar y agregar un integrante a una banda llaman a [clearCache].
 */
class ArtistRepositoryImpl(private val serviceAdapter: ArtistServiceAdapter) : ArtistRepository {

    private var artists: List<Artist>? = null
    private val details = LruCache<Pair<ArtistType, Int>, ArtistDetail>(MAX_CACHED_ARTISTS)

    override suspend fun getArtists(): Result<List<Artist>> =
        artists?.let { Result.success(it) } ?: resultOf { serviceAdapter.getArtists() }.onSuccess { artists = it }

    override suspend fun getArtistDetail(id: Int, type: ArtistType): Result<ArtistDetail> {
        val key = type to id
        return details[key]?.let { Result.success(it) }
            ?: resultOf { serviceAdapter.getArtistDetail(id, type) }.onSuccess { details.put(key, it) }
    }

    override fun clearCache() {
        artists = null
        details.evictAll()
    }

    private companion object {
        /** Suficientes para ir y volver entre una banda y sus integrantes. */
        const val MAX_CACHED_ARTISTS = 20
    }
}
