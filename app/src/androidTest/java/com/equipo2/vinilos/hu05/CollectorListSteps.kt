package com.equipo2.vinilos.hu05

import android.view.View
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.Collector
import com.equipo2.vinilos.data.model.CollectorDetail
import com.equipo2.vinilos.data.repository.CollectorRepository
import com.equipo2.vinilos.data.repository.DataError
import com.equipo2.vinilos.support.enterAs
import com.equipo2.vinilos.support.openTab
import kotlinx.coroutines.awaitCancellation
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf

/** Repositorio en memoria que da sus respuestas en orden y repite la última, o que nunca responde. Cuenta las peticiones. */
class InMemoryCollectorRepository(private val responses: List<Result<List<Collector>>>) : CollectorRepository {

    constructor(response: Result<List<Collector>> = Result.success(emptyList())) : this(listOf(response))

    var neverAnswer = false

    /** Consultas que hizo la app, para comprobar que Reintentar pide otra vez. */
    @Volatile
    var requests = 0
        private set

    override suspend fun getCollectors(): Result<List<Collector>> {
        requests++
        if (neverAnswer) awaitCancellation()
        return responses[minOf(requests - 1, responses.lastIndex)]
    }

    /** Detalle de cada coleccionista. Si no se indica, el del listado sin teléfono, favoritos, álbumes ni comentarios. */
    var details: Map<Int, CollectorDetail> = emptyMap()

    override suspend fun getCollectorDetail(id: Int): Result<CollectorDetail> {
        details[id]?.let { return Result.success(it) }
        val collector = responses.firstNotNullOfOrNull { response -> response.getOrNull()?.firstOrNull { it.id == id } }
            ?: return Result.failure(DataError.NotFound())
        return Result.success(CollectorDetail(collector = collector.copy(albumCount = 0), telephone = ""))
    }
}

fun noConnection(): Result<List<Collector>> = Result.failure(DataError.Network())

fun testCollector(id: Int, name: String, email: String, albumCount: Int) =
    Collector(id = id, name = name, email = email, albumCount = albumCount)

/** Entra como visitante y abre la pestaña Coleccionistas de la barra inferior. */
fun openCollectors() {
    enterAs(collector = false)
    openTab(R.id.collectorsGraph)
}

/** Nombre de una fila del listado, sin confundirlo con otros textos de la pantalla. */
fun collectorRowName(name: String): Matcher<View> =
    allOf(withId(R.id.collectorName), withText(name), isDescendantOfA(withId(R.id.collectorList)))
