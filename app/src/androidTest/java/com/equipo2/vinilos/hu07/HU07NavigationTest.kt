package com.equipo2.vinilos.hu07

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isSelected
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.equipo2.vinilos.R
import com.equipo2.vinilos.hu01.openAlbum
import com.equipo2.vinilos.hu01.shownAlbums
import com.equipo2.vinilos.hu03.openArtists
import com.equipo2.vinilos.hu03.openFirstArtist
import com.equipo2.vinilos.support.VinilosRule
import com.equipo2.vinilos.support.enterAs
import com.equipo2.vinilos.support.toolbarTitle
import com.equipo2.vinilos.support.waitUntil
import org.hamcrest.Matcher
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Barra de navegación: tocar otra vez la pestaña y cambiar de pestaña desde un detalle, contra la API publicada. */
@RunWith(AndroidJUnit4::class)
class HU07NavigationTest {

    @get:Rule
    val app = VinilosRule()

    @Test
    fun tocarOtraVezAlbumes_subeAlInicioDelCatalogo() {
        app.launch()
        enterAs(collector = false)
        onView(withId(R.id.albumGrid)).perform(scrollToEnd())

        onView(withId(R.id.albumsGraph)).perform(click())

        var grid: RecyclerView? = null
        onView(withId(R.id.albumGrid)).check { view, _ -> grid = view as RecyclerView }
        waitUntil { grid?.canScrollVertically(-1) == false }
        onView(withId(R.id.albumCount)).check(matches(isDisplayed()))
    }

    @Test
    fun tocarOtraVezAlbumesDesdeElDetalle_vuelveAlCatalogo() {
        app.launch()
        enterAs(collector = false)
        openAlbum(shownAlbums().first().album.name)

        onView(withId(R.id.albumsGraph)).perform(click())

        onView(toolbarTitle(R.string.album_list_title)).check(matches(isDisplayed()))
    }

    @Test
    fun irAArtistasDesdeElDetalleDeUnAlbum_yVolver_marcaAlbumesYSigueEnElDetalle() {
        app.launch()
        enterAs(collector = false)
        openAlbum(shownAlbums().first().album.name)

        onView(withId(R.id.artistsGraph)).perform(click())
        onView(withId(R.id.albumsGraph)).perform(click())

        onView(toolbarTitle(R.string.album_detail_title)).check(matches(isDisplayed()))
        onView(withId(R.id.albumsGraph)).check(matches(isSelected()))
        onView(withId(R.id.artistsGraph)).check(matches(not(isSelected())))
    }

    @Test
    fun irAAlbumesDesdeElDetalleDeUnArtista_yVolver_marcaArtistasYSigueEnElDetalle() {
        app.launch()
        openArtists()
        openFirstArtist()

        onView(withId(R.id.albumsGraph)).perform(click())
        onView(withId(R.id.artistsGraph)).perform(click())

        onView(toolbarTitle(R.string.artist_detail_title)).check(matches(isDisplayed()))
        onView(withId(R.id.artistsGraph)).check(matches(isSelected()))
        onView(withId(R.id.albumsGraph)).check(matches(not(isSelected())))
    }
}

/** Baja hasta la última fila, como si el usuario hubiera recorrido toda la lista. */
private fun scrollToEnd() = object : ViewAction {
    override fun getConstraints(): Matcher<View> = isAssignableFrom(RecyclerView::class.java)

    override fun getDescription() = "bajar hasta el final de la lista"

    override fun perform(uiController: UiController, view: View) {
        val list = view as RecyclerView
        list.scrollToPosition(list.adapter!!.itemCount - 1)
        uiController.loopMainThreadUntilIdle()
    }
}
