package com.equipo2.vinilos.hu06

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isSelected
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.CollectorComment
import com.equipo2.vinilos.hu01.CatalogAlbumRepository
import com.equipo2.vinilos.hu01.testAlbum
import com.equipo2.vinilos.hu03.InMemoryArtistRepository
import com.equipo2.vinilos.hu03.testBand
import com.equipo2.vinilos.hu03.testMusician
import com.equipo2.vinilos.hu04.countText
import com.equipo2.vinilos.hu04.pressToolbarUp
import com.equipo2.vinilos.hu05.openCollectors
import com.equipo2.vinilos.support.VinilosRule
import com.equipo2.vinilos.support.openTab
import com.equipo2.vinilos.support.rotateScreen
import com.equipo2.vinilos.support.scrollToWithBarsHidden
import com.equipo2.vinilos.support.toolbarTitle
import com.equipo2.vinilos.support.useTabRepositories
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** UI de HU06 con repositorios en memoria: secciones, vacíos, carga, error con reintento y navegación. */
@RunWith(AndroidJUnit4::class)
class HU06CollectorStatesTest {

    @get:Rule
    val app = VinilosRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val errorTitle = context.getString(R.string.error_load_title, context.getString(R.string.collector_detail_object))

    private val opera = testAlbum(102, "A Night at the Opera", 1975, "Rock")
    private val races = testAlbum(103, "A Day at the Races", 1976, "Rock")

    private val jaime = testCollectorDetail(
        id = 101,
        name = "Jaime Monsalve",
        telephone = "3012357936",
        email = "jmonsalve@rtvc.com.co",
        favorites = listOf(testBand(201, "Queen", 1970), testMusician(202, "Rubén Blades", 1948)),
        albums = listOf(collectorAlbum(103, races.name, 40, active = false), collectorAlbum(101, "Poeta del pueblo", 25)),
        comments = listOf(CollectorComment(101, opera.id, opera.name, 4, "I love this album of Queen")),
    )
    private val sinDatos = testCollectorDetail(id = 102, name = "Lucía Jaramillo", telephone = "", email = "lucia@ejemplo.com")

    private fun openWith(repository: InMemoryCollectorDetailRepository, name: String) {
        val albums = CatalogAlbumRepository(Result.success(listOf(opera, races)))
        // El detalle del álbum sale del último catálogo consultado
        runBlocking { albums.getAlbums() }
        useTabRepositories(albums = albums, artists = InMemoryArtistRepository(Result.success(emptyList())), collectors = repository)
        app.launch()
        openCollectors()
        openCollector(name)
    }

    @Test
    fun detalle_muestraIniciales_nombre_cantidadDeAlbumes_yContacto() {
        openWith(InMemoryCollectorDetailRepository(listOf(jaime, sinDatos)), jaime.collector.name)

        onView(toolbarTitle(R.string.collector_detail_title)).check(matches(isDisplayed()))
        checkShowingCollector("Jaime Monsalve")
        onView(withId(R.id.collectorInitials)).check(matches(withText("JM")))
        onView(withId(R.id.collectorSubtitle)).check(matches(withText(countText(R.plurals.collector_detail_subtitle, 2))))
        onView(withText(R.string.collector_detail_phone_label)).check(matches(isDisplayed()))
        onView(withText("3012357936")).check(matches(isDisplayed()))
        onView(withText(R.string.collector_detail_email_label)).check(matches(isDisplayed()))
        onView(withText("jmonsalve@rtvc.com.co")).check(matches(isDisplayed()))
    }

    @Test
    fun detalle_muestraFavoritos_coleccionConPrecioYEstado_yComentariosConCalificacion() {
        openWith(InMemoryCollectorDetailRepository(listOf(jaime)), jaime.collector.name)

        onView(withId(R.id.favoritesList)).perform(scrollToWithBarsHidden())
            .check(matches(allOf(hasDescendant(withText("Queen")), hasDescendant(withText("Rubén Blades")))))
        // Sin foto se ven las iniciales
        onView(withId(R.id.favoritesList)).check(matches(hasDescendant(withText("Q"))))

        onView(withId(R.id.collectorAlbumsCount)).perform(scrollToWithBarsHidden())
            .check(matches(withText(countText(R.plurals.collector_detail_albums_count, 2))))
        checkCollectorAlbumRow("A Day at the Races", "Precio: $40", R.string.collector_detail_status_inactive)
        checkCollectorAlbumRow("Poeta del pueblo", "Precio: $25", R.string.collector_detail_status_active)

        onView(commentAlbumLink("A Night at the Opera")).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
        onView(withText("4/5")).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
        onView(withText("“I love this album of Queen”")).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
    }

