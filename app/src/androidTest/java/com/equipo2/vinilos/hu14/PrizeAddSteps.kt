package com.equipo2.vinilos.hu14

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.ArtistPrize
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.data.model.NewPrize
import com.equipo2.vinilos.data.model.Prize
import com.equipo2.vinilos.data.model.PrizeCandidate
import com.equipo2.vinilos.data.repository.PrizeRepository
import com.equipo2.vinilos.hu04.openArtist
import com.equipo2.vinilos.support.enterAs
import com.equipo2.vinilos.support.openTab
import com.equipo2.vinilos.support.scrollToWithBarsHidden
import com.equipo2.vinilos.support.waitUntil
import com.equipo2.vinilos.ui.artists.prizes.PrizeCandidateAdapter
import com.equipo2.vinilos.ui.artists.prizes.PrizeRow
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Collections

/**
 * Repositorio de premios en memoria. Da sus respuestas en orden y repite la última,
 * y puede dejar el envío esperando para ver la pantalla mientras se guarda.
 */
class InMemoryPrizeRepository(candidates: List<PrizeCandidate>) : PrizeRepository {

    var candidateResponses: List<Result<List<PrizeCandidate>>> = listOf(Result.success(candidates))

    /** El listado no responde nunca, para ver la pantalla mientras carga. */
    var neverAnswerCandidates = false

    /** Fallos de los primeros envíos, en orden. Los envíos siguientes agregan el premio. */
    var addFailures: List<Throwable> = emptyList()

    /** Si no es null, guardar no responde hasta que la prueba lo complete. */
    var addGate: CompletableDeferred<Unit>? = null

    /** Artistas cuyo listado se pidió. La app las hace en el hilo principal y la prueba las lee en el suyo. */
    val candidateRequests: MutableList<Pair<Int, ArtistType>> = Collections.synchronizedList(mutableListOf())

    /** Un envío: a quién, qué premio y con qué fecha. */
    data class Added(val artistId: Int, val type: ArtistType, val prizeId: Int, val date: LocalDate)

    /** Envíos, en orden. */
    val added: MutableList<Added> = Collections.synchronizedList(mutableListOf())

    override suspend fun getPrizeCandidates(artistId: Int, type: ArtistType): Result<List<PrizeCandidate>> {
        candidateRequests += artistId to type
        if (neverAnswerCandidates) awaitCancellation()
        return candidateResponses[minOf(candidateRequests.size - 1, candidateResponses.lastIndex)]
    }

    override suspend fun addPrize(artistId: Int, type: ArtistType, prize: Prize, date: LocalDate): Result<ArtistPrize> {
        added += Added(artistId, type, prize.id, date)
        addGate?.await()
        addFailures.getOrNull(added.size - 1)?.let { return Result.failure(it) }
        return Result.success(ArtistPrize(1_000 + added.size, prize.name, prize.organization, date))
    }

    // Las pruebas de HU14 no crean premios
    override suspend fun createPrize(prize: NewPrize): Result<Prize> = Result.failure(UnsupportedOperationException())
}

/** Entra con el rol, abre la pestaña Artistas y el detalle del artista. */
fun openArtistDetail(name: String, collector: Boolean = true) {
    enterAs(collector)
    openTab(R.id.artistsGraph)
    openArtist(name)
}

/** Toca Agregar premio en la sección Premios del detalle. */
fun openPrizeAdd() {
    onView(withId(R.id.addPrizeButton)).perform(scrollToWithBarsHidden(), click())
}

/** Nombre de un premio en el listado de Agregar premio. */
fun candidatePrizeName(name: String): Matcher<View> =
    allOf(withId(R.id.prizeName), withText(name), isDescendantOfA(withId(R.id.prizeList)))

/** Fila completa del premio, que es la que se toca. */
fun candidatePrizeRow(name: String): Matcher<View> =
    allOf(withParent(withId(R.id.prizeList)), hasDescendant(candidatePrizeName(name)))

fun candidatePrizeOrganization(name: String): Matcher<View> =
    allOf(withId(R.id.prizeOrganization), hasSibling(candidatePrizeName(name)))

/** Nota «Ya lo recibió…» del premio. */
fun candidatePrizeReceived(name: String): Matcher<View> = allOf(withId(R.id.prizeReceived), hasSibling(candidatePrizeName(name)))

fun candidatePrizeRadio(name: String): Matcher<View> =
    allOf(withId(R.id.prizeRadio), hasSibling(hasDescendant(candidatePrizeName(name))))

/** Elige el premio, bajando hasta él, y espera a que su fila se vea elegida. */
fun choosePrize(name: String) {
    onView(candidatePrizeRow(name)).perform(scrollTo(), click())
    waitForPrizes { rows -> rows.any { it.selected && it.candidate.prize.name == name } }
}

/** Nombres del listado en el orden en que se muestran, incluidos los que no caben en la pantalla. */
fun candidatePrizeNamesInOrder(): List<String> = prizeListState().first.map { it.candidate.prize.name }

/**
 * Espera a que la lista termine de actualizarse: calcula los cambios en otro hilo y anima las filas
 * en los siguientes cuadros, y Espresso no espera ninguna de las dos cosas.
 */
fun waitForPrizes(condition: (List<PrizeRow>) -> Boolean) = waitUntil {
    val (rows, animating) = prizeListState()
    condition(rows) && !animating
}

private fun prizeListState(): Pair<List<PrizeRow>, Boolean> {
    var state = emptyList<PrizeRow>() to false
    onView(withId(R.id.prizeList)).check { view, noView ->
        val list = (view ?: throw noView) as RecyclerView
        state = (list.adapter as PrizeCandidateAdapter).currentList to list.isAnimating
    }
    return state
}

/** Abre el selector de fecha, que propone el día de hoy, y lo acepta. */
fun pickTodayAsPremiationDate() {
    onView(withId(R.id.premiationDateInput)).perform(scrollTo(), click())
    onView(withText(R.string.action_accept)).inRoot(isDialog()).perform(click())
}

/** El día de hoy como lo muestra el campo de fecha, por ejemplo «13/09/2000». */
fun todayInField(): String = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

fun tapSavePrize() {
    onView(withId(R.id.prizeSaveButton)).perform(click())
}

/** Nombre de un premio en la sección Premios del detalle del artista. */
fun artistPrizeName(name: String): Matcher<View> =
    allOf(withId(R.id.prizeName), withText(name), isDescendantOfA(withId(R.id.prizesList)))

/** «Nuevo» junto al premio en el detalle del artista. */
fun newPrizeBadge(name: String): Matcher<View> =
    allOf(withId(R.id.prizeNewBadge), withParent(hasDescendant(artistPrizeName(name))))

/** Nombres de los premios del detalle en el orden en que se muestran. */
fun artistPrizeNamesInOrder(): List<String> {
    var names = emptyList<String>()
    onView(withId(R.id.prizesList)).check { view, noView ->
        val list = (view ?: throw noView) as ViewGroup
        names = list.children.mapNotNull { it.findViewById<TextView>(R.id.prizeName)?.text?.toString() }.toList()
    }
    return names
}
