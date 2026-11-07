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
import com.equipo2.vinilos.hu04.checkShowingArtist
import com.equipo2.vinilos.hu04.countText
import com.equipo2.vinilos.hu04.memberRow
import com.equipo2.vinilos.support.BackVynils
import com.equipo2.vinilos.support.VinilosRule
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
 * E2E de HU12 con la API: botón según el rol, estado de cada músico, buscador, «Elige un músico»,
 * cambio de banda con confirmación e integrante nuevo marcado en el detalle y guardado en la API.
 *
 * Cada prueba crea su banda con un integrante, otra banda con un integrante y un músico sin banda,
 * con un sufijo propio para buscarlos sin depender de los datos del servidor.
 */
@RunWith(AndroidJUnit4::class)
class HU12AddMemberTest {

    @get:Rule
    val app = VinilosRule()

    private val suffix = System.currentTimeMillis().toString()
    private val bandName = "$PREFIX Banda $suffix"
    private val otherBandName = "$PREFIX Otra $suffix"
    private val freeMusician = "$PREFIX Libre $suffix"
    private val otherBandMusician = "$PREFIX Ajeno $suffix"
    private val currentMember = "$PREFIX Integrante $suffix"

    private var bandId = 0
    private var otherBandId = 0

    /** Lo que crea esta prueba. Se borra solo eso, para no borrar los datos de otra ejecución contra la misma API. */
    private val createdMusicians = mutableListOf<Int>()
    private val createdBands = mutableListOf<Int>()

    @Before
    fun createTestArtists() {
        bandId = createBand(bandName)
        otherBandId = createBand(otherBandName)
        createMusician(freeMusician)
        BackVynils.addMusicianToBand(otherBandId, createMusician(otherBandMusician))
        BackVynils.addMusicianToBand(bandId, createMusician(currentMember))
    }

    @After
    fun deleteTestArtists() {
        BackVynils.deleteArtists(createdMusicians, createdBands)
    }

    private fun createBand(name: String) = BackVynils.createBand(name).also { createdBands += it }

    private fun createMusician(name: String) = BackVynils.createMusician(name).also { createdMusicians += it }

    // Botón según el rol

