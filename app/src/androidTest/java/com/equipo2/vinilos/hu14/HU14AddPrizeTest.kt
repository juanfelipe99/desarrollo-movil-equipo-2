package com.equipo2.vinilos.hu14

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.equipo2.vinilos.R
import com.equipo2.vinilos.hu04.checkShowingArtist
import com.equipo2.vinilos.hu04.countText
import com.equipo2.vinilos.hu12.string
import com.equipo2.vinilos.support.BackVynils
import com.equipo2.vinilos.support.OwnData
import com.equipo2.vinilos.support.VinilosRule
import com.equipo2.vinilos.support.checkSnackbar
import com.equipo2.vinilos.support.hasFieldError
import com.equipo2.vinilos.support.rotateScreen
import com.equipo2.vinilos.support.scrollToWithBarsHidden
import com.equipo2.vinilos.support.toolbarTitle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * E2E de HU14 con la API: botón según el rol, listado de premios con la nota del que ya se recibió, errores de
 * validación, y el premio nuevo primero y marcado en el detalle y guardado en la API, para un músico y para una banda.
 *
 * Cada prueba crea su músico y sus premios, y solo revisa esos datos.
 */
@RunWith(AndroidJUnit4::class)
class HU14AddPrizeTest {

    @get:Rule
    val app = VinilosRule()

    private val own = OwnData()

    private val musicianName = own.name("HU14 Músico")
    private val prizeName = own.name("HU14 Premio")
    private val receivedPrizeName = own.name("HU14 Recibido")

    private var musicianId = 0

    @Before
    fun createTestData() {
        musicianId = own.musician(musicianName)
        own.prize(prizeName, ORGANIZATION)
        BackVynils.givePrize(own.prize(receivedPrizeName, ORGANIZATION), musicianId, "1978-12-10")
    }

    @After
    fun deleteTestData() = own.deleteAll()

    // Botón según el rol

