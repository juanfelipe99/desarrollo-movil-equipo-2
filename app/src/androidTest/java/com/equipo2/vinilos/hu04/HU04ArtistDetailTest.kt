package com.equipo2.vinilos.hu04

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.equipo2.vinilos.R
import com.equipo2.vinilos.hu03.openArtists
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
 * HU04 contra BackVynils: detalle de bandas y músicos, navegación entre ellos, contadores y «Ver más».
 * Cada prueba crea su banda, sus integrantes y un músico sin banda, y solo revisa esos datos,
 * porque el resto de la API puede cambiar mientras corre.
 */
@RunWith(AndroidJUnit4::class)
class HU04ArtistDetailTest {

    @get:Rule
    val app = VinilosRule()

    private val data = OwnData()
    private val band = data.name("HU04 Banda")
    private val members = listOf(data.name("HU04 Integrante"), data.name("HU04 Integrante"))
    private val soloist = data.name("HU04 Solista")
    private val soloistAlbum = data.name("HU04 Álbum")

    @Before
    fun createArtists() {
        val bandId = data.band(band)
        members.forEach { BackVynils.addMusicianToBand(bandId, data.musician(it)) }
        val soloistId = data.musician(soloist, LONG_DESCRIPTION)
        BackVynils.addAlbumToMusician(soloistId, data.album(soloistAlbum))
    }

    @After
    fun deleteArtists() = data.deleteAll()

    private fun openAsVisitor(name: String) {
        app.launch()
        openArtists()
        openArtist(name)
    }

    @Test
    fun banda_muestraSuFechaDeCreacion_susIntegrantes_yLosContadoresDeAlbumesYPremios() {
        openAsVisitor(band)

        onView(toolbarTitle(R.string.artist_detail_title)).check(matches(isDisplayed()))
        checkShowingArtist(band)
        onView(withId(R.id.artistTypeLabel)).check(matches(withText(R.string.artist_type_band)))
        onView(withText(R.string.artist_detail_creation_date_label)).check(matches(isDisplayed()))

        onView(withId(R.id.membersCount)).perform(scrollToWithBarsHidden())
            .check(matches(withText(countText(R.plurals.artist_detail_members_count, members.size))))
        members.forEach { onView(memberRow(it)).perform(scrollToWithBarsHidden()).check(matches(isDisplayed())) }

        onView(withId(R.id.albumsCount)).perform(scrollToWithBarsHidden())
            .check(matches(withText(countText(R.plurals.artist_detail_albums_count, 0))))
        onView(withId(R.id.prizesCount)).perform(scrollToWithBarsHidden())
            .check(matches(withText(countText(R.plurals.artist_detail_prizes_count, 0))))
    }

    @Test
    fun banda_alTocarUnIntegrante_muestraSuBanda_yDesdeAhiSeVuelveALaBanda() {
        val member = members.first()
        openAsVisitor(band)

        openMember(member)

        checkShowingArtist(member)
        onView(withId(R.id.artistTypeLabel)).check(matches(withText(R.string.artist_type_musician)))
        onView(withText(R.string.artist_detail_birth_date_label)).check(matches(isDisplayed()))
        onView(withText(band)).check(matches(isDisplayed()))

        openBandOfMusician()
        checkShowingArtist(band)

        // Atrás recorre el camino inverso: músico, banda y listado
        pressBack()
        checkShowingArtist(member)
        pressBack()
        checkShowingArtist(band)
        pressBack()
        onView(toolbarTitle(R.string.artist_list_title)).check(matches(isDisplayed()))
    }

    @Test
    fun alTocarUnAlbumDelCarrusel_abreSuDetalle_yAtrasVuelveAlArtista() {
        openAsVisitor(soloist)

        onView(allOf(withId(R.id.artistAlbumName), withText(soloistAlbum))).perform(scrollToWithBarsHidden(), click())

        onView(toolbarTitle(R.string.album_detail_title)).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.albumName), withText(soloistAlbum))).check(matches(isDisplayed()))
        pressBack()
        checkShowingArtist(soloist)
    }

    @Test
    fun musicoSinBanda_diceQueNoPerteneceAUnaBanda_yNoMuestraIntegrantes() {
        openAsVisitor(soloist)

        checkShowingArtist(soloist)
        onView(withText(R.string.artist_detail_no_band)).check(matches(isDisplayed()))
        onView(withId(R.id.membersSection)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.albumsCount)).perform(scrollToWithBarsHidden())
            .check(matches(withText(countText(R.plurals.artist_detail_albums_count, 1))))
    }

    @Test
    fun descripcionLarga_verMasMuestraElTextoCompleto() {
        openAsVisitor(soloist)

        onView(withId(R.id.artistDescriptionToggle)).perform(scrollToWithBarsHidden(), click())

        onView(withId(R.id.artistDescription)).check(matches(withText(LONG_DESCRIPTION)))
    }

    @Test
    fun laFlechaDeLaBarra_vuelveAlListado() {
        openAsVisitor(band)

        pressToolbarUp()

        onView(toolbarTitle(R.string.artist_list_title)).check(matches(isDisplayed()))
    }

    private companion object {
        // Más larga que el corte de 170 caracteres de ui/common/ExpandableText.kt, para que aparezca «Ver más»
        const val LONG_DESCRIPTION = "Músico creado por una prueba E2E con una descripción larga, que en el detalle " +
            "se corta con puntos suspensivos y se muestra completa al tocar Ver más, igual que la de cualquier artista de la API."
    }
}
