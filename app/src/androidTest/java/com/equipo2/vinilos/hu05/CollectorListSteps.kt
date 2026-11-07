package com.equipo2.vinilos.hu05

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.Collector
import com.equipo2.vinilos.data.repository.CollectorRepository
import com.equipo2.vinilos.data.repository.DataError
import com.equipo2.vinilos.support.enterAs
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
}

fun noConnection(): Result<List<Collector>> = Result.failure(DataError.Network())

fun testCollector(id: Int, name: String, email: String, albumCount: Int) =
    Collector(id = id, name = name, email = email, albumCount = albumCount)

/** Entra como visitante y abre la pestaña Coleccionistas de la barra inferior. */
fun openCollectors() {
    enterAs(collector = false)
    onView(withId(R.id.collectorsGraph)).perform(click())
}

/** Nombre de una fila del listado, sin confundirlo con otros textos de la pantalla. */
fun collectorRowName(name: String): Matcher<View> =
    allOf(withId(R.id.collectorName), withText(name), isDescendantOfA(withId(R.id.collectorList)))
