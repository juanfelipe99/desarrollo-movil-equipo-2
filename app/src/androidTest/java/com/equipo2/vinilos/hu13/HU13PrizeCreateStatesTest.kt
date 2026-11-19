package com.equipo2.vinilos.hu13

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.ArtistDetail
import com.equipo2.vinilos.data.model.NewPrize
import com.equipo2.vinilos.data.model.Prize
import com.equipo2.vinilos.data.model.PrizeCandidate
import com.equipo2.vinilos.data.repository.DataError
import com.equipo2.vinilos.hu03.testMusician
import com.equipo2.vinilos.hu04.InMemoryArtistDetailRepository
import com.equipo2.vinilos.hu04.checkShowingArtist
import com.equipo2.vinilos.hu14.InMemoryPrizeRepository
import com.equipo2.vinilos.hu14.candidatePrizeNamesInOrder
import com.equipo2.vinilos.hu14.candidatePrizeNewBadge
import com.equipo2.vinilos.hu14.candidatePrizeRadio
import com.equipo2.vinilos.hu14.choosePrize
import com.equipo2.vinilos.hu14.openArtistDetail
import com.equipo2.vinilos.hu14.openPrizeAdd
import com.equipo2.vinilos.hu14.pickTodayAsPremiationDate
import com.equipo2.vinilos.hu14.tapSavePrize
import com.equipo2.vinilos.hu14.todayInField
import com.equipo2.vinilos.hu14.waitForPrizes
import com.equipo2.vinilos.support.VinilosRule
import com.equipo2.vinilos.support.checkSnackbar
import com.equipo2.vinilos.support.hasFieldError
import com.equipo2.vinilos.support.toolbarTitle
import com.equipo2.vinilos.support.usePrizeRepositories
import com.equipo2.vinilos.support.waitUntil
import kotlinx.coroutines.CompletableDeferred
import org.hamcrest.Matchers.not
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI de HU13 con repositorios en memoria, sin tocar la API: entrar desde Agregar premio, campos obligatorios,
 * «Creando…», el error al guardar, el regreso con el premio nuevo primero, elegido y marcado, cancelar con
 * el botón, la X o atrás de Android, y agregar al artista el premio recién creado.
 */
@RunWith(AndroidJUnit4::class)
class HU13PrizeCreateStatesTest {

    @get:Rule
    val app = VinilosRule()

    private val ruben = testMusician(100, "Rubén Blades Bellido de Luna", 1948)
    private val rubenDetail = ArtistDetail(artist = ruben, description = "Cantante panameño.")

    private val grammy = PrizeCandidate(Prize(100, "Grammy Award", "National Academy"))
    private val latinGrammy = PrizeCandidate(Prize(1, "Latin Grammy", "Academia Latina de Artes y Ciencias de la Grabación"))

    private fun openPrizeCreate(prizes: InMemoryPrizeRepository) {
        usePrizeRepositories(InMemoryArtistDetailRepository(listOf(rubenDetail)), prizes)
        app.launch()
        openArtistDetail(ruben.name)
        openPrizeAdd()
        onView(withId(R.id.prizeCreateButton)).perform(scrollTo(), click())
    }

    private fun fillGardel() {
        onView(withId(R.id.nameInput)).perform(replaceText(NAME))
        onView(withId(R.id.organizationInput)).perform(replaceText(ORGANIZATION))
        onView(withId(R.id.descriptionInput)).perform(replaceText(DESCRIPTION), closeSoftKeyboard())
    }

    private fun tapCreate() {
        onView(withId(R.id.saveButton)).perform(click())
    }

    // Entrada

    @Test
    fun crearPremioNuevo_abreElFormularioConLaX() {
        openPrizeCreate(InMemoryPrizeRepository(listOf(grammy)))

        onView(toolbarTitle(R.string.prize_create_title)).check(matches(isDisplayed()))
        onView(withContentDescription(R.string.cd_close_without_saving)).check(matches(isDisplayed()))
        onView(withText(R.string.prize_create_note)).check(matches(isDisplayed()))
        onView(withId(R.id.saveButton)).check(matches(withText(R.string.prize_create_action)))
    }

