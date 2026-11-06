package com.equipo2.vinilos.hu04

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.ArtistDetail
import com.equipo2.vinilos.data.model.ArtistPrize
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.hu03.openArtists
import com.equipo2.vinilos.hu03.testBand
import com.equipo2.vinilos.hu03.testMusician
import com.equipo2.vinilos.support.VinilosRule
import com.equipo2.vinilos.support.rotateScreen
import com.equipo2.vinilos.support.scrollToWithBarsHidden
import com.equipo2.vinilos.support.toolbarTitle
import com.equipo2.vinilos.support.useArtistRepository
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** UI de HU04 con un repositorio en memoria: secciones, vacíos, carga, error con reintento y navegación. */
@RunWith(AndroidJUnit4::class)
class HU04ArtistStatesTest {

    @get:Rule
    val app = VinilosRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val errorTitle = context.getString(R.string.error_load_title, context.getString(R.string.artist_detail_object))

    private val ruben = testMusician(100, "Rubén Blades Bellido de Luna", 1948)
    private val seisDelSolar = testBand(200, "Seis del Solar", 1982)
    private val queen = testBand(101, "Queen", 1970)

    private val rubenDetail = ArtistDetail(
        artist = ruben.copy(date = LocalDate.of(1948, 7, 16)),
        description = "Cantante, compositor y actor panameño.",
        band = seisDelSolar,
        albums = listOf(testAlbum(100, "Buscando América", 1984, "Salsa"), testAlbum(101, "Poeta del pueblo", 1984, "Salsa")),
        prizes = listOf(ArtistPrize(100, "Grammy Award", "National Academy of Recording Arts & Sciences", LocalDate.of(1978, 12, 10))),
    )
    private val seisDelSolarDetail = ArtistDetail(
        artist = seisDelSolar,
        description = "Banda que acompañó a Rubén Blades.",
        members = listOf(ruben),
    )
    private val queenDetail = ArtistDetail(artist = queen, description = "Banda británica de rock.")

    private fun openWith(repository: InMemoryArtistDetailRepository, artistName: String) {
        useArtistRepository(repository)
        app.launch()
        openArtists()
        openArtist(artistName)
    }

    @Test
    fun musico_muestraSuTipoFechaBandaAlbumesYPremios() {
        openWith(InMemoryArtistDetailRepository(listOf(rubenDetail, seisDelSolarDetail)), ruben.name)

        onView(toolbarTitle(R.string.artist_detail_title)).check(matches(isDisplayed()))
        checkShowingArtist(ruben.name)
        onView(withId(R.id.artistTypeLabel)).check(matches(withText(R.string.artist_type_musician)))
        onView(withText(R.string.artist_detail_birth_date_label)).check(matches(isDisplayed()))
        onView(withText("16 de julio de 1948")).check(matches(isDisplayed()))
        onView(withText(seisDelSolar.name)).check(matches(isDisplayed()))
        // Un músico no tiene integrantes
        onView(withId(R.id.membersSection)).check(matches(withEffectiveVisibility(Visibility.GONE)))

        onView(withId(R.id.albumsCount)).perform(scrollToWithBarsHidden())
            .check(matches(withText(countText(R.plurals.artist_detail_albums_count, 2))))
        // Se baja hasta el carrusel y no solo hasta su título: en pantallas pequeñas queda debajo del borde.
        // En un carrusel angosto el segundo álbum puede quedar cortado, por eso se revisa que esté en la lista
        onView(withId(R.id.albumsList)).perform(scrollToWithBarsHidden()).check(
            matches(
                allOf(
                    isDisplayed(),
                    hasDescendant(withText("Buscando América")),
                    hasDescendant(withText("Poeta del pueblo")),
                    hasDescendant(withText("1984 · Salsa")),
                ),
            ),
        )

        onView(withId(R.id.prizesCount)).perform(scrollToWithBarsHidden())
            .check(matches(withText(countText(R.plurals.artist_detail_prizes_count, 1))))
        // La fecha es lo último de la tarjeta: al verla, el nombre y la organización también se ven
        onView(withText("10 de diciembre de 1978")).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
        onView(withText("Grammy Award")).check(matches(isDisplayed()))
        onView(withText("National Academy of Recording Arts & Sciences")).check(matches(isDisplayed()))
    }

    @Test
    fun musico_alTocarSuBanda_abreElDetalleDeLaBanda_yAtrasVuelveAlMusico() {
        val repository = InMemoryArtistDetailRepository(listOf(rubenDetail, seisDelSolarDetail))
        openWith(repository, ruben.name)

        openBandOfMusician()

        checkShowingArtist(seisDelSolar.name)
        onView(withId(R.id.artistTypeLabel)).check(matches(withText(R.string.artist_type_band)))
        onView(withText(R.string.artist_detail_creation_date_label)).check(matches(isDisplayed()))
        assertEquals(listOf(100 to ArtistType.MUSICIAN, 200 to ArtistType.BAND), repository.detailRequests)

        pressBack()

        checkShowingArtist(ruben.name)
    }

