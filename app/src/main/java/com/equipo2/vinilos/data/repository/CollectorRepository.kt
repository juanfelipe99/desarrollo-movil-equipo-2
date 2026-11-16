package com.equipo2.vinilos.data.repository

import androidx.collection.LruCache
import com.equipo2.vinilos.data.model.Collector
import com.equipo2.vinilos.data.model.CollectorDetail
import com.equipo2.vinilos.data.network.collector.CollectorServiceAdapter

/** Si algo falla, el Result trae un [DataError] que dice qué pasó. */
interface CollectorRepository {
    suspend fun getCollectors(): Result<List<Collector>>

    /** Detalle de un coleccionista (HU06). Si no existe, falla con [DataError.NotFound]. */
    suspend fun getCollectorDetail(id: Int): Result<CollectorDetail>

    /** Olvida lo guardado en memoria, para que la próxima consulta vaya a la API. Sin caché no hace nada. */
    fun clearCache() {}
}

/**
 * Guarda en memoria el último listado y los últimos coleccionistas abiertos, así volver a una pantalla o reintentar
 * no repite la petición. Deslizar para recargar llama a [clearCache].
 */
class CollectorRepositoryImpl(private val serviceAdapter: CollectorServiceAdapter) : CollectorRepository {

    private var collectors: List<Collector>? = null
    private val details = LruCache<Int, CollectorDetail>(MAX_CACHED_COLLECTORS)

    override suspend fun getCollectors(): Result<List<Collector>> =
        collectors?.let { Result.success(it) } ?: resultOf { serviceAdapter.getCollectors() }.onSuccess { collectors = it }

    override suspend fun getCollectorDetail(id: Int): Result<CollectorDetail> =
        details[id]?.let { Result.success(it) } ?: resultOf { serviceAdapter.getCollectorDetail(id) }.onSuccess { details.put(id, it) }

    override fun clearCache() {
        collectors = null
        details.evictAll()
        serviceAdapter.clearCache()
    }

    private companion object {
        /** Suficientes para ir y volver entre varios coleccionistas sin guardar todos. */
        const val MAX_CACHED_COLLECTORS = 20
    }
}