    @Test
    fun sinPremiosEnElCatalogo_tambienSePuedeCrearElPrimero() {
        openPrizeCreate(InMemoryPrizeRepository(emptyList()))

        onView(toolbarTitle(R.string.prize_create_title)).check(matches(isDisplayed()))
    }

    // Validación

    @Test
    fun crearVacio_muestraLosTresErrores_yNoEnviaNada() {
        val prizes = InMemoryPrizeRepository(listOf(grammy))
        openPrizeCreate(prizes)

        tapCreate()

        onView(withId(R.id.nameField)).check(matches(hasFieldError(R.string.error_prize_name_required)))
        onView(withId(R.id.organizationField)).check(matches(hasFieldError(R.string.error_prize_organization_required)))
        onView(withId(R.id.descriptionField)).check(matches(hasFieldError(R.string.error_prize_description_required)))
        assertTrue(prizes.created.isEmpty())
    }

    @Test
    fun corregirUnCampo_quitaSoloSuError_ySoloEspaciosCuentaComoVacio() {
        openPrizeCreate(InMemoryPrizeRepository(listOf(grammy)))
        onView(withId(R.id.nameInput)).perform(replaceText("   "), closeSoftKeyboard())
        tapCreate()
        onView(withId(R.id.nameField)).check(matches(hasFieldError(R.string.error_prize_name_required)))

        onView(withId(R.id.organizationInput)).perform(scrollTo(), replaceText(ORGANIZATION), closeSoftKeyboard())

        onView(withId(R.id.organizationField)).check(matches(hasFieldError(null)))
        onView(withId(R.id.nameField)).check(matches(hasFieldError(R.string.error_prize_name_required)))
    }

    // Envío

    @Test
    fun mientrasCrea_muestraCreando_yNoDejaEnviarDosVeces() {
        val prizes = InMemoryPrizeRepository(listOf(grammy))
        prizes.createGate = CompletableDeferred()
        openPrizeCreate(prizes)
        fillGardel()

        tapCreate()

        onView(withId(R.id.savingProgress)).check(matches(isDisplayed()))
        onView(withId(R.id.saveButton)).check(matches(withText(R.string.prize_create_saving)))
        onView(withId(R.id.saveButton)).check(matches(not(isEnabled())))
        assertEquals(1, prizes.created.size)
    }

    @Test
    fun siFallaAlCrear_avisa_conservaLoEscrito_yPermiteReintentar() {
        val prizes = InMemoryPrizeRepository(listOf(grammy))
        prizes.createFailures = listOf(DataError.Network())
        openPrizeCreate(prizes)
        fillGardel()

        tapCreate()

        checkSnackbar(R.string.error_prize_create_failed)
        onView(withId(R.id.nameInput)).check(matches(withText(NAME)))
        onView(withId(R.id.saveButton)).check(matches(withText(R.string.prize_create_action)))

        tapCreate()

        checkSnackbar(R.string.snackbar_prize_created)
        assertEquals(2, prizes.created.size)
    }

    // Regreso

    @Test
    fun alCrear_vuelveConElPremioPrimero_elegido_yMarcadoComoNuevo() {
        val prizes = InMemoryPrizeRepository(listOf(grammy, latinGrammy))
        openPrizeCreate(prizes)
        fillGardel()

        tapCreate()

        checkSnackbar(R.string.snackbar_prize_created)
        onView(toolbarTitle(R.string.prize_add_title)).check(matches(isDisplayed()))
        waitForPrizes { rows -> rows.firstOrNull()?.candidate?.prize?.name == NAME }
        assertEquals(listOf(NAME, grammy.prize.name, latinGrammy.prize.name), candidatePrizeNamesInOrder())
        onView(candidatePrizeNewBadge(NAME)).check(matches(isDisplayed()))
        onView(candidatePrizeRadio(NAME)).check(matches(isChecked()))
        // Se envió lo escrito y el listado no se volvió a pedir
        assertEquals(listOf(NewPrize(NAME, ORGANIZATION, DESCRIPTION)), prizes.created.toList())
        assertEquals(1, prizes.candidateRequests.size)
    }

