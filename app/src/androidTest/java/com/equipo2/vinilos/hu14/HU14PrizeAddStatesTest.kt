package com.equipo2.vinilos.hu14

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
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
import com.equipo2.vinilos.data.model.ArtistPrize
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.data.model.Prize
import com.equipo2.vinilos.data.model.PrizeCandidate
import com.equipo2.vinilos.data.repository.DataError
import com.equipo2.vinilos.hu03.testMusician
import com.equipo2.vinilos.hu04.InMemoryArtistDetailRepository
import com.equipo2.vinilos.hu04.checkShowingArtist
import com.equipo2.vinilos.hu12.string
import com.equipo2.vinilos.support.VinilosRule
import com.equipo2.vinilos.support.checkSnackbar
import com.equipo2.vinilos.support.hasFieldError
import com.equipo2.vinilos.support.scrollToWithBarsHidden
import com.equipo2.vinilos.support.usePrizeRepositories
import com.equipo2.vinilos.support.waitUntil
import kotlinx.coroutines.CompletableDeferred
import org.hamcrest.Matchers.not
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * UI de HU14 con repositorios en memoria: carga, catálogo sin premios, error al cargar, artista que ya no existe,
 * envío en curso, y los fallos al guardar por conexión, servidor, fecha rechazada y premio o artista que ya no existen.
 */
@RunWith(AndroidJUnit4::class)
class HU14PrizeAddStatesTest {

    @get:Rule
    val app = VinilosRule()

    private val ruben = testMusician(100, "Rubén Blades Bellido de Luna", 1948)
    private val grammyDelivery = ArtistPrize(100, "Grammy Award", "National Academy", LocalDate.of(1978, 12, 10))
    private val rubenDetail = ArtistDetail(artist = ruben, description = LONG_DESCRIPTION, prizes = listOf(grammyDelivery))

    private val grammy = PrizeCandidate(Prize(100, "Grammy Award", "National Academy"), listOf(LocalDate.of(1978, 12, 10), LocalDate.of(1990, 2, 21)))
    private val latinGrammy = PrizeCandidate(Prize(1, "Latin Grammy", "Academia Latina de Artes y Ciencias de la Grabación"))
    private val all = listOf(grammy, latinGrammy)

    private fun openWith(prizes: InMemoryPrizeRepository) {
        usePrizeRepositories(InMemoryArtistDetailRepository(listOf(rubenDetail)), prizes)
        app.launch()
        openArtistDetail(ruben.name)
        openPrizeAdd()
    }

    /** Elige el Latin Grammy y la fecha de hoy, y toca Guardar. */
    private fun saveLatinGrammy() {
        choosePrize(latinGrammy.prize.name)
        pickTodayAsPremiationDate()
        tapSavePrize()
    }

    // Detalle

    @Test
    fun laDescripcionExpandida_sigueExpandida_alVolverConElPremioAgregado() {
        usePrizeRepositories(InMemoryArtistDetailRepository(listOf(rubenDetail)), InMemoryPrizeRepository(all))
        app.launch()
        openArtistDetail(ruben.name)
        onView(withId(R.id.artistDescriptionToggle)).perform(scrollToWithBarsHidden(), click())
        onView(withId(R.id.artistDescription)).check(matches(withText(LONG_DESCRIPTION)))

        openPrizeAdd()
        saveLatinGrammy()

        checkSnackbar(R.string.snackbar_prize_added)
        onView(withId(R.id.artistDescription)).check(matches(withText(LONG_DESCRIPTION)))
        onView(withId(R.id.artistDescriptionToggle)).check(matches(withText(R.string.description_collapse)))
    }

    // Carga

    @Test
    fun mientrasCarga_muestraCargandoPremios_sinElFormularioNiLosBotones() {
        val prizes = InMemoryPrizeRepository(all)
        prizes.neverAnswerCandidates = true
        openWith(prizes)

        onView(withText(R.string.prize_add_loading)).check(matches(isDisplayed()))
        onView(withId(R.id.prizeScroll)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.prizeFooter)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun sinPremiosEnElCatalogo_loDice_yGuardarPideElPremio() {
        val prizes = InMemoryPrizeRepository(emptyList())
        openWith(prizes)

        onView(withText(R.string.prize_add_empty)).check(matches(isDisplayed()))
        onView(withId(R.id.prizeCard)).check(matches(withEffectiveVisibility(Visibility.GONE)))

        pickTodayAsPremiationDate()
        tapSavePrize()

        onView(withText(R.string.error_prize_required)).check(matches(isDisplayed()))
        assertEquals(0, prizes.added.size)
    }

    @Test
    fun siNoCargaPorConexion_muestraElError_yReintentarCargaLosPremios() {
        val prizes = InMemoryPrizeRepository(all)
        prizes.candidateResponses = listOf(Result.failure(DataError.Network()), Result.success(all))
        openWith(prizes)

        onView(withText(string(R.string.error_load_title, string(R.string.prize_add_object)))).check(matches(isDisplayed()))
        onView(withText(R.string.error_network_body)).check(matches(isDisplayed()))
        onView(withId(R.id.prizeFooter)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.stateRetry)).perform(click())

        onView(candidatePrizeName(latinGrammy.prize.name)).check(matches(isDisplayed()))
        assertEquals(listOf(100 to ArtistType.MUSICIAN, 100 to ArtistType.MUSICIAN), prizes.candidateRequests.toList())
    }

