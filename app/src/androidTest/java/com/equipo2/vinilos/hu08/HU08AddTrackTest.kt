package com.equipo2.vinilos.hu08

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.equipo2.vinilos.R
import com.equipo2.vinilos.VinilosApplication
import com.equipo2.vinilos.hu01.openAlbum
import com.equipo2.vinilos.support.BackVynils
import com.equipo2.vinilos.support.VinilosRule
import com.equipo2.vinilos.support.enterAs
import com.equipo2.vinilos.support.hasFieldError
import com.equipo2.vinilos.support.rotateScreen
import com.equipo2.vinilos.support.scrollToWithBarsHidden
import com.equipo2.vinilos.support.useAlbumRepository
import com.equipo2.vinilos.support.waitUntil
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** E2E de HU08: botón Agregar track, hoja con validaciones y registro en la API. */
@RunWith(AndroidJUnit4::class)
class HU08AddTrackTest {

    @get:Rule
    val app = VinilosRule()

    @After
    fun deleteTestAlbums() {
        BackVynils.deleteAlbumsStartingWith(TEST_ALBUM_PREFIX)
    }

    @Test
    fun visitante_noVeAgregarTrack() {
        openNewAlbum(collector = false)

        onView(withId(R.id.addTrackButton)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withText(R.string.album_detail_tracks_empty_body)).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
    }

    @Test
    fun coleccionista_veAgregarTrack_yElVacioLeDiceComoAgregarElPrimero() {
        openNewAlbum(collector = true)

        onView(withId(R.id.addTrackButton)).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
        onView(withText(R.string.album_detail_tracks_empty_body_collector)).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
    }

    @Test
    fun cancelar_cierraLaHoja_sinAgregarElTrack() {
        val albumId = openNewAlbum(collector = true)
        openAddTrackSheet()

        onView(withId(R.id.trackNameInput)).perform(replaceText(TRACK_NAME), closeSoftKeyboard())
        onView(withId(R.id.trackCancelButton)).perform(click())

        onView(withId(R.id.trackAddSubtitle)).check(doesNotExist())
        onView(withText(R.string.album_detail_tracks_empty_title)).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
        assertTrue(BackVynils.trackNames(albumId).isEmpty())
    }

    @Test
    fun agregarVacio_muestraElErrorDeCadaCampo() {
        openNewAlbum(collector = true)
        openAddTrackSheet()

        onView(withId(R.id.trackAddButton)).perform(click())

        onView(withId(R.id.trackNameField)).check(matches(hasFieldError(R.string.error_track_name_required)))
        onView(withId(R.id.trackDurationField)).check(matches(hasFieldError(R.string.error_track_duration_required)))
    }

    @Test
    fun duracionSinDosPuntos_muestraElErrorDeFormato() {
        openNewAlbum(collector = true)
        openAddTrackSheet()

        fillTrack(TRACK_NAME, "456")
        onView(withId(R.id.trackAddButton)).perform(click())

        onView(withId(R.id.trackNameField)).check(matches(hasFieldError(null)))
        onView(withId(R.id.trackDurationField)).check(matches(hasFieldError(R.string.error_track_duration_invalid)))
    }

    @Test
    fun agregarUnTrack_apareceAlFinalComoNuevo_avisa_yQuedaEnLaApi() {
        val albumId = openNewAlbum(collector = true)
        openAddTrackSheet()

        fillTrack(TRACK_NAME, "4:56")
        onView(withId(R.id.trackAddButton)).perform(click())

        onView(withId(R.id.trackAddSubtitle)).check(doesNotExist())
        onView(withText(R.string.snackbar_track_added)).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.trackNewBadge), hasSibling(withText(TRACK_NAME))))
            .perform(scrollToWithBarsHidden())
            .check(matches(isDisplayed()))
        assertEquals(listOf(TRACK_NAME), BackVynils.trackNames(albumId))
    }

    @Test
    fun agregarDespuesDeGirarLaPantalla_igualApareceElTrackNuevo() {
        val albumId = openNewAlbum(collector = true)
        openAddTrackSheet()
        fillTrack(TRACK_NAME, "4:56")

        rotateScreen()
        onView(withId(R.id.trackDurationInput)).perform(closeSoftKeyboard())
        waitForSheetToSettle()
        onView(withId(R.id.trackAddButton)).perform(click())

        onView(withText(R.string.snackbar_track_added)).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.trackNewBadge), hasSibling(withText(TRACK_NAME))))
            .perform(scrollToWithBarsHidden())
            .check(matches(isDisplayed()))
        assertEquals(listOf(TRACK_NAME), BackVynils.trackNames(albumId))
    }

    @Test
    fun siNoSePuedeAgregar_laHojaSigueAbiertaConLoEscrito_yAvisa() {
        useAlbumRepository(FailingTrackRepository(VinilosApplication.container.albumRepository))
        openNewAlbum(collector = true)
        openAddTrackSheet()

        fillTrack(TRACK_NAME, "4:56")
        onView(withId(R.id.trackAddButton)).perform(click())

        onView(withText(R.string.error_track_add_failed)).check(matches(isDisplayed()))
        onView(withId(R.id.trackNameInput)).check(matches(withText(TRACK_NAME)))
        onView(withId(R.id.trackAddButton)).check(matches(withText(R.string.action_add)))
    }

    /** Crea un álbum vacío en la API, entra con el rol y lo abre. Devuelve su id. */
    private fun openNewAlbum(collector: Boolean): Int {
        val name = "$TEST_ALBUM_PREFIX ${System.currentTimeMillis()}"
        val albumId = BackVynils.createAlbum(name)
        app.launch()
        enterAs(collector)
        openAlbum(name)
        return albumId
    }

    /** Abre la hoja y espera a que termine de subir. */
    private fun openAddTrackSheet() {
        onView(withId(R.id.addTrackButton)).perform(scrollToWithBarsHidden(), click())
        waitForSheetToSettle()
    }

    /** Un toque mientras la hoja todavía sube cae fuera de ella. */
    private fun waitForSheetToSettle() {
        var lastTop = -1
        waitUntil {
            val top = addButtonTop()
            (top == lastTop).also { lastTop = top }
        }
    }

    private fun addButtonTop(): Int {
        val location = IntArray(2)
        onView(withId(R.id.trackAddButton)).check { view, noView -> (view ?: throw noView).getLocationOnScreen(location) }
        return location[1]
    }

    private fun fillTrack(name: String, duration: String) {
        onView(withId(R.id.trackNameInput)).perform(replaceText(name), closeSoftKeyboard())
        onView(withId(R.id.trackDurationInput)).perform(replaceText(duration), closeSoftKeyboard())
    }

    private companion object {
        /** Prefijo de los álbumes de prueba, que se borran al terminar junto con sus tracks. */
        const val TEST_ALBUM_PREFIX = "E2E HU08"
        const val TRACK_NAME = "Todos vuelven"
    }
}
