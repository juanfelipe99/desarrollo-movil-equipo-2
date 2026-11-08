
package com.equipo2.vinilos.hu02

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.equipo2.vinilos.R
import com.equipo2.vinilos.VinilosApplication
import com.equipo2.vinilos.hu01.openAlbum
import com.equipo2.vinilos.support.BackVynils
import com.equipo2.vinilos.support.OwnData
import com.equipo2.vinilos.support.VinilosRule
import com.equipo2.vinilos.support.enterAs
import com.equipo2.vinilos.support.scrollToWithBarsHidden
import com.equipo2.vinilos.support.toolbarTitle
import com.equipo2.vinilos.support.useAlbumRepository
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HU02AlbumDetailTest {

    @get:Rule
    val app = VinilosRule()

    // Un álbum propio de cada prueba: el resto del catálogo puede cambiar mientras corre
    private val data = OwnData()
    private val albumName = data.name("HU02 Álbum")

    @After
    fun deleteAlbum() = data.deleteAll()

    /** Crea el álbum de la prueba, con una descripción que se corta con «Ver más», y lo abre como visitante. */
    private fun openAlbumAsVisitor() {
        data.album(albumName, LONG_DESCRIPTION)
        app.launch()
        enterAs(collector = false)
        openAlbum(albumName)
    }

    private fun installFailingRepository(): FailingAlbumDetailRepository {
        val fake = FailingAlbumDetailRepository(VinilosApplication.container.albumRepository)
        useAlbumRepository(fake)
        return fake
    }

    @Test
    fun visitante_abreUnAlbum_yConsultaSuDetalle() {
        openAlbumAsVisitor()

        onView(toolbarTitle(R.string.album_detail_title))
            .check(matches(isDisplayed()))

        onView(allOf(withId(R.id.albumName), withText(albumName)))
            .check(matches(isDisplayed()))

        onView(withId(R.id.albumDescription))
            .perform(scrollToWithBarsHidden())
            .check(matches(isDisplayed()))

        onView(withId(R.id.tracksCount))
            .perform(scrollToWithBarsHidden())
            .check(matches(isDisplayed()))

        onView(withId(R.id.commentsCount))
            .perform(scrollToWithBarsHidden())
            .check(matches(isDisplayed()))
    }

    @Test
    fun alTocarUnInterprete_abreElDetalleDelArtista_yAtrasVuelveAlAlbum() {
        val artist = data.name("HU02 Intérprete")
        val musicianId = data.musician(artist)
        data.album(albumName, LONG_DESCRIPTION).also { BackVynils.addAlbumToMusician(musicianId, it) }
        app.launch()
        enterAs(collector = false)
        openAlbum(albumName)

        onView(allOf(withId(R.id.performerName), withText(artist))).perform(scrollToWithBarsHidden(), click())

        onView(toolbarTitle(R.string.artist_detail_title)).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.artistName), withText(artist))).check(matches(isDisplayed()))
        pressBack()
        onView(allOf(withId(R.id.albumName), withText(albumName))).check(matches(isDisplayed()))
    }

    @Test
    fun visitante_puedeRegresarAlCatalogoDesdeElDetalle() {
        openAlbumAsVisitor()

        onView(allOf(withId(R.id.albumName), withText(albumName)))
            .check(matches(isDisplayed()))

        pressBack()

        onView(toolbarTitle(R.string.album_list_title))
            .check(matches(isDisplayed()))
    }

    @Test
    fun visitante_puedeExpandirLaDescripcionDelAlbum() {
        openAlbumAsVisitor()
        expandAlbumDescription()
    }

    @Test
    fun visitante_puedeConsultarLasSeccionesDeTracksYComentarios() {
        openAlbumAsVisitor()
        verifyTracksAndCommentsSections()
    }

    @Test
    fun visitante_veErrorCuandoFallaLaConexionAlConsultarDetalle() {
        val fake = installFailingRepository()
        openAlbumAsVisitor()

        onView(withId(R.id.albumError))
            .check(matches(isDisplayed()))

        assertTrue(
            "Se esperaba al menos una consulta fallida",
            fake.detailRequests >= 1
        )
    }

    @Test
    fun visitante_puedeReintentarConsultarElDetalleTrasUnError() {
        val fake = installFailingRepository()
        openAlbumAsVisitor()

        val requestsBeforeRetry = fake.detailRequests

        onView(withId(R.id.albumError))
            .check(matches(isDisplayed()))

        onView(withId(R.id.stateRetry))
            .check(matches(isDisplayed()))
            .perform(click())

        assertTrue(
            "Reintentar debe generar otra petición",
            fake.detailRequests > requestsBeforeRetry
        )
    }

    private companion object {
        // Más larga que el corte de 170 caracteres de ui/common/ExpandableText.kt, para que aparezca «Ver más»
        const val LONG_DESCRIPTION = "Álbum creado por una prueba E2E con una descripción larga, que en el detalle " +
            "se corta con puntos suspensivos y se muestra completa al tocar Ver más, igual que la de cualquier álbum de la API."
    }
}