    @Test
    fun visitante_noVeAgregarIntegrante() {
        app.launch()
        openBand(bandName, collector = false)

        onView(memberRow(currentMember)).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
        onView(withId(R.id.addMemberButton)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun coleccionista_veAgregarIntegrante_yElVacioLeDiceComoAgregarElPrimero() {
        val emptyBand = "$PREFIX Vacia $suffix"
        createBand(emptyBand)
        app.launch()
        openBand(emptyBand)

        onView(withId(R.id.addMemberButton)).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
        onView(withText(R.string.artist_detail_members_empty_body_collector)).perform(scrollToWithBarsHidden())
            .check(matches(isDisplayed()))
    }

    // Listado y buscador

    @Test
    fun elListado_muestraElEstadoDeCadaMusico_yLosIntegrantesAlFinalDeshabilitados() {
        openMemberAddScreen()
        searchMusician(suffix)

        onView(toolbarTitle(R.string.member_add_title)).check(matches(isDisplayed()))
        onView(withId(R.id.memberAddSubtitle)).check(matches(withText(string(R.string.member_add_subtitle, bandName))))
        onView(candidateStatus(freeMusician)).check(matches(withText(R.string.member_status_no_band)))
        onView(candidateStatus(otherBandMusician)).check(matches(withText(string(R.string.member_status_in_band, otherBandName))))
        onView(candidateStatus(currentMember)).check(matches(withText(R.string.member_status_member)))
        onView(candidateRow(currentMember)).check(matches(not(isEnabled())))
        assertEquals(currentMember, candidateNamesInOrder().last())
    }

    @Test
    fun elBuscador_filtraPorNombreSinImportarMayusculas() {
        openMemberAddScreen()

        searchMusician("libre $suffix")

        onView(candidateName(freeMusician)).check(matches(isDisplayed()))
        onView(candidateName(otherBandMusician)).check(doesNotExist())
        onView(candidateName(currentMember)).check(doesNotExist())
    }

    @Test
    fun unIntegranteActual_noSePuedeElegir() {
        openMemberAddScreen()
        searchMusician(suffix)

        tapMusician(currentMember)

        onView(candidateRadio(currentMember)).check(matches(not(isChecked())))
    }

    // Agregar

    @Test
    fun agregarSinElegir_muestraEligeUnMusico_yNoCambiaLaBanda() {
        openMemberAddScreen()

        onView(withId(R.id.memberAddButton)).perform(click())

        onView(withId(R.id.memberRequiredError)).check(matches(isDisplayed()))
        onView(withId(R.id.memberRequiredError)).check(matches(withText(R.string.error_member_required)))
        assertEquals(listOf(currentMember), BackVynils.bandMemberNames(bandId))
    }

    @Test
    fun agregarUnMusicoSinBanda_vuelveAlDetalle_marcadoComoNuevo_conElAviso_yQuedaEnLaApi() {
        openMemberAddScreen()
        searchMusician(suffix)

        chooseMusician(freeMusician)
        onView(withId(R.id.memberAddButton)).perform(click())

        // El aviso dura unos segundos, así que se revisa antes que lo demás
        checkSnackbar(R.string.snackbar_member_added)
        checkShowingArtist(bandName)
        onView(withId(R.id.membersCount)).check(matches(withText(countText(R.plurals.artist_detail_members_count, 2))))
        onView(newMemberBadge(freeMusician)).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
        assertEquals(setOf(currentMember, freeMusician), BackVynils.bandMemberNames(bandId).toSet())
    }

    @Test
    fun unMusicoDeOtraBanda_avisaQueLaDejara_yAlConfirmarCambiaDeBanda() {
        openMemberAddScreen()
        searchMusician(suffix)

        chooseMusician(otherBandMusician)
        onView(candidateLeavesBand(otherBandMusician)).check(matches(withText(string(R.string.member_leaves_band, otherBandName))))
        onView(withId(R.id.memberAddButton)).perform(click())

        onView(withText(R.string.band_change_title)).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText(R.string.band_change_confirm)).inRoot(isDialog()).perform(click())

        checkSnackbar(R.string.snackbar_member_added)
        onView(newMemberBadge(otherBandMusician)).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
        assertEquals(setOf(currentMember, otherBandMusician), BackVynils.bandMemberNames(bandId).toSet())
        assertEquals(emptyList<String>(), BackVynils.bandMemberNames(otherBandId))
    }

    @Test
    fun cancelarElCambioDeBanda_noAgregaNada_yConservaLaSeleccion() {
        openMemberAddScreen()
        searchMusician(suffix)

        chooseMusician(otherBandMusician)
        onView(withId(R.id.memberAddButton)).perform(click())
        onView(withText(R.string.action_cancel)).inRoot(isDialog()).perform(click())

        onView(withId(R.id.memberAddSubtitle)).check(matches(isDisplayed()))
        onView(candidateRadio(otherBandMusician)).check(matches(isChecked()))
        assertEquals(listOf(currentMember), BackVynils.bandMemberNames(bandId))
        assertEquals(listOf(otherBandMusician), BackVynils.bandMemberNames(otherBandId))
    }

    @Test
    fun cancelar_vuelveAlDetalle_sinAgregar() {
        openMemberAddScreen()
        searchMusician(suffix)

        chooseMusician(freeMusician)
        onView(withId(R.id.memberCancelButton)).perform(click())

        checkShowingArtist(bandName)
        onView(withId(R.id.membersCount)).check(matches(withText(countText(R.plurals.artist_detail_members_count, 1))))
        assertEquals(listOf(currentMember), BackVynils.bandMemberNames(bandId))
    }

    @Test
    fun alGirarLaPantalla_seConservanLaBusquedaYLaSeleccion_yAgregaAlMismoMusico() {
        openMemberAddScreen()
        searchMusician(suffix)
        chooseMusician(freeMusician)

        rotateScreen()

        onView(withId(R.id.memberSearchInput)).check(matches(withText(suffix)))
        onView(candidateRadio(freeMusician)).check(matches(isChecked()))
        onView(withId(R.id.memberAddButton)).perform(click())

        checkSnackbar(R.string.snackbar_member_added)
        assertEquals(setOf(currentMember, freeMusician), BackVynils.bandMemberNames(bandId).toSet())
    }

    /** Entra como coleccionista, abre la banda de prueba y toca Agregar integrante. */
    private fun openMemberAddScreen() {
        app.launch()
        openBand(bandName)
        openMemberAdd()
    }

    private companion object {
        /** Prefijo de los músicos y bandas de prueba, para reconocerlos en la API. */
        const val PREFIX = "E2E HU12"
    }
}
