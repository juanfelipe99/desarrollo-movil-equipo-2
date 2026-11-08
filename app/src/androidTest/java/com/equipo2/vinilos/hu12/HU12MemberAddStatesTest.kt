package com.equipo2.vinilos.hu12

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
import com.equipo2.vinilos.data.model.ArtistDetail
import com.equipo2.vinilos.data.model.MusicianCandidate
import com.equipo2.vinilos.data.model.MusicianStatus
import com.equipo2.vinilos.data.repository.DataError
import com.equipo2.vinilos.hu03.testBand
import com.equipo2.vinilos.hu03.testMusician
import com.equipo2.vinilos.hu04.InMemoryArtistDetailRepository
import com.equipo2.vinilos.support.VinilosRule
import com.equipo2.vinilos.support.checkSnackbar
import com.equipo2.vinilos.support.scrollToWithBarsHidden
import com.equipo2.vinilos.support.useBandRepositories
import org.hamcrest.Matchers.not
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.CompletableDeferred

/**
 * UI de HU12 con repositorios en memoria: sin músicos disponibles, error al cargar, envío en curso,
 * sin conexión al agregar con Reintentar, y banda o músico que ya no existen.
 */
@RunWith(AndroidJUnit4::class)
class HU12MemberAddStatesTest {

    @get:Rule
    val app = VinilosRule()

    private val queen = testBand(101, "Queen", 1970)
    private val freddie = testMusician(9, "Freddie Mercury", 1946)
    private val brian = testMusician(10, "Brian May", 1947)
    private val ruben = testMusician(100, "Rubén Blades", 1948)

    private val queenDetail = ArtistDetail(artist = queen, description = "Banda británica de rock.", members = listOf(freddie))

    private val brianCandidate = MusicianCandidate(brian, MusicianStatus.NoBand)
    private val rubenCandidate = MusicianCandidate(ruben, MusicianStatus.InOtherBand(200, "Seis del Solar"))
    private val freddieCandidate = MusicianCandidate(freddie, MusicianStatus.AlreadyMember)
    private val all = listOf(brianCandidate, rubenCandidate, freddieCandidate)

    private fun openWith(bands: InMemoryBandRepository) {
        useBandRepositories(InMemoryArtistDetailRepository(listOf(queenDetail)), bands)
        app.launch()
        openBand(queen.name)
        openMemberAdd()
    }

    private fun tapAdd() = onView(withId(R.id.memberAddButton)).perform(click())

    // Carga

    @Test
    fun sinMusicosDisponibles_muestraElMensajeInformativo() {
        openWith(InMemoryBandRepository(listOf(freddieCandidate)))

        onView(withText(R.string.member_add_empty_title)).check(matches(isDisplayed()))
        onView(withText(R.string.member_add_empty_body)).check(matches(isDisplayed()))
    }

    @Test
    fun siNoCargaPorConexion_muestraElError_yReintentarCargaElListado() {
        val bands = InMemoryBandRepository(all)
        bands.candidateResponses = listOf(Result.failure(DataError.Network()), Result.success(all))
        openWith(bands)

        onView(withText(string(R.string.error_load_title, string(R.string.member_add_object)))).check(matches(isDisplayed()))
        onView(withText(R.string.error_network_body)).check(matches(isDisplayed()))
        onView(withId(R.id.stateRetry)).perform(click())

        onView(candidateName(brian.name)).check(matches(isDisplayed()))
        assertEquals(2, bands.candidateRequests.size)
    }

