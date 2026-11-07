package com.equipo2.vinilos.hu12

import android.view.View
import androidx.annotation.StringRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.MusicianCandidate
import com.equipo2.vinilos.data.repository.BandRepository
import com.equipo2.vinilos.hu04.openArtist
import com.equipo2.vinilos.support.enterAs
import com.equipo2.vinilos.support.scrollToWithBarsHidden
import com.equipo2.vinilos.support.waitUntil
import com.equipo2.vinilos.ui.artists.members.MemberCandidateAdapter
import com.equipo2.vinilos.ui.artists.members.MemberRow
import kotlinx.coroutines.CompletableDeferred
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import java.util.Collections

/**
 * Repositorio de bandas en memoria. Da sus respuestas en orden y repite la última,
 * y puede dejar el envío esperando para ver la pantalla mientras se agrega.
 */
class InMemoryBandRepository(candidates: List<MusicianCandidate>) : BandRepository {

    var candidateResponses: List<Result<List<MusicianCandidate>>> = listOf(Result.success(candidates))

    var addResponses: List<Result<Unit>> = listOf(Result.success(Unit))

    /** Si no es null, agregar no responde hasta que la prueba lo complete. */
    var addGate: CompletableDeferred<Unit>? = null

    /** Veces que se pidió el listado. La app las hace en el hilo principal y la prueba las lee en el suyo. */
    val candidateRequests: MutableList<Int> = Collections.synchronizedList(mutableListOf())

    /** Envíos con el id de la banda y del músico, en orden. */
    val added: MutableList<Pair<Int, Int>> = Collections.synchronizedList(mutableListOf())

    override suspend fun getMusicianCandidates(bandId: Int): Result<List<MusicianCandidate>> {
        candidateRequests += bandId
        return candidateResponses[minOf(candidateRequests.size - 1, candidateResponses.lastIndex)]
    }

    override suspend fun addMusician(bandId: Int, musicianId: Int): Result<Unit> {
        added += bandId to musicianId
        addGate?.await()
        return addResponses[minOf(added.size - 1, addResponses.lastIndex)]
    }
}

/** Entra con el rol, abre la pestaña Artistas y el detalle de la banda. */
fun openBand(name: String, collector: Boolean = true) {
    enterAs(collector)
    onView(withId(R.id.artistsGraph)).perform(click())
    openArtist(name)
}

/** Toca Agregar integrante en la sección Integrantes del detalle. */
fun openMemberAdd() {
    onView(withId(R.id.addMemberButton)).perform(scrollToWithBarsHidden(), click())
}

/** Escribe en el buscador y espera a que la lista muestre solo lo que coincide. */
fun searchMusician(text: String) {
    onView(withId(R.id.memberSearchInput)).perform(replaceText(text), closeSoftKeyboard())
    // La lista calcula los cambios en otro hilo, que Espresso no espera
    waitForCandidates { rows -> rows.all { it.candidate.musician.name.contains(text, ignoreCase = true) } }
}

/** Nombre de un músico en el listado de Agregar integrante. */
fun candidateName(name: String): Matcher<View> =
    allOf(withId(R.id.memberName), withText(name), isDescendantOfA(withId(R.id.memberList)))

/** Fila completa del músico, que es la que se toca y la que queda deshabilitada. */
fun candidateRow(name: String): Matcher<View> =
    allOf(withParent(withId(R.id.memberList)), hasDescendant(candidateName(name)))

/** Estado del músico debajo de su nombre: «Sin banda», «Integrante de …» o «Ya es integrante». */
fun candidateStatus(name: String): Matcher<View> = allOf(withId(R.id.memberStatus), hasSibling(candidateName(name)))

/** Aviso «Al agregarlo dejará …» del músico elegido. */
fun candidateLeavesBand(name: String): Matcher<View> = allOf(withId(R.id.memberLeavesBand), hasSibling(candidateName(name)))

fun candidateRadio(name: String): Matcher<View> =
    allOf(withId(R.id.memberRadio), hasSibling(hasDescendant(candidateName(name))))

/** Elige el músico y espera a que su fila se vea elegida. */
fun chooseMusician(name: String) {
    tapMusician(name)
    waitForCandidates { rows -> rows.any { it.selected && it.candidate.musician.name == name } }
}

/** Toca la fila del músico, desplazando antes la lista para que se vea completa en pantallas pequeñas. */
fun tapMusician(name: String) {
    onView(withId(R.id.memberList)).perform(scrollToCandidate(name))
    onView(candidateRow(name)).perform(click())
}

/** Nombres del listado en el orden en que se muestran, incluidos los que no caben en la pantalla. */
fun candidateNamesInOrder(): List<String> = candidateRows().map { it.candidate.musician.name }

/** Filas que tiene la lista ahora. */
private fun candidateRows(): List<MemberRow> = candidateListState().first

/** Filas de la lista y si todavía anima filas que entran o salen. */
private fun candidateListState(): Pair<List<MemberRow>, Boolean> {
    var state = emptyList<MemberRow>() to false
    onView(withId(R.id.memberList)).check { view, noView ->
        val list = (view ?: throw noView) as RecyclerView
        state = (list.adapter as MemberCandidateAdapter).currentList to list.isAnimating
    }
    return state
}

/**
 * Espera a que la lista termine de actualizarse: calcula los cambios en otro hilo y anima las filas
 * que salen en los siguientes cuadros, y Espresso no espera ninguna de las dos cosas.
 */
fun waitForCandidates(condition: (List<MemberRow>) -> Boolean) = waitUntil {
    val (rows, animating) = candidateListState()
    condition(rows) && !animating
}

private fun scrollToCandidate(name: String) = object : ViewAction {
    override fun getConstraints(): Matcher<View> = isAssignableFrom(RecyclerView::class.java)

    override fun getDescription() = "desplazar la lista de músicos hasta «$name»"

    override fun perform(uiController: UiController, view: View) {
        val list = view as RecyclerView
        val position = (list.adapter as MemberCandidateAdapter).currentList.indexOfFirst { it.candidate.musician.name == name }
        check(position >= 0) { "La lista no tiene al músico «$name»" }
        // Arriba de la lista se ve completa; con scrollToPosition puede quedar a medias en una pantalla pequeña
        (list.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, 0)
        uiController.loopMainThreadUntilIdle()
    }
}

/** Fila del detalle de la banda con «Nuevo» junto al integrante. */
fun newMemberBadge(name: String): Matcher<View> =
    allOf(withId(R.id.memberNewBadge), withParent(hasDescendant(allOf(withId(R.id.memberName), withText(name)))))

/**
 * Revisa que se vea el aviso. Puede tardar un instante: si otro aviso se está cerrando, el nuevo espera su turno
 * y Espresso no espera esa cola.
 */
fun checkSnackbar(@StringRes text: Int) {
    waitUntil { runCatching { onView(withText(text)).check(matches(isDisplayed())) }.isSuccess }
}

fun string(id: Int, vararg args: Any): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)
