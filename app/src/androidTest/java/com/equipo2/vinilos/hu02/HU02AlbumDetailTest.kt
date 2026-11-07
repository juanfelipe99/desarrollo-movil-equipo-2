
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
import com.equipo2.vinilos.support.VinilosRule
import com.equipo2.vinilos.support.enterAs
import com.equipo2.vinilos.support.scrollToWithBarsHidden
import com.equipo2.vinilos.support.toolbarTitle
import com.equipo2.vinilos.support.useAlbumRepository
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HU02AlbumDetailTest {

    @get:Rule
    val app = VinilosRule()

    /** Abre como visitante el primero de candidates cuyo nombre no se repite en el catálogo. */
    private fun openAlbumAsVisitor(candidates: List<String> = BackVynils.albumNames()): String {
        val albums = BackVynils.albumNames()
        assertTrue("BackVynils no tiene álbumes", candidates.isNotEmpty())

        val albumName = candidates.first { candidate ->
            albums.count { it == candidate } == 1
        }

        app.launch()
        enterAs(collector = false)
        openAlbum(albumName)

        return albumName
    }

    private fun installFailingRepository(): FailingAlbumDetailRepository {
        val fake = FailingAlbumDetailRepository(VinilosApplication.container.albumRepository)
        useAlbumRepository(fake)
        return fake
    }

    @Test
    fun visitante_abreUnAlbum_yConsultaSuDetalle() {
        val albumName = openAlbumAsVisitor()

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
        val albums = BackVynils.albumNames()
        val (album, artist) = BackVynils.albumPerformers().first { (album, _) -> albums.count { it == album } == 1 }
        openAlbumAsVisitor(listOf(album))

        onView(allOf(withId(R.id.performerName), withText(artist))).perform(scrollToWithBarsHidden(), click())

        onView(toolbarTitle(R.string.artist_detail_title)).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.artistName), withText(artist))).check(matches(isDisplayed()))
        pressBack()
        onView(allOf(withId(R.id.albumName), withText(album))).check(matches(isDisplayed()))
    }

    @Test
    fun visitante_puedeRegresarAlCatalogoDesdeElDetalle() {
        val albumName = openAlbumAsVisitor()

        onView(allOf(withId(R.id.albumName), withText(albumName)))
            .check(matches(isDisplayed()))

        pressBack()

        onView(toolbarTitle(R.string.album_list_title))
            .check(matches(isDisplayed()))
    }

    @Test
    fun visitante_puedeExpandirLaDescripcionDelAlbum() {
        // Solo las descripciones de más de 170 caracteres tienen «Ver más»
        openAlbumAsVisitor(BackVynils.albumNamesWithDescriptionLongerThan(170))
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
}
