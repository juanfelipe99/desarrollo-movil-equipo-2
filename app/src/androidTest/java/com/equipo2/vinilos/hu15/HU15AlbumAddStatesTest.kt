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
import com.equipo2.vinilos.data.model.AlbumCandidate
import com.equipo2.vinilos.data.model.ArtistDetail
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.data.model.Performer
import com.equipo2.vinilos.data.repository.DataError
import com.equipo2.vinilos.hu03.testBand
import com.equipo2.vinilos.hu03.testMusician
import com.equipo2.vinilos.hu04.InMemoryArtistDetailRepository
import com.equipo2.vinilos.hu04.checkShowingArtist
import com.equipo2.vinilos.hu04.countText
import com.equipo2.vinilos.hu04.testAlbum
import com.equipo2.vinilos.hu12.string
import com.equipo2.vinilos.hu15.InMemoryArtistAlbumRepository.Added
import com.equipo2.vinilos.support.VinilosRule
import com.equipo2.vinilos.support.checkSnackbar
import com.equipo2.vinilos.support.scrollToWithBarsHidden
import com.equipo2.vinilos.support.useArtistAlbumRepositories
import kotlinx.coroutines.CompletableDeferred
import org.hamcrest.Matchers.not
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI de HU15 con repositorios en memoria: conteo del catálogo, catálogo vacío o ya completo, error al cargar con
 * Reintentar, artista que ya no existe, envío en curso sin envíos duplicados, errores al agregar que conservan la
 * selección, y el álbum nuevo primero y marcado en el carrusel.
 */
@RunWith(AndroidJUnit4::class)
class HU15AlbumAddStatesTest {

    @get:Rule
    val app = VinilosRule()

    private val ruben = testMusician(100, "Rubén Blades", 1948)
    private val queen = testBand(101, "Queen", 1970)
    private val willie = Performer(1, "Willie Colón", "", ArtistType.MUSICIAN)

    private val buscando = testAlbum(100, "Buscando América", 1984, "Salsa")
    private val siembra = testAlbum(178, "Siembra", 1978, "Salsa").copy(performers = listOf(willie))
    private val abbeyRoad = testAlbum(173, "Abbey Road", 1969, "Rock")

    private val rubenDetail = ArtistDetail(artist = ruben, description = "Cantante panameño.", albums = listOf(buscando))
    private val queenDetail = ArtistDetail(artist = queen, description = "Banda británica de rock.")

    private val abbeyRoadCandidate = AlbumCandidate(abbeyRoad)
    private val siembraCandidate = AlbumCandidate(siembra)
    private val buscandoCandidate = AlbumCandidate(buscando, alreadyAdded = true)
    private val all = listOf(abbeyRoadCandidate, siembraCandidate, buscandoCandidate)

    private fun openWith(albums: InMemoryArtistAlbumRepository, artistName: String = ruben.name) {
        useArtistAlbumRepositories(InMemoryArtistDetailRepository(listOf(rubenDetail, queenDetail)), albums)
        app.launch()
        openArtistForAlbums(artistName)
        openAlbumAdd()
    }

    // Carga

    @Test
    fun sinBuscar_muestraCuantosAlbumesHayEnElCatalogo() {
        openWith(InMemoryArtistAlbumRepository(all))

        onView(withId(R.id.albumAddCount)).check(matches(withText(countText(R.plurals.album_list_count, 3))))
        assertEquals(listOf(abbeyRoad.name, siembra.name, buscando.name), candidateAlbumNamesInOrder())
    }

    @Test
    fun sinAlbumesEnElCatalogo_muestraElMensajeInformativo_yCancelarVuelveAlDetalle() {
        openWith(InMemoryArtistAlbumRepository(emptyList()))

        onView(withText(R.string.album_add_empty_title)).check(matches(isDisplayed()))
        onView(withText(R.string.album_add_empty_body)).check(matches(isDisplayed()))
        onView(withId(R.id.albumAddCount)).check(matches(withText(countText(R.plurals.album_list_count, 0))))

        onView(withId(R.id.albumAddCancelButton)).perform(click())
        checkShowingArtist(ruben.name)
    }

