package com.equipo2.vinilos.hu15

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.AlbumCandidate
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.data.repository.ArtistAlbumRepository
import com.equipo2.vinilos.hu04.openArtist
import com.equipo2.vinilos.support.enterAs
import com.equipo2.vinilos.support.openTab
import com.equipo2.vinilos.support.scrollToWithBarsHidden
import com.equipo2.vinilos.support.waitUntil
import com.equipo2.vinilos.ui.artists.albums.AlbumCandidateAdapter
import com.equipo2.vinilos.ui.artists.albums.AlbumRow
import com.equipo2.vinilos.ui.artists.detail.ArtistAlbumAdapter
import kotlinx.coroutines.CompletableDeferred
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import java.util.Collections

/**
 * Repositorio de álbumes de artistas en memoria. Da sus respuestas en orden y repite la última,
 * y puede dejar el envío esperando para ver la pantalla mientras se agrega.
 */
class InMemoryArtistAlbumRepository(candidates: List<AlbumCandidate>) : ArtistAlbumRepository {

    var candidateResponses: List<Result<List<AlbumCandidate>>> = listOf(Result.success(candidates))

    var addResponses: List<Result<Unit>> = listOf(Result.success(Unit))

    /** Si no es null, agregar no responde hasta que la prueba lo complete. */
    var addGate: CompletableDeferred<Unit>? = null

    /** Artistas cuyo listado se pidió, con su tipo. La app los pide en el hilo principal y la prueba los lee en el suyo. */
    val candidateRequests: MutableList<Pair<Int, ArtistType>> = Collections.synchronizedList(mutableListOf())

    /** Un envío: a quién, con su tipo, y qué álbum. */
    data class Added(val artistId: Int, val type: ArtistType, val albumId: Int)

    val added: MutableList<Added> = Collections.synchronizedList(mutableListOf())

    override suspend fun getAlbumCandidates(artistId: Int, type: ArtistType): Result<List<AlbumCandidate>> {
        candidateRequests += artistId to type
        return candidateResponses[minOf(candidateRequests.size - 1, candidateResponses.lastIndex)]
    }

    override suspend fun addAlbum(artistId: Int, type: ArtistType, albumId: Int): Result<Unit> {
        added += Added(artistId, type, albumId)
        addGate?.await()
        return addResponses[minOf(added.size - 1, addResponses.lastIndex)]
    }
}

/** Entra con el rol, abre la pestaña Artistas y el detalle del músico o de la banda. */
fun openArtistForAlbums(name: String, collector: Boolean = true) {
    enterAs(collector)
    openTab(R.id.artistsGraph)
    openArtist(name)
}

/** Toca Agregar álbum en la sección Álbumes del detalle. */
fun openAlbumAdd() {
    onView(withId(R.id.addAlbumButton)).perform(scrollToWithBarsHidden(), click())
}

/**
 * Escribe en el buscador y espera a que la lista muestre solo lo que coincide por el nombre del álbum o de un intérprete.
 * Sin tildes en [text], que la app ignora pero esta comparación no.
 */
fun searchAlbum(text: String) {
    onView(withId(R.id.albumSearchInput)).perform(replaceText(text), closeSoftKeyboard())
    // La lista calcula los cambios en otro hilo, que Espresso no espera
    waitForAlbumCandidates { rows ->
        rows.all { row ->
            val album = row.candidate.album
            album.name.contains(text, ignoreCase = true) || album.performers.any { it.name.contains(text, ignoreCase = true) }
        }
    }
}

/** Nombre de un álbum en el listado de Agregar álbum. */
fun candidateAlbumName(name: String): Matcher<View> =
    allOf(withId(R.id.albumCandidateName), withText(name), isDescendantOfA(withId(R.id.albumAddList)))

/** Fila completa del álbum, que es la que se toca y la que queda deshabilitada. */
fun candidateAlbumRow(name: String): Matcher<View> =
    allOf(withParent(withId(R.id.albumAddList)), hasDescendant(candidateAlbumName(name)))

/** «1978 · Salsa · Rubén Blades» debajo del nombre. */
fun candidateAlbumDetails(name: String): Matcher<View> = allOf(withId(R.id.albumCandidateDetails), hasSibling(candidateAlbumName(name)))