    @Test
    fun conElCatalogoVacio_alCrear_elPremioNuevoApareceElegido() {
        openPrizeCreate(InMemoryPrizeRepository(emptyList()))
        fillGardel()

        tapCreate()

        checkSnackbar(R.string.snackbar_prize_created)
        waitForPrizes { rows -> rows.singleOrNull()?.selected == true }
        onView(withId(R.id.prizeEmpty)).check(matches(not(isDisplayed())))
    }

    @Test
    fun cancelarYLaX_vuelvenSinCrearNada() {
        val prizes = InMemoryPrizeRepository(listOf(grammy))
        openPrizeCreate(prizes)
        fillGardel()

        onView(withId(R.id.cancelButton)).perform(click())
        onView(toolbarTitle(R.string.prize_add_title)).check(matches(isDisplayed()))

        onView(withId(R.id.prizeCreateButton)).perform(scrollTo(), click())
        onView(withContentDescription(R.string.cd_close_without_saving)).perform(click())

        waitUntil { runCatching { onView(toolbarTitle(R.string.prize_add_title)).check(matches(isDisplayed())) }.isSuccess }
        assertTrue(prizes.created.isEmpty())
        assertEquals(listOf(grammy.prize.name), candidatePrizeNamesInOrder())
    }

    // Recorrido completo (T6)

    @Test
    fun elPremioCreado_conservaLaFechaElegida_ySeAgregaAlArtista() {
        val prizes = InMemoryPrizeRepository(listOf(grammy))
        usePrizeRepositories(InMemoryArtistDetailRepository(listOf(rubenDetail)), prizes)
        app.launch()
        openArtistDetail(ruben.name)
        openPrizeAdd()
        pickTodayAsPremiationDate()
        onView(withId(R.id.prizeCreateButton)).perform(scrollTo(), click())
        fillGardel()

        tapCreate()
        checkSnackbar(R.string.snackbar_prize_created)

        // La fecha elegida antes de crear el premio sigue en su campo
        onView(withId(R.id.premiationDateInput)).perform(scrollTo()).check(matches(withText(todayInField())))
        tapSavePrize()

        checkSnackbar(R.string.snackbar_prize_added)
        checkShowingArtist(ruben.name)
        // Se agregó el premio recién creado, con el id que devolvió la creación
        assertEquals(listOf(2_001), prizes.added.map { it.prizeId })
    }

    @Test
    fun atrasDeAndroid_vuelveSinCrear_yConservaElPremioElegidoAntes() {
        val prizes = InMemoryPrizeRepository(listOf(grammy, latinGrammy))
        usePrizeRepositories(InMemoryArtistDetailRepository(listOf(rubenDetail)), prizes)
        app.launch()
        openArtistDetail(ruben.name)
        openPrizeAdd()
        choosePrize(latinGrammy.prize.name)
        onView(withId(R.id.prizeCreateButton)).perform(scrollTo(), click())
        fillGardel()

        pressBack()

        onView(toolbarTitle(R.string.prize_add_title)).check(matches(isDisplayed()))
        onView(candidatePrizeRadio(latinGrammy.prize.name)).perform(scrollTo()).check(matches(isChecked()))
        assertEquals(listOf(grammy.prize.name, latinGrammy.prize.name), candidatePrizeNamesInOrder())
        assertTrue(prizes.created.isEmpty())
    }

    private companion object {
        const val NAME = "Premio Gardel"
        const val ORGANIZATION = "CAPIF"
        const val DESCRIPTION = "Reconoce a los mejores artistas de música en español."
    }
}
