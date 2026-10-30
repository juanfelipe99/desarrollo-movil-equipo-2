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
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.equipo2.vinilos.R
import com.equipo2.vinilos.hu01.openAlbum
import com.equipo2.vinilos.hu01.shownAlbums
import com.equipo2.vinilos.support.VinilosRule
import com.equipo2.vinilos.support.enterAs
import com.equipo2.vinilos.support.toolbarTitle
import com.equipo2.vinilos.support.waitUntil
import org.hamcrest.Matcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tocar otra vez la pestaña en la que ya se está, contra la API publicada. */
@RunWith(AndroidJUnit4::class)
class HU07NavigationTest {

    @get:Rule
    val app = VinilosRule()

    @Test
    fun tocarOtraVezAlbumes_subeAlInicioDelCatalogo() {
        app.launch()
        enterAs(collector = false)
        onView(withId(R.id.albumGrid)).perform(scrollToEnd())

        onView(withId(R.id.albumListFragment)).perform(click())

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

        onView(withId(R.id.albumListFragment)).perform(click())

        onView(toolbarTitle(R.string.album_list_title)).check(matches(isDisplayed()))
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