    @Test
    fun siYaTieneTodosLosAlbumes_loIndica_sinDecirQueElCatalogoEstaVacio() {
        openWith(InMemoryArtistAlbumRepository(listOf(buscandoCandidate)))

        onView(withText(R.string.album_add_all_added_title)).check(matches(isDisplayed()))
        onView(withText(R.string.album_add_empty_title)).check(doesNotExist())
        onView(withId(R.id.albumAddCount)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun siNoCargaPorConexion_muestraElError_yReintentarCargaElListado() {
        val albums = InMemoryArtistAlbumRepository(all)
        albums.candidateResponses = listOf(Result.failure(DataError.Network()), Result.success(all))
        openWith(albums)

        onView(withText(string(R.string.error_load_title, string(R.string.album_add_object)))).check(matches(isDisplayed()))
        onView(withText(R.string.error_network_body)).check(matches(isDisplayed()))
        onView(withId(R.id.stateRetry)).perform(click())

        onView(candidateAlbumName(abbeyRoad.name)).check(matches(isDisplayed()))
        assertEquals(2, albums.candidateRequests.size)
    }

    @Test
    fun siElArtistaYaNoExiste_loIndica_sinOfrecerReintentar() {
        val albums = InMemoryArtistAlbumRepository(all)
        albums.candidateResponses = listOf(Result.failure(DataError.NotFound()))
        openWith(albums)

        onView(withText(R.string.album_add_artist_missing_title)).check(matches(isDisplayed()))
        onView(withText(R.string.album_add_artist_missing_body)).check(matches(isDisplayed()))
        onView(withId(R.id.stateRetry)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun pideElListadoConElTipoDelArtista() {
        val albums = InMemoryArtistAlbumRepository(all)
        openWith(albums, artistName = queen.name)

        assertEquals(listOf(queen.id to ArtistType.BAND), albums.candidateRequests.toList())
    }

    // Envío

    @Test
    fun mientrasSeAgrega_muestraLaCarga_yDeshabilitaLasAcciones_paraNoEnviarDosVeces() {
        val albums = InMemoryArtistAlbumRepository(all)
        val gate = CompletableDeferred<Unit>()
        albums.addGate = gate
        openWith(albums)

        chooseAlbum(abbeyRoad.name)
        tapAddAlbum()

        onView(withId(R.id.albumAddSubmittingProgress)).check(matches(isDisplayed()))
        onView(withId(R.id.albumAddButton)).check(matches(withText(R.string.album_add_saving)))
        onView(withId(R.id.albumAddButton)).check(matches(not(isEnabled())))
        onView(withId(R.id.albumAddCancelButton)).check(matches(not(isEnabled())))
        onView(withId(R.id.albumSearchField)).check(matches(not(isEnabled())))
        onView(candidateAlbumRow(siembra.name)).check(matches(not(isEnabled())))

        // Tocar Agregar otra vez no hace un segundo envío
        tapAddAlbum()
        assertEquals(1, albums.added.size)

        gate.complete(Unit)
        checkSnackbar(R.string.snackbar_album_added)
        assertEquals(listOf(Added(ruben.id, ArtistType.MUSICIAN, abbeyRoad.id)), albums.added.toList())
    }

    @Test
    fun alAgregar_vuelveAlDetalle_conElAlbumPrimeroYMarcado_yElConteoActualizado() {
        openWith(InMemoryArtistAlbumRepository(all))

        chooseAlbum(abbeyRoad.name)
        tapAddAlbum()

        checkSnackbar(R.string.snackbar_album_added)
        onView(withId(R.id.albumsCount)).check(matches(withText(countText(R.plurals.artist_detail_albums_count, 2))))
        assertEquals(listOf(abbeyRoad.name, buscando.name), carouselAlbumNamesInOrder())
        onView(newAlbumBadge(abbeyRoad.name)).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
        // El álbum que ya estaba no se marca
        onView(newAlbumBadge(buscando.name)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    // Errores al agregar

    @Test
    fun sinConexionAlAgregar_avisaSinBoton_conservaLaSeleccion_yAgregarOtraVezLoAgrega() {
        val albums = InMemoryArtistAlbumRepository(all)
        albums.addResponses = listOf(Result.failure(DataError.Network()), Result.success(Unit))
        openWith(albums)

        chooseAlbum(abbeyRoad.name)
        tapAddAlbum()

        checkSnackbar(R.string.error_album_add_connection)
        onView(candidateAlbumRadio(abbeyRoad.name)).check(matches(isChecked()))
        onView(withId(R.id.albumAddSubtitle)).check(matches(isDisplayed()))

        // Como en el prototipo, el aviso no tiene botón y se reintenta con el mismo Agregar
        onView(withId(SNACKBAR_ACTION)).check(matches(not(isDisplayed())))
        tapAddAlbum()

        checkSnackbar(R.string.snackbar_album_added)
        onView(newAlbumBadge(abbeyRoad.name)).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
        assertEquals(2, albums.added.size)
    }

    @Test
    fun sinConexionAlAgregarAUnaBanda_agregarOtraVezPideConfirmar_yLoAgrega() {
        val albums = InMemoryArtistAlbumRepository(all)
        albums.addResponses = listOf(Result.failure(DataError.Network()), Result.success(Unit))
        openWith(albums, artistName = queen.name)

        chooseAlbum(siembra.name)
        tapAddAlbum()
        onView(withText(R.string.album_replace_confirm)).inRoot(isDialog()).perform(click())
        checkSnackbar(R.string.error_album_add_connection)
        onView(candidateAlbumRadio(siembra.name)).check(matches(isChecked()))

        tapAddAlbum()
        onView(withText(R.string.album_replace_confirm)).inRoot(isDialog()).perform(click())

        checkSnackbar(R.string.snackbar_album_added)
        assertEquals(List(2) { Added(queen.id, ArtistType.BAND, siembra.id) }, albums.added.toList())
    }

    @Test
    fun siElServidorFallaAlAgregar_avisa_yConservaLaSeleccion() {
        val albums = InMemoryArtistAlbumRepository(all)
        albums.addResponses = listOf(Result.failure(DataError.Server(500)))
        openWith(albums)

        chooseAlbum(abbeyRoad.name)
        tapAddAlbum()

        checkSnackbar(R.string.error_album_add_failed)
        onView(candidateAlbumRadio(abbeyRoad.name)).check(matches(isChecked()))
        onView(withId(R.id.albumAddButton)).check(matches(isEnabled()))
    }

    @Test
    fun siElAlbumYaNoExiste_loIndica_yActualizaElListado() {
        val albums = InMemoryArtistAlbumRepository(all)
        albums.candidateResponses = listOf(Result.success(all), Result.success(listOf(siembraCandidate, buscandoCandidate)))
        albums.addResponses = listOf(Result.failure(DataError.NotFound()))
        openWith(albums)

        chooseAlbum(abbeyRoad.name)
        tapAddAlbum()

        checkSnackbar(R.string.error_album_add_not_found)
        waitForAlbumCandidates { rows -> rows.none { it.candidate.album.id == abbeyRoad.id } }
        onView(candidateAlbumName(abbeyRoad.name)).check(doesNotExist())
        onView(candidateAlbumName(siembra.name)).check(matches(isDisplayed()))
        assertEquals(2, albums.candidateRequests.size)
    }

    private companion object {
        /** Botón del aviso, que es una vista interna de Material. */
        val SNACKBAR_ACTION = com.google.android.material.R.id.snackbar_action
    }
}
