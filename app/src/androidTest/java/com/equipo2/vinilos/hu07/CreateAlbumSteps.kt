package com.equipo2.vinilos.hu07

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.equipo2.vinilos.R
import com.equipo2.vinilos.support.RUN_TAG
import com.equipo2.vinilos.support.enterAs
import com.equipo2.vinilos.support.pickOption
import com.equipo2.vinilos.support.waitUntil

/** Portada que carga en todas las versiones de Android. */
const val TEST_COVER_URL = "https://www.gstatic.com/webp/gallery/1.jpg"

/** Prefijo de los álbumes de prueba, que se borran al terminar. */
val TEST_ALBUM_PREFIX = "E2E HU07 $RUN_TAG"

/** Entra como coleccionista y abre Crear álbum. */
fun openCreateAlbum() {
    enterAs(collector = true)
    onView(withId(R.id.createAlbumButton)).perform(click())
}

fun fillAlbumForm(name: String) {
    onView(withId(R.id.nameInput)).perform(replaceText(name), closeSoftKeyboard())
    onView(withId(R.id.coverInput)).perform(replaceText(TEST_COVER_URL), closeSoftKeyboard())
    pickTodayAsReleaseDate()
    pickOption(R.id.genreInput, "Rock")
    pickOption(R.id.recordLabelInput, "EMI")
    onView(withId(R.id.descriptionInput)).perform(scrollTo(), replaceText("Álbum creado por la prueba E2E de HU07."), closeSoftKeyboard())
}

fun pickTodayAsReleaseDate() {
    onView(withId(R.id.releaseDateInput)).perform(scrollTo(), click())
    onView(withText(R.string.action_accept)).inRoot(isDialog()).perform(click())
}

/** Toca Guardar y espera a que el formulario deje de desplazarse o se cierre. */
fun tapSave() {
    onView(withId(R.id.saveButton)).perform(click())
    var lastScroll: Int? = -1
    waitUntil {
        val scroll = formScrollY()
        (scroll == null || scroll == lastScroll).also { lastScroll = scroll }
    }
}

private fun formScrollY(): Int? = try {
    var scroll = 0
    onView(withId(R.id.formScroll)).check { view, noView -> scroll = view?.scrollY ?: throw noView }
    scroll
} catch (e: NoMatchingViewException) {
    null
}
