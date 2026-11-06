package com.equipo2.vinilos.hu12

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
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
import com.equipo2.vinilos.ui.artists.members.MemberCandidateAdapter
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

fun searchMusician(text: String) {
    onView(withId(R.id.memberSearchInput)).perform(replaceText(text), closeSoftKeyboard())
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

fun chooseMusician(name: String) {
    onView(candidateRow(name)).perform(click())
}

/** Nombres del listado en el orden en que se muestran, incluidos los que no caben en la pantalla. */
fun candidateNamesInOrder(): List<String> {
    var names = emptyList<String>()
    onView(withId(R.id.memberList)).check { view, noView ->
        val adapter = ((view ?: throw noView) as RecyclerView).adapter as MemberCandidateAdapter
        names = adapter.currentList.map { it.candidate.musician.name }
    }
    return names
}

/** Fila del detalle de la banda con «Nuevo» junto al integrante. */
fun newMemberBadge(name: String): Matcher<View> =
    allOf(withId(R.id.memberNewBadge), withParent(hasDescendant(allOf(withId(R.id.memberName), withText(name)))))

fun string(id: Int, vararg args: Any): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)
