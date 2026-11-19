package com.equipo2.vinilos.hu15

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.equipo2.vinilos.R
import com.equipo2.vinilos.hu04.checkShowingArtist
import com.equipo2.vinilos.hu04.countText
import com.equipo2.vinilos.hu04.pressToolbarUp
import com.equipo2.vinilos.hu12.string
import com.equipo2.vinilos.support.BackVynils
import com.equipo2.vinilos.support.OwnData
import com.equipo2.vinilos.support.RUN_TAG
import com.equipo2.vinilos.support.VinilosRule
import com.equipo2.vinilos.support.checkSnackbar
import com.equipo2.vinilos.support.rotateScreen
import com.equipo2.vinilos.support.scrollToWithBarsHidden
import com.equipo2.vinilos.support.toolbarTitle
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * E2E de HU15 con la API: botón según el rol, listado con los álbumes que ya tiene al final, buscador por álbum
 * o intérprete con su conteo, «Elige un álbum», reemplazo de intérpretes con confirmación en una banda, y el álbum
 * nuevo primero y marcado en el detalle y guardado en la API, para un músico y para una banda.
 *
 * Cada prueba crea sus artistas y sus álbumes, los busca por la marca de esta ejecución y solo revisa esos datos.
 */
@RunWith(AndroidJUnit4::class)
class HU15AddAlbumTest {

    @get:Rule
    val app = VinilosRule()

    private val own = OwnData()

    private val musicianName = own.name("HU15 Músico")
    private val otherPerformerName = own.name("HU15 Intérprete")
    private val bandName = own.name("HU15 Banda")
    private val freeAlbumName = own.name("HU15 Libre")
    private val ownedAlbumName = own.name("HU15 Propio")
    private val otherAlbumName = own.name("HU15 Ajeno")

    private var musicianId = 0
    private var freeAlbumId = 0
    private var ownedAlbumId = 0
    private var otherAlbumId = 0

    @Before
    fun createTestData() {
        musicianId = own.musician(musicianName)
        val otherPerformerId = own.musician(otherPerformerName)
        own.band(bandName)
        freeAlbumId = own.album(freeAlbumName)
        ownedAlbumId = own.album(ownedAlbumName)
        otherAlbumId = own.album(otherAlbumName)
        BackVynils.addAlbumToMusician(musicianId, ownedAlbumId)
        BackVynils.addAlbumToMusician(otherPerformerId, otherAlbumId)
    }

    @After
    fun deleteTestData() = own.deleteAll()

    // Botón según el rol

