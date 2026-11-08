package com.equipo2.vinilos.hu05

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isSelected
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.equipo2.vinilos.R
import com.equipo2.vinilos.hu01.CatalogAlbumRepository
import com.equipo2.vinilos.hu01.albumCardName
import com.equipo2.vinilos.hu01.testAlbum
import com.equipo2.vinilos.hu03.InMemoryArtistRepository
import com.equipo2.vinilos.hu03.artistRowName
import com.equipo2.vinilos.hu03.testBand
import com.equipo2.vinilos.support.VinilosRule
import com.equipo2.vinilos.support.enterAs
import com.equipo2.vinilos.support.toolbarTitle
import com.equipo2.vinilos.support.useTabRepositories
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Barra inferior de HU05 con repositorios en memoria: pasar entre Álbumes, Artistas y Coleccionistas. */
@RunWith(AndroidJUnit4::class)
class HU05CollectorNavigationTest {

    @get:Rule
    val app = VinilosRule()

    @Before
    fun useFakeTabs() {
        useTabRepositories(
            albums = CatalogAlbumRepository(Result.success(listOf(testAlbum(100, "Buscando América", 1984, "Salsa")))),
            artists = InMemoryArtistRepository(Result.success(listOf(testBand(101, "Queen", 1970)))),
            collectors = InMemoryCollectorRepository(
                Result.success(listOf(testCollector(100, "Manolo Bellon", "manollo@caracol.com.co", albumCount = 3))),
            ),
        )
    }

    @Test
    fun laBarraInferior_permitePasarEntreAlbumesArtistasYColeccionistas() {
        app.launch()
        enterAs(collector = false)

        onView(toolbarTitle(R.string.album_list_title)).check(matches(isDisplayed()))
        onView(albumCardName("Buscando América")).check(matches(isDisplayed()))
        onView(withId(R.id.albumsGraph)).check(matches(isSelected()))

        onView(withId(R.id.artistsGraph)).perform(click())
        onView(toolbarTitle(R.string.artist_list_title)).check(matches(isDisplayed()))
        onView(artistRowName("Queen")).check(matches(isDisplayed()))
        onView(withId(R.id.artistsGraph)).check(matches(isSelected()))
        onView(withId(R.id.albumsGraph)).check(matches(not(isSelected())))

        onView(withId(R.id.collectorsGraph)).perform(click())
        onView(toolbarTitle(R.string.collector_list_title)).check(matches(isDisplayed()))
        onView(collectorRowName("Manolo Bellon")).check(matches(isDisplayed()))
        onView(withId(R.id.collectorsGraph)).check(matches(isSelected()))
        onView(withId(R.id.artistsGraph)).check(matches(not(isSelected())))

        onView(withId(R.id.albumsGraph)).perform(click())
        onView(toolbarTitle(R.string.album_list_title)).check(matches(isDisplayed()))
        onView(withId(R.id.albumsGraph)).check(matches(isSelected()))
        onView(withId(R.id.collectorsGraph)).check(matches(not(isSelected())))
    }

    @Test
    fun irAAlbumesDesdeElDetalleDeUnColeccionista_yVolver_marcaColeccionistasYSigueEnElDetalle() {
        app.launch()
        openCollectors()
        onView(collectorRowName("Manolo Bellon")).perform(click())

        onView(withId(R.id.albumsGraph)).perform(click())
        onView(withId(R.id.collectorsGraph)).perform(click())

        onView(toolbarTitle(R.string.collector_detail_title)).check(matches(isDisplayed()))
        onView(withId(R.id.collectorsGraph)).check(matches(isSelected()))
        onView(withId(R.id.albumsGraph)).check(matches(not(isSelected())))
    }
}