    @Test
    fun siLaBandaYaNoExiste_loIndica_sinOfrecerReintentar() {
        val bands = InMemoryBandRepository(all)
        bands.candidateResponses = listOf(Result.failure(DataError.NotFound()))
        openWith(bands)

        onView(withText(R.string.member_add_band_missing_title)).check(matches(isDisplayed()))
        onView(withText(R.string.member_add_band_missing_body)).check(matches(isDisplayed()))
        onView(withId(R.id.stateRetry)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    // Envío

    @Test
    fun mientrasSeAgrega_muestraLaCarga_yDeshabilitaLasAcciones_paraNoEnviarDosVeces() {
        val bands = InMemoryBandRepository(all)
        val gate = CompletableDeferred<Unit>()
        bands.addGate = gate
        openWith(bands)

        chooseMusician(brian.name)
        tapAdd()

        onView(withId(R.id.memberSubmittingProgress)).check(matches(isDisplayed()))
        onView(withId(R.id.memberAddButton)).check(matches(withText(R.string.member_add_saving)))
        onView(withId(R.id.memberAddButton)).check(matches(not(isEnabled())))
        onView(withId(R.id.memberCancelButton)).check(matches(not(isEnabled())))
        onView(withId(R.id.memberSearchField)).check(matches(not(isEnabled())))
        onView(candidateRow(ruben.name)).check(matches(not(isEnabled())))

        // Tocar Agregar otra vez no hace un segundo envío
        tapAdd()
        assertEquals(1, bands.added.size)

        gate.complete(Unit)
        checkSnackbar(R.string.snackbar_member_added)
        assertEquals(listOf(queen.id to brian.id), bands.added.toList())
    }

    @Test
    fun alAgregar_vuelveAlDetalle_conElIntegranteMarcadoComoNuevo() {
        openWith(InMemoryBandRepository(all))

        chooseMusician(brian.name)
        tapAdd()

        checkSnackbar(R.string.snackbar_member_added)
        onView(newMemberBadge(brian.name)).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
        // El integrante que ya estaba no se marca
        onView(newMemberBadge(freddie.name)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    // Errores al agregar

    @Test
    fun sinConexionAlAgregar_avisaSinBoton_conservaLaSeleccion_yAgregarOtraVezLoAgrega() {
        val bands = InMemoryBandRepository(all)
        bands.addResponses = listOf(Result.failure(DataError.Network()), Result.success(Unit))
        openWith(bands)

        chooseMusician(brian.name)
        tapAdd()

        checkSnackbar(R.string.error_member_add_connection)
        onView(candidateRadio(brian.name)).check(matches(isChecked()))
        onView(withId(R.id.memberAddSubtitle)).check(matches(isDisplayed()))

        // Como en el prototipo, el aviso no tiene botón y se reintenta con el mismo Agregar
        onView(withId(SNACKBAR_ACTION)).check(matches(not(isDisplayed())))
        tapAdd()

        checkSnackbar(R.string.snackbar_member_added)
        onView(newMemberBadge(brian.name)).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
        assertEquals(listOf(queen.id to brian.id, queen.id to brian.id), bands.added.toList())
    }

    @Test
    fun sinConexionAlCambiarDeBanda_agregarOtraVezPideConfirmar_yLoAgrega() {
        val bands = InMemoryBandRepository(all)
        bands.addResponses = listOf(Result.failure(DataError.Network()), Result.success(Unit))
        openWith(bands)

        chooseMusician(ruben.name)
        tapAdd()
        onView(withText(R.string.band_change_confirm)).inRoot(isDialog()).perform(click())
        checkSnackbar(R.string.error_member_add_connection)

        tapAdd()
        onView(withText(R.string.band_change_confirm)).inRoot(isDialog()).perform(click())

        checkSnackbar(R.string.snackbar_member_added)
        assertEquals(2, bands.added.size)
    }

    @Test
    fun siElServidorFallaAlAgregar_avisa_yConservaLaSeleccion() {
        val bands = InMemoryBandRepository(all)
        bands.addResponses = listOf(Result.failure(DataError.Server(500)))
        openWith(bands)

        chooseMusician(brian.name)
        tapAdd()

        checkSnackbar(R.string.error_member_add_failed)
        onView(candidateRadio(brian.name)).check(matches(isChecked()))
        onView(withId(R.id.memberAddButton)).check(matches(isEnabled()))
    }

    @Test
    fun siElMusicoYaNoExiste_loIndica_yActualizaElListado() {
        val bands = InMemoryBandRepository(all)
        bands.candidateResponses = listOf(Result.success(all), Result.success(listOf(rubenCandidate, freddieCandidate)))
        bands.addResponses = listOf(Result.failure(DataError.NotFound()))
        openWith(bands)

        chooseMusician(brian.name)
        tapAdd()

        checkSnackbar(R.string.error_member_add_not_found)
        onView(candidateName(brian.name)).check(doesNotExist())
        onView(candidateName(ruben.name)).check(matches(isDisplayed()))
        assertEquals(2, bands.candidateRequests.size)
    }

    private companion object {
        /** Botón del aviso, que es una vista interna de Material. */
        val SNACKBAR_ACTION = com.google.android.material.R.id.snackbar_action
    }
}