    @Test
    fun visitante_noVeAgregarAlbum() {
        app.launch()
        openArtistForAlbums(musicianName, collector = false)

        onView(carouselAlbumName(ownedAlbumName)).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
        onView(withId(R.id.addAlbumButton)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun coleccionista_veAgregarAlbum_yElVacioLeDiceComoAgregarElPrimero() {
        val withoutAlbums = own.name("HU15 Sin álbumes")
        own.musician(withoutAlbums)
        app.launch()
        openArtistForAlbums(withoutAlbums)

        onView(withId(R.id.addAlbumButton)).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
        onView(withText(R.string.artist_detail_albums_empty_body_collector)).perform(scrollToWithBarsHidden())
            .check(matches(isDisplayed()))
    }

    // Listado

    @Test
    fun elListado_muestraLosDatosDeCadaAlbum_yLosQueYaTieneAlFinalDeshabilitados() {
        openAlbumAddScreen(musicianName)
        searchAlbum(RUN_TAG)

        onView(toolbarTitle(R.string.album_add_title)).check(matches(isDisplayed()))
        onView(withId(R.id.albumAddSubtitle)).check(matches(withText(string(R.string.album_add_subtitle, musicianName))))
        onView(candidateAlbumDetails(otherAlbumName)).check(matches(withText(details(otherPerformerName))))
        onView(candidateAlbumDetails(freeAlbumName)).check(matches(withText(details(string(R.string.album_list_no_performers)))))

        // El que ya tiene va al final, deshabilitado y con su aviso
        assertEquals(ownedAlbumName, candidateAlbumNamesInOrder().last())
        tapAlbum(ownedAlbumName)
        onView(candidateAlbumRow(ownedAlbumName)).check(matches(not(isEnabled())))
        onView(candidateAlbumAlreadyAdded(ownedAlbumName)).check(matches(isDisplayed()))
        onView(candidateAlbumRadio(ownedAlbumName)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun aUnMusico_noSeMuestraElAvisoDeReemplazo() {
        openAlbumAddScreen(musicianName)

        onView(withId(R.id.albumAddBandNotice)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    // Buscador

    @Test
    fun elBuscador_filtraPorElNombreDelAlbum_yMuestraCuantosCoinciden() {
        openAlbumAddScreen(musicianName)

        searchAlbum(freeAlbumName.lowercase())

        onView(candidateAlbumName(freeAlbumName)).check(matches(isDisplayed()))
        onView(candidateAlbumName(otherAlbumName)).check(doesNotExist())
        onView(withId(R.id.albumAddCount)).check(matches(withText(countText(R.plurals.album_add_result_count, 1))))
    }

    @Test
    fun elBuscador_filtraPorElNombreDeUnInterprete() {
        openAlbumAddScreen(musicianName)

        searchAlbum(otherPerformerName)

        onView(candidateAlbumName(otherAlbumName)).check(matches(isDisplayed()))
        onView(candidateAlbumName(freeAlbumName)).check(doesNotExist())
        onView(withId(R.id.albumAddCount)).check(matches(withText(countText(R.plurals.album_add_result_count, 1))))
    }

    @Test
    fun sinCoincidencias_loIndica_conElConteoEnCero() {
        openAlbumAddScreen(musicianName)
        val search = "sin coincidencias $RUN_TAG"

        searchAlbum(search)

        onView(withText(R.string.album_add_no_results_title)).check(matches(isDisplayed()))
        onView(withText(string(R.string.album_add_no_results_body, search))).check(matches(isDisplayed()))
        onView(withId(R.id.albumAddCount)).check(matches(withText(countText(R.plurals.album_add_result_count, 0))))
    }

    // Agregar a un músico

    @Test
    fun agregarSinElegir_muestraEligeUnAlbum_yNoCambiaNada() {
        openAlbumAddScreen(musicianName)

        tapAddAlbum()

        onView(withId(R.id.albumRequiredError)).check(matches(isDisplayed()))
        onView(withId(R.id.albumRequiredError)).check(matches(withText(R.string.error_album_required)))
        assertEquals(emptyList<String>(), BackVynils.albumPerformerNames(freeAlbumId))
    }

    @Test
    fun agregarAUnMusico_vuelveAlDetalle_conElAlbumPrimeroYMarcado_elAviso_yQuedaEnLaApi() {
        openAlbumAddScreen(musicianName)
        searchAlbum(RUN_TAG)

        chooseAlbum(freeAlbumName)
        tapAddAlbum()

        // El aviso dura unos segundos, así que se revisa antes que lo demás
        checkSnackbar(R.string.snackbar_album_added)
        checkShowingArtist(musicianName)
        onView(withId(R.id.albumsCount)).check(matches(withText(countText(R.plurals.artist_detail_albums_count, 2))))
        assertEquals(listOf(freeAlbumName, ownedAlbumName), carouselAlbumNamesInOrder())
        onView(newAlbumBadge(freeAlbumName)).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
        onView(newAlbumBadge(ownedAlbumName)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        assertEquals(listOf(musicianName), BackVynils.albumPerformerNames(freeAlbumId))
    }

    @Test
    fun aUnMusico_seSumaALosInterpretesDelAlbum_sinPreguntar() {
        openAlbumAddScreen(musicianName)
        searchAlbum(RUN_TAG)

        chooseAlbum(otherAlbumName)
        onView(candidateAlbumReplaces(otherAlbumName)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        tapAddAlbum()

        checkSnackbar(R.string.snackbar_album_added)
        assertEquals(listOf(otherPerformerName, musicianName).sorted(), BackVynils.albumPerformerNames(otherAlbumId))
    }

    // Agregar a una banda

    @Test
    fun aUnaBanda_avisaDelReemplazo_yAlConfirmarLaBandaQuedaComoUnicoInterprete() {
        openAlbumAddScreen(bandName)
        onView(withId(R.id.albumAddBandNotice)).check(matches(isDisplayed()))
        searchAlbum(RUN_TAG)

        chooseAlbum(otherAlbumName)
        onView(candidateAlbumReplaces(otherAlbumName))
            .check(matches(withText(string(R.string.album_candidate_replaces, otherPerformerName))))
        tapAddAlbum()

        onView(withText(R.string.album_replace_title)).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText(R.string.album_replace_confirm)).inRoot(isDialog()).perform(click())

        checkSnackbar(R.string.snackbar_album_added)
        checkShowingArtist(bandName)
        onView(newAlbumBadge(otherAlbumName)).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
        // Lo que dice el aviso: la banda reemplaza a los intérpretes que tenía el álbum
        assertEquals(listOf(bandName), BackVynils.albumPerformerNames(otherAlbumId))
    }

    @Test
    fun aUnaBanda_unAlbumSinInterpretes_seAgregaSinPreguntar() {
        openAlbumAddScreen(bandName)
        searchAlbum(RUN_TAG)

        chooseAlbum(freeAlbumName)
        onView(candidateAlbumReplaces(freeAlbumName)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        tapAddAlbum()

        checkSnackbar(R.string.snackbar_album_added)
        assertEquals(listOf(bandName), BackVynils.albumPerformerNames(freeAlbumId))
    }

    @Test
    fun cancelarElReemplazo_noAgregaNada_yConservaLaSeleccion() {
        openAlbumAddScreen(bandName)
        searchAlbum(RUN_TAG)

        chooseAlbum(otherAlbumName)
        tapAddAlbum()
        onView(withText(R.string.action_cancel)).inRoot(isDialog()).perform(click())

        onView(withId(R.id.albumAddSubtitle)).check(matches(isDisplayed()))
        onView(candidateAlbumRadio(otherAlbumName)).check(matches(isChecked()))
        assertEquals(listOf(otherPerformerName), BackVynils.albumPerformerNames(otherAlbumId))
    }

    // Cancelar y girar

    @Test
    fun cancelar_vuelveAlDetalle_sinAgregar() {
        openAlbumAddScreen(musicianName)
        searchAlbum(RUN_TAG)

        chooseAlbum(freeAlbumName)
        onView(withId(R.id.albumAddCancelButton)).perform(click())

        checkShowingArtist(musicianName)
        onView(withId(R.id.albumsCount)).check(matches(withText(countText(R.plurals.artist_detail_albums_count, 1))))
        assertEquals(emptyList<String>(), BackVynils.albumPerformerNames(freeAlbumId))
    }

    @Test
    fun laXDeArriba_vuelveAlDetalle_sinAgregar() {
        openAlbumAddScreen(musicianName)
        searchAlbum(RUN_TAG)

        chooseAlbum(freeAlbumName)
        pressToolbarUp()

        checkShowingArtist(musicianName)
        assertEquals(emptyList<String>(), BackVynils.albumPerformerNames(freeAlbumId))
    }

    @Test
    fun alGirarLaPantalla_seConservanLaBusquedaYLaSeleccion_yAgregaElMismoAlbum() {
        openAlbumAddScreen(musicianName)
        searchAlbum(RUN_TAG)
        chooseAlbum(freeAlbumName)

        rotateScreen()

        onView(withId(R.id.albumSearchInput)).check(matches(withText(RUN_TAG)))
        onView(candidateAlbumRadio(freeAlbumName)).check(matches(isChecked()))
        tapAddAlbum()

        checkSnackbar(R.string.snackbar_album_added)
        assertEquals(listOf(musicianName), BackVynils.albumPerformerNames(freeAlbumId))
    }

    /** Entra como coleccionista, abre el artista de prueba y toca Agregar álbum. */
    private fun openAlbumAddScreen(artistName: String) {
        app.launch()
        openArtistForAlbums(artistName)
        openAlbumAdd()
    }

    /** «2020 · Rock · intérpretes», con el año y el género con que BackVynils.createAlbum crea los álbumes. */
    private fun details(performers: String) =
        string(R.string.album_candidate_details, string(R.string.album_list_subtitle, 2020, "Rock"), performers)
}