/** «Ya está en sus álbumes» de un álbum que el artista ya tiene. */
fun candidateAlbumAlreadyAdded(name: String): Matcher<View> =
    allOf(withId(R.id.albumCandidateAlreadyAdded), hasSibling(candidateAlbumName(name)))

/** «Se reemplazará a …» del álbum elegido cuando el artista es una banda. */
fun candidateAlbumReplaces(name: String): Matcher<View> = allOf(withId(R.id.albumCandidateReplaces), hasSibling(candidateAlbumName(name)))

fun candidateAlbumRadio(name: String): Matcher<View> =
    allOf(withId(R.id.albumCandidateRadio), hasSibling(hasDescendant(candidateAlbumName(name))))

/** Elige el álbum y espera a que su fila se vea elegida. */
fun chooseAlbum(name: String) {
    tapAlbum(name)
    waitForAlbumCandidates { rows -> rows.any { it.selected && it.candidate.album.name == name } }
}

/** Toca la fila del álbum, desplazando antes la lista para que se vea completa en pantallas pequeñas. */
fun tapAlbum(name: String) {
    onView(withId(R.id.albumAddList)).perform(scrollToAlbumCandidate(name))
    onView(candidateAlbumRow(name)).perform(click())
}

fun tapAddAlbum() {
    onView(withId(R.id.albumAddButton)).perform(click())
}

/** Nombres del listado en el orden en que se muestran, incluidos los que no caben en la pantalla. */
fun candidateAlbumNamesInOrder(): List<String> = albumCandidateListState().first.map { it.candidate.album.name }

/**
 * Espera a que la lista termine de actualizarse: calcula los cambios en otro hilo y anima las filas
 * que salen en los siguientes cuadros, y Espresso no espera ninguna de las dos cosas.
 */
fun waitForAlbumCandidates(condition: (List<AlbumRow>) -> Boolean) = waitUntil {
    val (rows, animating) = albumCandidateListState()
    condition(rows) && !animating
}

/** Filas de la lista y si todavía anima filas que entran o salen. */
private fun albumCandidateListState(): Pair<List<AlbumRow>, Boolean> {
    var state = emptyList<AlbumRow>() to false
    onView(withId(R.id.albumAddList)).check { view, noView ->
        val list = (view ?: throw noView) as RecyclerView
        state = (list.adapter as AlbumCandidateAdapter).currentList to list.isAnimating
    }
    return state
}

private fun scrollToAlbumCandidate(name: String) = object : ViewAction {
    override fun getConstraints(): Matcher<View> = isAssignableFrom(RecyclerView::class.java)

    override fun getDescription() = "desplazar la lista de álbumes hasta «$name»"

    override fun perform(uiController: UiController, view: View) {
        val list = view as RecyclerView
        val position = (list.adapter as AlbumCandidateAdapter).currentList.indexOfFirst { it.candidate.album.name == name }
        check(position >= 0) { "La lista no tiene el álbum «$name»" }
        // Arriba de la lista se ve completa; con scrollToPosition puede quedar a medias en una pantalla pequeña
        (list.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, 0)
        uiController.loopMainThreadUntilIdle()
    }
}

// Detalle del artista

/** Nombre de un álbum en el carrusel del detalle del artista. */
fun carouselAlbumName(name: String): Matcher<View> =
    allOf(withId(R.id.artistAlbumName), withText(name), isDescendantOfA(withId(R.id.albumsList)))

/** «Nuevo» sobre la portada del álbum en el carrusel. */
fun newAlbumBadge(name: String): Matcher<View> =
    allOf(withId(R.id.artistAlbumNewBadge), withParent(hasSibling(carouselAlbumName(name))))

/**
 * Nombres del carrusel en el orden en que se muestran, incluidos los que no caben en la pantalla.
 * Espera a que la lista aplique los cambios, que calcula en otro hilo.
 */
fun carouselAlbumNamesInOrder(): List<String> {
    var names = emptyList<String>()
    waitUntil {
        var settled = false
        onView(withId(R.id.albumsList)).check { view, noView ->
            val list = (view ?: throw noView) as RecyclerView
            names = (list.adapter as ArtistAlbumAdapter).currentList.map { it.album.name }
            settled = !list.isAnimating && !list.hasPendingAdapterUpdates()
        }
        settled
    }
    return names
}