    @Test
    fun sinFavoritosAlbumesNiComentarios_cadaSeccionLoDice_ySinTelefonoDiceSinRegistrar() {
        openWith(InMemoryCollectorDetailRepository(listOf(jaime, sinDatos)), sinDatos.collector.name)

        checkShowingCollector("Lucía Jaramillo")
        onView(withId(R.id.collectorSubtitle)).check(matches(withText(countText(R.plurals.collector_detail_subtitle, 0))))
        onView(withText(R.string.collector_detail_no_data)).check(matches(isDisplayed()))
        onView(withText(R.string.collector_detail_favorites_empty_body)).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
        onView(withText(R.string.collector_detail_albums_empty_body)).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
        onView(withId(R.id.collectorAlbumsCount)).check(matches(withText(countText(R.plurals.collector_detail_albums_count, 0))))
        onView(withText(R.string.collector_detail_comments_empty_body)).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
    }

    @Test
    fun mientrasCarga_muestraElTextoYElEsqueleto() {
        openWith(InMemoryCollectorDetailRepository(listOf(jaime)).apply { neverAnswer = true }, jaime.collector.name)

        onView(withId(R.id.collectorLoading)).check(matches(isDisplayed()))
        onView(withText(R.string.collector_detail_loading)).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.skeleton), isDisplayedInLoading())).check(matches(isDisplayed()))
        onView(withId(R.id.collectorContent)).check(matches(not(isDisplayed())))
    }

    @Test
    fun sinConexion_muestraElError_yReintentarCargaElColeccionista() {
        val repository = InMemoryCollectorDetailRepository(listOf(jaime)).apply { failuresBeforeAnswer = 1 }
        openWith(repository, jaime.collector.name)

        onView(withText(errorTitle)).check(matches(isDisplayed()))
        onView(withText(R.string.error_network_body)).check(matches(isDisplayed()))
        onView(withId(R.id.stateRetry)).perform(click())

        checkShowingCollector("Jaime Monsalve")
        assertEquals(listOf(101, 101), repository.detailRequests)
    }

    @Test
    fun siElReintentoTambienFalla_sigueMostrandoElErrorConReintentar() {
        val repository = InMemoryCollectorDetailRepository(listOf(jaime)).apply { failuresBeforeAnswer = 2 }
        openWith(repository, jaime.collector.name)

        onView(withId(R.id.stateRetry)).perform(click())

        onView(withText(errorTitle)).check(matches(isDisplayed()))
        onView(withId(R.id.stateRetry)).check(matches(isDisplayed()))
        assertEquals(2, repository.detailRequests.size)
    }

    @Test
    fun alTocarElAlbumDeUnComentario_abreSuDetalle_yAtrasVuelveAlColeccionista() {
        openWith(InMemoryCollectorDetailRepository(listOf(jaime)), jaime.collector.name)

        openCommentAlbum("A Night at the Opera")

        onView(toolbarTitle(R.string.album_detail_title)).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.albumName), withText("A Night at the Opera"))).check(matches(isDisplayed()))
        // Sigue en la pestaña Coleccionistas
        onView(withId(R.id.collectorsGraph)).check(matches(isSelected()))

        pressBack()
        checkShowingCollector("Jaime Monsalve")
    }

    @Test
    fun laFlechaDeLaBarra_vuelveAlListado() {
        openWith(InMemoryCollectorDetailRepository(listOf(jaime)), jaime.collector.name)

        pressToolbarUp()

        onView(toolbarTitle(R.string.collector_list_title)).check(matches(isDisplayed()))
    }

    @Test
    fun desdeElDetalle_laBarraInferiorLlevaAAlbumesYArtistas_yAlVolverSigueElDetalle() {
        openWith(InMemoryCollectorDetailRepository(listOf(jaime)), jaime.collector.name)

        openTab(R.id.albumsGraph)
        onView(toolbarTitle(R.string.album_list_title)).check(matches(isDisplayed()))
        openTab(R.id.artistsGraph)
        onView(toolbarTitle(R.string.artist_list_title)).check(matches(isDisplayed()))
        openTab(R.id.collectorsGraph)

        checkShowingCollector("Jaime Monsalve")
    }

    @Test
    fun alGirarLaPantalla_conservaElColeccionistaSinVolverAPedirlo() {
        val repository = InMemoryCollectorDetailRepository(listOf(jaime))
        openWith(repository, jaime.collector.name)
        checkShowingCollector("Jaime Monsalve")

        rotateScreen()

        checkShowingCollector("Jaime Monsalve")
        assertEquals(1, repository.detailRequests.size)
    }

    /** El esqueleto de carga, dentro del indicador de esta pantalla y no del listado que queda atrás. */
    private fun isDisplayedInLoading() = isDescendantOfA(withId(R.id.collectorLoading))
}
