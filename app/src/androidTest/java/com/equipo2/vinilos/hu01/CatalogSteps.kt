package com.equipo2.vinilos.hu01

import android.view.View
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.Album
import com.equipo2.vinilos.support.RUN_TAG
import com.equipo2.vinilos.support.isSettled
import com.equipo2.vinilos.support.waitUntil
import com.equipo2.vinilos.ui.albums.list.AlbumAdapter
import com.equipo2.vinilos.ui.albums.list.AlbumItem
import com.google.android.material.card.MaterialCardView
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import java.time.LocalDate

/** Prefijo de los álbumes que crean las pruebas de HU01, que se borran al terminar. */
val HU01_ALBUM_PREFIX = "E2E HU01 $RUN_TAG"

fun testAlbum(id: Int, name: String, year: Int, genre: String) = Album(
    id = id,
    name = name,
    // Sin portada se ve el marcador y la prueba no depende de internet
    cover = "",
    releaseDate = LocalDate.of(year, 1, 1),
    description = "Álbum de prueba de HU01.",
    genre = genre,
    recordLabel = "Elektra",
)

/** Tarjeta del catálogo que muestra este nombre. */
fun albumCard(name: String): Matcher<View> =
    allOf(isAssignableFrom(MaterialCardView::class.java), hasDescendant(withText(name)))

/** Nombre del álbum dentro de su tarjeta, sin confundirlo con el título del detalle. */
fun albumCardName(name: String): Matcher<View> = allOf(withText(name), isDescendantOfA(albumCard(name)))

/** Desplaza la cuadrícula hasta el álbum y lo toca, cuando el catálogo ya lo tiene y terminó de acomodarse. */
fun openAlbum(name: String) {
    shownAlbums { albums -> albums.any { it.album.name == name } }
    onView(withId(R.id.albumGrid)).perform(scrollToAlbum(name))
    onView(albumCardName(name)).perform(click())
}

/**
 * Álbumes que muestra el catálogo, cuando ya terminó de recibirlos y acomodarlos y cumplen la condición.
 * El catálogo calcula los cambios en otro hilo, que Espresso no espera.
 */
fun shownAlbums(until: (List<AlbumItem>) -> Boolean = { it.isNotEmpty() }): List<AlbumItem> {
    var items = emptyList<AlbumItem>()
    waitUntil {
        onView(withId(R.id.albumGrid)).check { view, noView ->
            val grid = (view ?: throw noView) as RecyclerView
            items = if (grid.isSettled()) grid.albumAdapter.currentList else emptyList()
        }
        until(items)
    }
    return items
}

private fun scrollToAlbum(name: String) = object : ViewAction {
    override fun getConstraints(): Matcher<View> = isAssignableFrom(RecyclerView::class.java)

    override fun getDescription() = "desplazar el catálogo hasta «$name»"

    override fun perform(uiController: UiController, view: View) {
        val grid = view as RecyclerView
        while (!grid.isSettled()) uiController.loopMainThreadForAtLeast(50)
        val position = grid.albumAdapter.currentList.indexOfFirst { it.album.name == name }
        check(position >= 0) { "El catálogo no tiene el álbum «$name»" }
        // Antes de las tarjetas va la fila del conteo
        val headerRows = grid.adapter!!.itemCount - grid.albumAdapter.itemCount
        grid.scrollToPosition(position + headerRows)
        uiController.loopMainThreadUntilIdle()
    }
}

private val RecyclerView.albumAdapter: AlbumAdapter
    get() = (adapter as ConcatAdapter).adapters.filterIsInstance<AlbumAdapter>().single()