    @Test
    fun siElArtistaYaNoExiste_loIndica_sinOfrecerReintentar() {
        val prizes = InMemoryPrizeRepository(all)
        prizes.candidateResponses = listOf(Result.failure(DataError.NotFound()))
        openWith(prizes)

        onView(withText(R.string.prize_add_artist_missing_title)).check(matches(isDisplayed()))
        onView(withText(R.string.prize_add_artist_missing_body)).check(matches(isDisplayed()))
        onView(withId(R.id.stateRetry)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun unPremioRecibidoVariasVeces_diceTodasLasFechas() {
        openWith(InMemoryPrizeRepository(all))

        val dates = string(
            R.string.prize_received_dates_join,
            string(R.string.prize_received_date, "10 de diciembre de 1978"),
            string(R.string.prize_received_date, "21 de febrero de 1990"),
        )
        onView(candidatePrizeReceived(grammy.prize.name)).check(matches(withText(string(R.string.prize_already_received, dates))))
    }

    // Envío

    @Test
    fun mientrasGuarda_muestraGuardando_yDeshabilitaLasAcciones_paraNoEnviarDosVeces() {
        val prizes = InMemoryPrizeRepository(all)
        val gate = CompletableDeferred<Unit>()
        prizes.addGate = gate
        openWith(prizes)

        saveLatinGrammy()

        onView(withId(R.id.prizeSavingProgress)).check(matches(isDisplayed()))
        onView(withId(R.id.prizeSaveButton)).check(matches(withText(R.string.prize_add_saving))).check(matches(not(isEnabled())))
        onView(withId(R.id.prizeCancelButton)).check(matches(not(isEnabled())))
        onView(candidatePrizeRow(grammy.prize.name)).check(matches(not(isEnabled())))
        assertEquals(1, prizes.added.size)

        gate.complete(Unit)

        checkSnackbar(R.string.snackbar_prize_added)
        assertEquals(listOf(InMemoryPrizeRepository.Added(100, ArtistType.MUSICIAN, 1, LocalDate.now())), prizes.added.toList())
        assertEquals(listOf(latinGrammy.prize.name, grammy.prize.name), artistPrizeNamesInOrder())
    }

    @Test
    fun sinConexionAlGuardar_avisa_conservaElPremioYLaFecha_yAlReintentarSeAgrega() {
        val prizes = InMemoryPrizeRepository(all)
        prizes.addFailures = listOf(DataError.Network())
        openWith(prizes)

        saveLatinGrammy()

        checkSnackbar(R.string.error_prize_add_connection)
        onView(candidatePrizeRadio(latinGrammy.prize.name)).check(matches(isChecked()))
        onView(withId(R.id.premiationDateInput)).check(matches(withText(todayInField())))
        onView(withId(R.id.prizeSaveButton)).check(matches(withText(R.string.action_save))).check(matches(isEnabled()))

        tapSavePrize()

        checkSnackbar(R.string.snackbar_prize_added)
        checkShowingArtist(ruben.name)
        onView(newPrizeBadge(latinGrammy.prize.name)).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
        assertEquals(2, prizes.added.size)
    }

    @Test
    fun siElServidorFallaAlGuardar_avisaQueNoSePudoAgregar_yConservaLoElegido() {
        val prizes = InMemoryPrizeRepository(all)
        prizes.addFailures = listOf(DataError.Server(500))
        openWith(prizes)

        saveLatinGrammy()

        checkSnackbar(R.string.error_prize_add_failed)
        onView(candidatePrizeRadio(latinGrammy.prize.name)).check(matches(isChecked()))
        onView(withId(R.id.premiationDateInput)).check(matches(withText(todayInField())))
    }

    @Test
    fun siLaApiNoAceptaLaFecha_elErrorSaleEnElCampoDeFecha() {
        val prizes = InMemoryPrizeRepository(all)
        prizes.addFailures = listOf(DataError.Request(400))
        openWith(prizes)

        saveLatinGrammy()

        waitUntil {
            runCatching {
                onView(withId(R.id.premiationDateField)).check(matches(hasFieldError(R.string.error_premiation_date_invalid)))
            }.isSuccess
        }
        onView(candidatePrizeRadio(latinGrammy.prize.name)).check(matches(isChecked()))
        onView(withId(R.id.premiationDateField)).perform(scrollTo()).check(matches(isDisplayed()))
    }

    @Test
    fun siElPremioOElArtistaYaNoExisten_avisa_yVuelveAPedirElListado() {
        val prizes = InMemoryPrizeRepository(all)
        prizes.addFailures = listOf(DataError.NotFound())
        // Al recargar, el premio que se había elegido ya no está
        prizes.candidateResponses = listOf(Result.success(all), Result.success(listOf(grammy)))
        openWith(prizes)

        saveLatinGrammy()

        checkSnackbar(R.string.error_prize_add_not_found)
        waitForPrizes { rows -> rows.map { it.candidate.prize.name } == listOf(grammy.prize.name) && rows.none { it.selected } }
        assertEquals(2, prizes.candidateRequests.size)
    }

    private companion object {
        /** Más larga que lo que el detalle muestra sin «Ver más». */
        val LONG_DESCRIPTION = "Cantante, compositor, músico, actor, abogado y político panameño. ".repeat(4).trim()
    }
}
