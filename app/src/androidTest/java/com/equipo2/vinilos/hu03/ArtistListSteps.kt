package com.equipo2.vinilos.hu03

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.Artist
import com.equipo2.vinilos.data.model.ArtistDetail
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.data.repository.ArtistRepository
import com.equipo2.vinilos.support.enterAs
import com.equipo2.vinilos.ui.artists.list.ArtistAdapter
import kotlinx.coroutines.awaitCancellation
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import java.io.IOException
import java.time.LocalDate

/** Repositorio en memoria que da sus respuestas en orden y repite la última, o que nunca responde. */
class InMemoryArtistRepository(private val responses: List<Result<List<Artist>>>) : ArtistRepository {

    constructor(response: Result<List<Artist>> = Result.success(emptyList())) : this(listOf(response))

    var neverAnswer = false

    private var calls = 0

    override suspend fun getArtists(): Result<List<Artist>> {
        if (neverAnswer) awaitCancellation()
        return responses[minOf(calls++, responses.lastIndex)]
    }

    // Aquí solo se prueba que el detalle se abra; su contenido se prueba en HU04 con otro repositorio
    override suspend fun getArtistDetail(id: Int, type: ArtistType): Result<ArtistDetail> =
        Result.failure(UnsupportedOperationException("Estas pruebas no revisan el contenido del detalle"))
}

fun noConnection(): Result<List<Artist>> = Result.failure(IOException("Sin conexión"))

fun testMusician(id: Int, name: String, birthYear: Int) =
    Artist(id, name, image = "", type = ArtistType.MUSICIAN, date = LocalDate.of(birthYear, 1, 1))

fun testBand(id: Int, name: String, creationYear: Int) =
    Artist(id, name, image = "", type = ArtistType.BAND, date = LocalDate.of(creationYear, 1, 1))

/** Entra como visitante y abre la pestaña Artistas de la barra inferior. */
fun openArtists() {
    enterAs(collector = false)
    onView(withId(R.id.artistsGraph)).perform(click())
}

/** Nombre de una fila del listado, sin confundirlo con otros textos de la pantalla. */
fun artistRowName(name: String): Matcher<View> = allOf(withId(R.id.artistName), withText(name), isDescendantOfA(withId(R.id.artistList)))

/** Artistas que muestra el listado con el filtro actual. */
fun shownArtists(): List<Artist> {
    var items = emptyList<Artist>()
    onView(withId(R.id.artistList)).check { view, noView ->
        items = ((view ?: throw noView) as RecyclerView).artistAdapter.currentList
    }
    return items
}

/** Desplaza el listado hasta el artista, que debe estar a la vista para revisarlo o tocarlo. */
fun showArtistRow(name: String) {
    onView(withId(R.id.artistList)).perform(object : ViewAction {
        override fun getConstraints(): Matcher<View> = isAssignableFrom(RecyclerView::class.java)

        override fun getDescription() = "desplazar el listado hasta «$name»"

        override fun perform(uiController: UiController, view: View) {
            val list = view as RecyclerView
            val position = list.artistAdapter.currentList.indexOfFirst { it.name == name }
            check(position >= 0) { "El listado no tiene el artista «$name»" }
            list.scrollToPosition(position)
            uiController.loopMainThreadUntilIdle()
        }
    })
    onView(artistRowName(name)).check(matches(isDisplayed()))
}

/** Toca la primera fila del listado, sea cual sea el artista. */
fun openFirstArtist() {
    onView(withId(R.id.artistList)).perform(object : ViewAction {
        override fun getConstraints(): Matcher<View> = isAssignableFrom(RecyclerView::class.java)

        override fun getDescription() = "tocar la primera fila del listado"

        override fun perform(uiController: UiController, view: View) {
            val list = view as RecyclerView
            list.scrollToPosition(0)
            uiController.loopMainThreadUntilIdle()
            list.findViewHolderForAdapterPosition(0)!!.itemView.performClick()
            uiController.loopMainThreadUntilIdle()
        }
    })
}

private val RecyclerView.artistAdapter: ArtistAdapter
    get() = adapter as ArtistAdapter