    @Test
    fun visitante_noVeAgregarPremio() {
        app.launch()
        openArtistDetail(musicianName, collector = false)

        onView(artistPrizeName(receivedPrizeName)).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
        onView(withId(R.id.addPrizeButton)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun coleccionista_veAgregarPremio_yElVacioLeDiceComoAgregarElPrimero() {
        val withoutPrizes = own.name("HU14 Sin premios")
        own.musician(withoutPrizes)
        app.launch()
        openArtistDetail(withoutPrizes)

        onView(withId(R.id.addPrizeButton)).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
        onView(withText(R.string.artist_detail_prizes_empty_body_collector)).perform(scrollToWithBarsHidden())
            .check(matches(isDisplayed()))
    }

    @Test
    fun visitante_enElVacioSoloVeSinPremiosRegistrados() {
        val withoutPrizes = own.name("HU14 Sin premios")
        own.musician(withoutPrizes)
        app.launch()
        openArtistDetail(withoutPrizes, collector = false)

        onView(withText(R.string.artist_detail_prizes_empty_body)).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
    }

    // Listado

    @Test
    fun laPantalla_nombraAlArtista_yMuestraCadaPremioConSuOrganizacion_sinNingunoElegido() {
        openPrizeAddScreen()

        onView(toolbarTitle(R.string.prize_add_title)).check(matches(isDisplayed()))
        onView(withId(R.id.prizeAddSubtitle)).check(matches(withText(string(R.string.prize_add_subtitle, musicianName))))
        onView(candidatePrizeOrganization(prizeName)).perform(scrollTo()).check(matches(withText(ORGANIZATION)))
        onView(candidatePrizeRadio(prizeName)).check(matches(isNotChecked()))
        // Las pantallas que se cierran con X no llevan la barra de navegación
        onView(withId(R.id.navBar)).check(matches(withEffectiveVisibility(Visibility.GONE)))

        val own = candidatePrizeNamesInOrder().filter { it == prizeName || it == receivedPrizeName }
        assertEquals("Los premios van en orden alfabético", listOf(prizeName, receivedPrizeName), own)
    }

    @Test
    fun unPremioYaRecibido_diceCuando_ySePuedeAgregarOtraVezConOtraFecha() {
        openPrizeAddScreen()

        val note = string(R.string.prize_already_received, string(R.string.prize_received_date, "10 de diciembre de 1978"))
        onView(candidatePrizeReceived(receivedPrizeName)).perform(scrollTo()).check(matches(withText(note)))
        onView(candidatePrizeReceived(prizeName)).check(matches(withEffectiveVisibility(Visibility.GONE)))

        choosePrize(receivedPrizeName)
        pickTodayAsPremiationDate()
        tapSavePrize()

        checkShowingArtist(musicianName)
        onView(withId(R.id.prizesCount)).perform(scrollToWithBarsHidden())
            .check(matches(withText(countText(R.plurals.artist_detail_prizes_count, 2))))
        assertEquals(listOf(receivedPrizeName, receivedPrizeName), artistPrizeNamesInOrder())
        assertEquals(listOf("1978-12-10", LocalDate.now().toString()), BackVynils.musicianPrizeDays(musicianId))
    }

    // Validación

    @Test
    fun guardarSinElegir_pideElPremioYLaFecha_yCadaErrorSeQuitaAlCorregirlo() {
        openPrizeAddScreen()

        tapSavePrize()

        onView(withText(R.string.error_prize_required)).check(matches(isDisplayed()))
        onView(withId(R.id.premiationDateField)).perform(scrollTo())
            .check(matches(hasFieldError(R.string.error_premiation_date_required)))
        assertEquals(listOf("1978-12-10"), BackVynils.musicianPrizeDays(musicianId))

        choosePrize(prizeName)
        onView(withId(R.id.prizeRequiredError)).check(matches(withEffectiveVisibility(Visibility.GONE)))

        pickTodayAsPremiationDate()
        onView(withId(R.id.premiationDateField)).check(matches(hasFieldError(null)))
    }

    @Test
    fun elSelectorDeFecha_muestraLaFechaElegidaEnElCampo() {
        openPrizeAddScreen()

        pickTodayAsPremiationDate()

        onView(withId(R.id.premiationDateInput)).check(matches(withText(todayInField())))
    }

    // Envío

    @Test
    fun agregarUnPremio_vuelveAlDetalle_conElPremioPrimeroMarcadoComoNuevo_elAviso_yQuedaEnLaApi() {
        openPrizeAddScreen()

        choosePrize(prizeName)
        onView(candidatePrizeRadio(prizeName)).check(matches(isChecked()))
        pickTodayAsPremiationDate()
        tapSavePrize()

        checkSnackbar(R.string.snackbar_prize_added)
        checkShowingArtist(musicianName)
        onView(newPrizeBadge(prizeName)).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
        onView(newPrizeBadge(receivedPrizeName)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        assertEquals(listOf(prizeName, receivedPrizeName), artistPrizeNamesInOrder())
        onView(withId(R.id.prizesCount)).check(matches(withText(countText(R.plurals.artist_detail_prizes_count, 2))))
        assertEquals(listOf("1978-12-10", LocalDate.now().toString()), BackVynils.musicianPrizeDays(musicianId))

        // Al girar sigue primero y marcado
        rotateScreen()
        onView(newPrizeBadge(prizeName)).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
        assertEquals(listOf(prizeName, receivedPrizeName), artistPrizeNamesInOrder())
    }

    @Test
    fun aUnaBanda_tambienSeLeAgregaElPremio_yQuedaEnLaApi() {
        val bandName = own.name("HU14 Banda")
        val bandId = own.band(bandName)
        app.launch()
        openArtistDetail(bandName)
        openPrizeAdd()

        choosePrize(prizeName)
        pickTodayAsPremiationDate()
        tapSavePrize()

        checkSnackbar(R.string.snackbar_prize_added)
        checkShowingArtist(bandName)
        onView(newPrizeBadge(prizeName)).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
        assertEquals(listOf(LocalDate.now().toString()), BackVynils.bandPrizeDays(bandId))
    }

    // Salir sin agregar

    @Test
    fun cancelar_vuelveAlDetalle_sinAgregar() {
        openPrizeAddScreen()
        choosePrize(prizeName)
        pickTodayAsPremiationDate()

        onView(withId(R.id.prizeCancelButton)).perform(click())

        checkShowingArtist(musicianName)
        onView(artistPrizeName(prizeName)).check(doesNotExist())
        assertEquals(listOf("1978-12-10"), BackVynils.musicianPrizeDays(musicianId))
    }

    @Test
    fun atrasDeAndroid_vuelveAlDetalle_sinAgregar_yAgregarPremioSePuedeAbrirOtraVez() {
        openPrizeAddScreen()
        choosePrize(prizeName)

        pressBack()

        checkShowingArtist(musicianName)
        assertEquals(listOf("1978-12-10"), BackVynils.musicianPrizeDays(musicianId))

        // La pantalla empieza de nuevo, sin el premio que se había elegido
        openPrizeAdd()
        onView(toolbarTitle(R.string.prize_add_title)).check(matches(isDisplayed()))
        onView(candidatePrizeRadio(prizeName)).perform(scrollTo()).check(matches(isNotChecked()))
    }

    // Girar

    @Test
    fun alGirarLaPantalla_seConservanElPremioYLaFecha_ySeAgregaLoElegido() {
        openPrizeAddScreen()
        choosePrize(prizeName)
        pickTodayAsPremiationDate()

        rotateScreen()

        waitForPrizes { rows -> rows.any { it.selected && it.candidate.prize.name == prizeName } }
        onView(candidatePrizeRadio(prizeName)).perform(scrollTo()).check(matches(isChecked()))
        onView(withId(R.id.premiationDateInput)).perform(scrollTo()).check(matches(withText(todayInField())))

        tapSavePrize()

        checkSnackbar(R.string.snackbar_prize_added)
        assertEquals(listOf(prizeName, receivedPrizeName), artistPrizeNamesInOrder())
    }

    private fun openPrizeAddScreen() {
        app.launch()
        openArtistDetail(musicianName)
        openPrizeAdd()
    }

    private companion object {
        const val ORGANIZATION = "Academia E2E"
    }
}
