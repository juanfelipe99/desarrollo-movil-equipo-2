package com.equipo2.vinilos.data.repository

import com.equipo2.vinilos.data.model.Collector
import com.equipo2.vinilos.data.network.collector.CollectorServiceAdapter

interface CollectorRepository {
    suspend fun getCollectors(): Result<List<Collector>>

    /** Olvida lo guardado en memoria, para que la próxima consulta vaya a la API. Sin caché no hace nada. */
    fun clearCache() {}
}

/** Guarda en memoria el último listado, así volver a la pestaña no repite la petición. Deslizar para recargar llama a [clearCache]. */
class CollectorRepositoryImpl(private val serviceAdapter: CollectorServiceAdapter) : CollectorRepository {

    private var collectors: List<Collector>? = null

    override suspend fun getCollectors(): Result<List<Collector>> =
        collectors?.let { Result.success(it) } ?: resultOf { serviceAdapter.getCollectors() }.onSuccess { collectors = it }

    override fun clearCache() {
        collectors = null
    }
}
