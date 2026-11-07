package com.equipo2.vinilos.hu03

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.support.OwnData
import com.equipo2.vinilos.support.VinilosRule
import com.equipo2.vinilos.support.toolbarTitle
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * E2E de HU03 con la API real: el listado trae los músicos y bandas, y los filtros los separan.
 * Se revisan un músico y una banda que crea la prueba, porque el resto de la API puede cambiar mientras corre.
 */
@RunWith(AndroidJUnit4::class)
class HU03ArtistListTest {

    @get:Rule
    val app = VinilosRule()

    private val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
    private val data = OwnData()
    private val musician = data.name("HU03 Músico")
    private val band = data.name("HU03 Banda")

    @Before
    fun createArtists() {
        data.musician(musician)
        data.band(band)
    }

    @After
    fun deleteArtists() = data.deleteAll()

    @Test
    fun todos_muestraMusicosYBandas_conElContadorDeLaLista() {
        app.launch()

        openArtists()

        onView(toolbarTitle(R.string.artist_list_title)).check(matches(isDisplayed()))
        showArtistRow(musician)
        showArtistRow(band)
        onView(withText(countText(shownArtists().size))).check(matches(isDisplayed()))
    }

    @Test
    fun musicos_muestraSoloMusicos() {
        app.launch()
        openArtists()

        onView(withId(R.id.filterMusicians)).perform(click())

        val shown = shownArtists()
        assertTrue("El filtro Músicos muestra bandas", shown.all { it.type == ArtistType.MUSICIAN })
        showArtistRow(musician)
        onView(artistRowName(band)).check(doesNotExist())
        onView(withText(countText(shown.size))).check(matches(isDisplayed()))
    }

    @Test
    fun bandas_muestraSoloBandas() {
        app.launch()
        openArtists()

        onView(withId(R.id.filterBands)).perform(click())

        val shown = shownArtists()
        assertTrue("El filtro Bandas muestra músicos", shown.all { it.type == ArtistType.BAND })
        showArtistRow(band)
        onView(artistRowName(musician)).check(doesNotExist())
        onView(withText(countText(shown.size))).check(matches(isDisplayed()))
    }

    private fun countText(count: Int) = resources.getQuantityString(R.plurals.artist_list_count, count, count)
}
