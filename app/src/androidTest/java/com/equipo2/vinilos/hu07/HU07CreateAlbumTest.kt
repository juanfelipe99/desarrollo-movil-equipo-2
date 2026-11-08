package com.equipo2.vinilos.hu07

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.equipo2.vinilos.R
import com.equipo2.vinilos.support.BackVynils
import com.equipo2.vinilos.support.VinilosRule
import com.equipo2.vinilos.support.checkSnackbar
import com.equipo2.vinilos.support.hasFieldError
import com.equipo2.vinilos.support.pickOption
import com.equipo2.vinilos.support.toolbarTitle
import com.equipo2.vinilos.support.useAlbumRepository
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** E2E de HU07: formulario de Crear álbum, validaciones y creación en la API. */
@RunWith(AndroidJUnit4::class)
class HU07CreateAlbumTest {

    @get:Rule
    val app = VinilosRule()

    @After
    fun deleteTestAlbums() {
        BackVynils.deleteAlbumsStartingWith(TEST_ALBUM_PREFIX)
    }

    @Test
    fun alAbrir_noHayErrores() {
        app.launch()
        openCreateAlbum()

        listOf(R.id.nameField, R.id.coverField, R.id.releaseDateField, R.id.genreField, R.id.recordLabelField, R.id.descriptionField)
            .forEach { field -> onView(withId(field)).check(matches(hasFieldError(null))) }
    }

    @Test
    fun guardarVacio_muestraElErrorDeCadaCampo() {
        app.launch()
        openCreateAlbum()

        tapSave()

        onView(withId(R.id.nameField)).check(matches(hasFieldError(R.string.error_album_name_required)))
        onView(withId(R.id.coverField)).check(matches(hasFieldError(R.string.error_cover_required)))
        onView(withId(R.id.releaseDateField)).check(matches(hasFieldError(R.string.error_release_date_required)))
        onView(withId(R.id.genreField)).check(matches(hasFieldError(R.string.error_genre_required)))
        onView(withId(R.id.recordLabelField)).check(matches(hasFieldError(R.string.error_record_label_required)))
        onView(withId(R.id.descriptionField)).check(matches(hasFieldError(R.string.error_description_required)))
        onView(withText(R.string.error_album_name_required)).check(matches(isDisplayed()))
    }

    @Test
    fun despuesDeGuardar_cadaErrorDesapareceAlCorregirSuCampo() {
        app.launch()
        openCreateAlbum()
        tapSave()

        onView(withId(R.id.nameInput)).perform(replaceText("$TEST_ALBUM_PREFIX nombre"), closeSoftKeyboard())
        onView(withId(R.id.nameField)).check(matches(hasFieldError(null)))

        pickOption(R.id.genreInput, "Salsa")
        onView(withId(R.id.genreField)).check(matches(hasFieldError(null)))

        onView(withId(R.id.descriptionField)).check(matches(hasFieldError(R.string.error_description_required)))
    }

    @Test
    fun urlSinHttp_muestraElErrorDeFormato_yUnaValidaMuestraLaVistaPrevia() {
        app.launch()
        openCreateAlbum()

        onView(withId(R.id.coverInput)).perform(replaceText("portada-amor-y-control"), closeSoftKeyboard())
        onView(withId(R.id.coverPreview)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        tapSave()
        onView(withId(R.id.coverField)).check(matches(hasFieldError(R.string.error_cover_invalid)))

        onView(withId(R.id.coverInput)).perform(replaceText(TEST_COVER_URL), closeSoftKeyboard())
        onView(withId(R.id.coverField)).check(matches(hasFieldError(null)))
        onView(withId(R.id.coverPreview)).check(matches(isDisplayed()))
        onView(withId(R.id.coverPreviewUrl)).check(matches(withText(TEST_COVER_URL)))
    }

    @Test
    fun selectorDeFecha_muestraLaFechaElegida_yQuitaSuError() {
        app.launch()
        openCreateAlbum()
        tapSave()

        pickTodayAsReleaseDate()

        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        onView(withId(R.id.releaseDateInput)).check(matches(withText(today)))
        onView(withId(R.id.releaseDateField)).check(matches(hasFieldError(null)))
    }

    @Test
    fun crearAlbumValido_vuelveAAlbumesConElAviso_yQuedaEnLaApi() {
        val name = "$TEST_ALBUM_PREFIX ${System.currentTimeMillis()}"
        app.launch()
        openCreateAlbum()
        fillAlbumForm(name)

        tapSave()

        onView(toolbarTitle(R.string.album_list_title)).check(matches(isDisplayed()))
        checkSnackbar(R.string.snackbar_album_created)
        assertTrue("El álbum $name no está en la API", name in BackVynils.albumNames())
    }

    @Test
    fun siFallaElGuardado_conservaLosDatos_yAvisaQueNoSePudoCrear() {
        useAlbumRepository(FailingAlbumRepository())
        val name = "$TEST_ALBUM_PREFIX sin guardar"
        app.launch()
        openCreateAlbum()
        fillAlbumForm(name)

        tapSave()

        onView(withText(R.string.error_album_create_failed)).check(matches(isDisplayed()))
        onView(toolbarTitle(R.string.album_create_title)).check(matches(isDisplayed()))
        onView(withId(R.id.nameInput)).perform(scrollTo()).check(matches(withText(name)))
        onView(withId(R.id.saveButton)).check(matches(withText(R.string.action_save)))
    }
}
