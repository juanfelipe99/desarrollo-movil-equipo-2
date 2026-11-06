package com.equipo2.vinilos.hu04

import android.view.View
import android.widget.ImageButton
import androidx.appcompat.widget.Toolbar
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.Album
import com.equipo2.vinilos.data.model.Artist
import com.equipo2.vinilos.data.model.ArtistDetail
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.data.repository.ArtistRepository
import com.equipo2.vinilos.hu03.artistRowName
import com.equipo2.vinilos.ui.artists.list.ArtistAdapter
import com.equipo2.vinilos.support.scrollToWithBarsHidden
import kotlinx.coroutines.awaitCancellation
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import java.io.IOException
import java.time.LocalDate

/**
 * Repositorio en memoria con un listado fijo y el detalle de cada artista.
 * Puede fallar las primeras consultas del detalle o no responderlas nunca.
 */
class InMemoryArtistDetailRepository(private val details: List<ArtistDetail>) : ArtistRepository {

    /** Cuántas consultas del detalle fallan sin conexión antes de responder. */
    var failuresBeforeAnswer = 0

    var neverAnswer = false

    /** Artistas pedidos al detalle, con su tipo, en orden. */
    val detailRequests = mutableListOf<Pair<Int, ArtistType>>()

    override suspend fun getArtists(): Result<List<Artist>> = Result.success(details.map { it.artist })

    override suspend fun getArtistDetail(id: Int, type: ArtistType): Result<ArtistDetail> {
        detailRequests += id to type
        if (neverAnswer) awaitCancellation()
        if (detailRequests.size <= failuresBeforeAnswer) return Result.failure(IOException("Sin conexión"))
        return details.firstOrNull { it.artist.id == id && it.artist.type == type }
            ?.let { Result.success(it) }
            ?: Result.failure(IllegalArgumentException("No hay detalle para $type $id"))
    }
}

fun testAlbum(id: Int, name: String, year: Int, genre: String) = Album(
    id = id,
    name = name,
    // Sin portada se ve el marcador y la prueba no depende de internet
    cover = "",
    releaseDate = LocalDate.of(year, 1, 1),
    description = "Álbum de prueba de HU04.",
    genre = genre,
    recordLabel = "Elektra",
)

/** Desplaza el listado de artistas hasta el artista y lo toca. */
fun openArtist(name: String) {
    onView(withId(R.id.artistList)).perform(scrollToArtist(name))
    onView(artistRowName(name)).perform(click())
}

/**
 * El detalle que se ve es el de este artista. Al volver con Atrás el detalle conserva donde se había bajado,
 * así que primero se sube hasta el nombre. Sin esconder las barras, para no cambiar lo que revise la prueba después.
 */
fun checkShowingArtist(name: String) {
    onView(allOf(withId(R.id.artistName), withText(name))).perform(scrollTo()).check(matches(isDisplayed()))
}

/** Fila de un integrante dentro de la sección Integrantes. */
fun memberRow(name: String): Matcher<View> = allOf(withId(R.id.memberName), withText(name), isDescendantOfA(withId(R.id.membersList)))

fun openMember(name: String) {
    onView(memberRow(name)).perform(scrollToWithBarsHidden(), click())
}

/** Toca la banda del músico en la tarjeta de información. */
fun openBandOfMusician() {
    onView(withId(R.id.artistBandRow)).perform(scrollToWithBarsHidden(), click())
}

/** Texto de un contador, por ejemplo «· 2 integrantes». */
fun countText(plural: Int, count: Int): String =
    InstrumentationRegistry.getInstrumentation().targetContext.resources.getQuantityString(plural, count, count)

/** Toca la flecha de la barra superior. */
fun pressToolbarUp() {
    onView(withId(R.id.toolbar)).perform(object : ViewAction {
        override fun getConstraints(): Matcher<View> = isAssignableFrom(Toolbar::class.java)

        override fun getDescription() = "tocar la flecha de la barra superior"

        override fun perform(uiController: UiController, view: View) {
            val toolbar = view as Toolbar
            val up = toolbar.children.filterIsInstance<ImageButton>().first { it.contentDescription == toolbar.navigationContentDescription }
            up.performClick()
            uiController.loopMainThreadUntilIdle()
        }
    })
}

private fun scrollToArtist(name: String) = object : ViewAction {
    override fun getConstraints(): Matcher<View> = isAssignableFrom(RecyclerView::class.java)

    override fun getDescription() = "desplazar el listado hasta «$name»"

    override fun perform(uiController: UiController, view: View) {
        val list = view as RecyclerView
        val position = (list.adapter as ArtistAdapter).currentList.indexOfFirst { it.name == name }
        check(position >= 0) { "El listado no tiene el artista «$name»" }
        list.scrollToPosition(position)
        uiController.loopMainThreadUntilIdle()
    }
}
