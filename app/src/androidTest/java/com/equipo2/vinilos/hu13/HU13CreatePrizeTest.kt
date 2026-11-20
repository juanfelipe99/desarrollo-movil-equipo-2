package com.equipo2.vinilos.hu13

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.equipo2.vinilos.R
import com.equipo2.vinilos.hu04.checkShowingArtist
import com.equipo2.vinilos.hu14.candidatePrizeNamesInOrder
import com.equipo2.vinilos.hu14.candidatePrizeNewBadge
import com.equipo2.vinilos.hu14.candidatePrizeRadio
import com.equipo2.vinilos.hu14.newPrizeBadge
import com.equipo2.vinilos.hu14.openArtistDetail
import com.equipo2.vinilos.hu14.openPrizeAdd
import com.equipo2.vinilos.hu14.pickTodayAsPremiationDate
import com.equipo2.vinilos.hu14.tapSavePrize
import com.equipo2.vinilos.hu14.waitForPrizes
import com.equipo2.vinilos.support.BackVynils
import com.equipo2.vinilos.support.OwnData
import com.equipo2.vinilos.support.VinilosRule
import com.equipo2.vinilos.support.checkSnackbar
import com.equipo2.vinilos.support.scrollToWithBarsHidden
import com.equipo2.vinilos.support.toolbarTitle
import com.equipo2.vinilos.support.waitForSnackbarGone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * E2E de HU13 con la API: el premio que se crea queda en la API con lo escrito, vuelve a Agregar premio primero,
 * elegido y marcado, y se le puede entregar al artista en el mismo recorrido.
 *
 * Cada prueba crea su músico y el nombre de su premio, y solo revisa esos datos.
 */
@RunWith(AndroidJUnit4::class)
class HU13CreatePrizeTest {

    @get:Rule
    val app = VinilosRule()

    private val own = OwnData()

    private val musicianName = own.name("HU13 Músico")
    private val prizeName = own.name("HU13 Premio")

    private var musicianId = 0

    @Before
    fun createMusician() {
        musicianId = own.musician(musicianName)
    }

    // El premio lo crea la app, así que se busca por su nombre para borrarlo aunque la prueba falle a la mitad
    @After
    fun deleteTestData() {
        BackVynils.prizeNamed(prizeName)?.let { own.keepPrize(it.getInt("id")) }
        own.deleteAll()
    }

    @Test
    fun crearUnPremio_quedaEnLaApi_yVuelvePrimeroElegidoYMarcadoComoNuevo() {
        openPrizeCreate()

        fillPrize()
        onView(withId(R.id.saveButton)).perform(click())

        checkSnackbar(R.string.snackbar_prize_created)
        onView(toolbarTitle(R.string.prize_add_title)).check(matches(isDisplayed()))
        waitForPrizes { rows -> rows.firstOrNull()?.candidate?.prize?.name == prizeName }
        assertEquals(prizeName, candidatePrizeNamesInOrder().first())
        onView(candidatePrizeNewBadge(prizeName)).check(matches(isDisplayed()))
        onView(candidatePrizeRadio(prizeName)).check(matches(isChecked()))

        val saved = BackVynils.prizeNamed(prizeName)
        assertNotNull("El premio no quedó en la API", saved)
        assertEquals(ORGANIZATION, saved!!.getString("organization"))
        assertEquals(DESCRIPTION, saved.getString("description"))
    }

    @Test
    fun elPremioCreado_seEntregaAlArtista_yApareceEnSuDetalle() {
        openPrizeCreate()
        fillPrize()
        onView(withId(R.id.saveButton)).perform(click())
        checkSnackbar(R.string.snackbar_prize_created)
        waitForPrizes { rows -> rows.any { it.selected && it.candidate.prize.name == prizeName } }
        waitForSnackbarGone(R.string.snackbar_prize_created)

        pickTodayAsPremiationDate()
        tapSavePrize()

        checkSnackbar(R.string.snackbar_prize_added)
        checkShowingArtist(musicianName)
        onView(newPrizeBadge(prizeName)).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
        assertEquals(listOf(LocalDate.now().toString()), BackVynils.musicianPrizeDays(musicianId))
    }

    private fun openPrizeCreate() {
        app.launch()
        openArtistDetail(musicianName)
        openPrizeAdd()
        onView(withId(R.id.prizeCreateButton)).perform(scrollTo(), click())
        onView(toolbarTitle(R.string.prize_create_title)).check(matches(isDisplayed()))
    }

    // Con espacios al borde, que la app quita antes de enviar
    private fun fillPrize() {
        onView(withId(R.id.nameInput)).perform(replaceText("  $prizeName "))
        onView(withId(R.id.organizationInput)).perform(replaceText(ORGANIZATION))
        onView(withId(R.id.descriptionInput)).perform(replaceText(DESCRIPTION), closeSoftKeyboard())
    }

    private companion object {
        const val ORGANIZATION = "Academia E2E"
        const val DESCRIPTION = "Premio creado desde la app por una prueba E2E."
    }
}
