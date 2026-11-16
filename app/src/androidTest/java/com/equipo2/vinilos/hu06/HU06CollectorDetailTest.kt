package com.equipo2.vinilos.hu06

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.equipo2.vinilos.R
import com.equipo2.vinilos.hu04.countText
import com.equipo2.vinilos.hu04.pressToolbarUp
import com.equipo2.vinilos.hu05.openCollectors
import com.equipo2.vinilos.support.BackVynils
import com.equipo2.vinilos.support.OwnData
import com.equipo2.vinilos.support.VinilosRule
import com.equipo2.vinilos.support.scrollToWithBarsHidden
import com.equipo2.vinilos.support.toolbarTitle
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * HU06 contra BackVynils: detalle de un coleccionista con su contacto, favoritos, colección y comentarios,
 * y el paso del comentario al detalle del álbum. Cada prueba crea su coleccionista, su músico favorito y sus álbumes,
 * y solo revisa esos datos, porque el resto de la API puede cambiar mientras corre.
 */
@RunWith(AndroidJUnit4::class)
class HU06CollectorDetailTest {

    @get:Rule
    val app = VinilosRule()

    private val data = OwnData()
    private val collector = data.name("HU06 Coleccionista")
    private val emptyCollector = data.name("HU06 Sin datos")
    private val favorite = data.name("HU06 Favorito")
    private val activeAlbum = data.name("HU06 Álbum activo")
    private val inactiveAlbum = data.name("HU06 Álbum inactivo")

    @Before
    fun createCollector() {
        val collectorId = data.collector(collector)
        data.collector(emptyCollector)
        BackVynils.addFavoriteMusician(collectorId, data.musician(favorite))
        val activeId = data.album(activeAlbum)
        BackVynils.addAlbumToCollector(collectorId, activeId, price = 25, status = "Active")
        BackVynils.addAlbumToCollector(collectorId, data.album(inactiveAlbum), price = 40, status = "Inactive")
        data.comment(activeId, collectorId, COMMENT, rating = 4)
    }

    @After
    fun deleteCollector() = data.deleteAll()

    private fun openAsVisitor(name: String) {
        app.launch()
        openCollectors()
        openCollector(name)
    }

    @Test
    fun detalle_muestraContacto_favoritos_coleccionConPrecioYEstado_yComentario() {
        openAsVisitor(collector)

        onView(toolbarTitle(R.string.collector_detail_title)).check(matches(isDisplayed()))
        checkShowingCollector(collector)
        onView(withId(R.id.collectorSubtitle)).check(matches(withText(countText(R.plurals.collector_detail_subtitle, 2))))
        onView(withText(BackVynils.TEST_TELEPHONE)).check(matches(isDisplayed()))
        onView(withText(BackVynils.TEST_EMAIL)).check(matches(isDisplayed()))

        onView(withId(R.id.favoritesList)).perform(scrollToWithBarsHidden()).check(matches(hasDescendant(withText(favorite))))

        onView(withId(R.id.collectorAlbumsCount)).perform(scrollToWithBarsHidden())
            .check(matches(withText(countText(R.plurals.collector_detail_albums_count, 2))))
        checkCollectorAlbumRow(activeAlbum, "Precio: $25", R.string.collector_detail_status_active)
        checkCollectorAlbumRow(inactiveAlbum, "Precio: $40", R.string.collector_detail_status_inactive)

        onView(commentAlbumLink(activeAlbum)).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
        onView(withText("4/5")).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
        onView(withText("“$COMMENT”")).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
    }

    @Test
    fun alTocarElAlbumDelComentario_abreSuDetalle_yAtrasVuelveAlColeccionista() {
        openAsVisitor(collector)

        openCommentAlbum(activeAlbum)

        onView(toolbarTitle(R.string.album_detail_title)).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.albumName), withText(activeAlbum))).check(matches(isDisplayed()))
        pressBack()
        checkShowingCollector(collector)
    }

    @Test
    fun coleccionistaSinFavoritosAlbumesNiComentarios_cadaSeccionLoDice() {
        openAsVisitor(emptyCollector)

        checkShowingCollector(emptyCollector)
        onView(withText(R.string.collector_detail_favorites_empty_body)).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
        onView(withText(R.string.collector_detail_albums_empty_body)).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
        onView(withText(R.string.collector_detail_comments_empty_body)).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
    }

    @Test
    fun laFlechaDeLaBarra_vuelveAlListado() {
        openAsVisitor(collector)

        pressToolbarUp()

        onView(toolbarTitle(R.string.collector_list_title)).check(matches(isDisplayed()))
    }

    private companion object {
        const val COMMENT = "Comentario creado por una prueba E2E."
    }
}