    @Test
    fun banda_muestraSusIntegrantes_yAlTocarUnoAbreSuDetalle() {
        openWith(InMemoryArtistDetailRepository(listOf(rubenDetail, seisDelSolarDetail)), seisDelSolar.name)

        onView(withId(R.id.membersCount)).perform(scrollToWithBarsHidden())
            .check(matches(withText(countText(R.plurals.artist_detail_members_count, 1))))

        openMember(ruben.name)

        checkShowingArtist(ruben.name)
    }

    @Test
    fun bandaSinIntegrantesAlbumesNiPremios_muestraLosMensajesDeCadaSeccion() {
        openWith(InMemoryArtistDetailRepository(listOf(queenDetail)), queen.name)

        onView(withId(R.id.membersCount)).perform(scrollToWithBarsHidden())
            .check(matches(withText(countText(R.plurals.artist_detail_members_count, 0))))
        // El cuerpo es lo último de la tarjeta: al verlo, el título también se ve
        onView(withText(R.string.artist_detail_members_empty_body)).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
        onView(withText(R.string.artist_detail_members_empty_title)).check(matches(isDisplayed()))

        onView(withText(R.string.artist_detail_albums_empty_body)).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
        onView(withId(R.id.albumsCount)).check(matches(withText(countText(R.plurals.artist_detail_albums_count, 0))))

        onView(withText(R.string.artist_detail_prizes_empty_body)).perform(scrollToWithBarsHidden()).check(matches(isDisplayed()))
        onView(withId(R.id.prizesCount)).check(matches(withText(countText(R.plurals.artist_detail_prizes_count, 0))))
    }

    @Test
    fun musicoSinBanda_noMuestraLaFilaDeBanda() {
        openWith(InMemoryArtistDetailRepository(listOf(rubenDetail.copy(band = null))), ruben.name)

        checkShowingArtist(ruben.name)
        onView(withId(R.id.artistBandRow)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun descripcionLarga_seCorta_yVerMasLaMuestraCompleta() {
        val long = "Queen es una banda británica de rock formada en 1970 en Londres por el cantante Freddie Mercury, " +
            "el guitarrista Brian May, el baterista Roger Taylor y el bajista John Deacon, y aún se considera activa."
        openWith(InMemoryArtistDetailRepository(listOf(queenDetail.copy(description = long))), queen.name)

        onView(withId(R.id.artistDescriptionToggle)).perform(scrollToWithBarsHidden())
            .check(matches(withText(R.string.description_expand)))
            .perform(click())

        onView(withId(R.id.artistDescription)).check(matches(withText(long)))
        onView(withId(R.id.artistDescriptionToggle)).check(matches(withText(R.string.description_collapse)))
    }

    @Test
    fun mientrasCarga_muestraCargandoArtista() {
        openWith(InMemoryArtistDetailRepository(listOf(queenDetail)).apply { neverAnswer = true }, queen.name)

        onView(withText(R.string.artist_detail_loading)).check(matches(isDisplayed()))
        onView(withId(R.id.artistContent)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun sinConexion_muestraElError_yReintentarCargaElArtista() {
        val repository = InMemoryArtistDetailRepository(listOf(queenDetail)).apply { failuresBeforeAnswer = 1 }
        openWith(repository, queen.name)

        onView(withText(errorTitle)).check(matches(isDisplayed()))
        onView(withText(R.string.error_network_body)).check(matches(isDisplayed()))

        // En pantallas pequeñas el botón queda debajo del mensaje y hay que bajar
        onView(withId(R.id.stateRetry)).perform(scrollToWithBarsHidden(), click())

        checkShowingArtist(queen.name)
        assertEquals(2, repository.detailRequests.size)
    }

    @Test
    fun alGirarLaPantalla_conservaElDetalle_sinVolverAPedirlo() {
        val repository = InMemoryArtistDetailRepository(listOf(queenDetail))
        openWith(repository, queen.name)

        rotateScreen()

        checkShowingArtist(queen.name)
        assertEquals(1, repository.detailRequests.size)
    }

    @Test
    fun laFlechaDeLaBarra_vuelveAlListadoDeArtistas() {
        openWith(InMemoryArtistDetailRepository(listOf(queenDetail)), queen.name)

        pressToolbarUp()

        onView(toolbarTitle(R.string.artist_list_title)).check(matches(isDisplayed()))
    }
}
