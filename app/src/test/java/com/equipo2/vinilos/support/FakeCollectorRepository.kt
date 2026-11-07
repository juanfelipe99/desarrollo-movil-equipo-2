package com.equipo2.vinilos.support

import com.equipo2.vinilos.data.model.Collector
import com.equipo2.vinilos.data.repository.CollectorRepository
import kotlinx.coroutines.CompletableDeferred

/** Cuenta las consultas y responde lo que la prueba le indique. */
class FakeCollectorRepository : CollectorRepository {

    var calls = 0
        private set

    /** Respuesta de la próxima consulta, que la prueba completa cuando quiera. */
    var response = CompletableDeferred<Result<List<Collector>>>()

    override suspend fun getCollectors(): Result<List<Collector>> {
        calls++
        return response.await()
    }
}

fun collector(id: Int, name: String, albumCount: Int = 0) =
    Collector(id, name, email = "${name.lowercase().replace(' ', '.')}@ejemplo.com", albumCount = albumCount)
