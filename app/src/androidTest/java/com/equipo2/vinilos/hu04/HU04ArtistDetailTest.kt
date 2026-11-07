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
import com.equipo2.vinilos.support.VinilosRule
import com.equipo2.vinilos.support.scrollToWithBarsHidden
import com.equipo2.vinilos.support.toolbarTitle
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** HU04 contra BackVynils: detalle de bandas y músicos, navegación entre ellos, contadores y «Ver más». */
@RunWith(AndroidJUnit4::class)
class HU04ArtistDetailTest {

    @get:Rule
    val app = VinilosRule()

    /** Nombres que aparecen una sola vez en el listado, para tocar la fila correcta. */
    private val uniqueNames by lazy {
        (BackVynils.musicianNames() + BackVynils.bandNames()).groupingBy { it }.eachCount().filterValues { it == 1 }.keys
    }

    private fun bandWithMembers(): Pair<BackVynils.ArtistSummary, List<String>> {
        val bands = BackVynils.bandMembers().filter { (band, members) -> band.name in uniqueNames && members.all { it in uniqueNames } }
        assertTrue("BackVynils no tiene bandas con integrantes", bands.isNotEmpty())
        return bands.entries.first().toPair()
    }

    private fun openAsVisitor(name: String) {
        app.launch()
        openArtists()
        openArtist(name)
    }

    @Test
    fun banda_muestraSuFechaDeCreacion_susIntegrantes_yLosContadoresDeAlbumesYPremios() {
        val (band, members) = bandWithMembers()

        openAsVisitor(band.name)

        onView(toolbarTitle(R.string.artist_detail_title)).check(matches(isDisplayed()))
        checkShowingArtist(band.name)
        onView(withId(R.id.artistTypeLabel)).check(matches(withText(R.string.artist_type_band)))
        onView(withText(R.string.artist_detail_creation_date_label)).check(matches(isDisplayed()))

        onView(withId(R.id.membersCount)).perform(scrollToWithBarsHidden())
            .check(matches(withText(countText(R.plurals.artist_detail_members_count, members.size))))
        members.forEach { onView(memberRow(it)).perform(scrollToWithBarsHidden()).check(matches(isDisplayed())) }

        onView(withId(R.id.albumsCount)).perform(scrollToWithBarsHidden())
            .check(matches(withText(countText(R.plurals.artist_detail_albums_count, band.albums))))
        onView(withId(R.id.prizesCount)).perform(scrollToWithBarsHidden())
            .check(matches(withText(countText(R.plurals.artist_detail_prizes_count, band.prizes))))
    }

    @Test
    fun banda_alTocarUnIntegrante_muestraSuBanda_yDesdeAhiSeVuelveALaBanda() {
        val (band, members) = bandWithMembers()
        val member = members.first()
        openAsVisitor(band.name)

        openMember(member)

        checkShowingArtist(member)
        onView(withId(R.id.artistTypeLabel)).check(matches(withText(R.string.artist_type_musician)))
        onView(withText(R.string.artist_detail_birth_date_label)).check(matches(isDisplayed()))
        onView(withText(band.name)).check(matches(isDisplayed()))

        openBandOfMusician()
        checkShowingArtist(band.name)

        // Atrás recorre el camino inverso: músico, banda y listado
        pressBack()
        checkShowingArtist(member)
        pressBack()
        checkShowingArtist(band.name)
        pressBack()
        onView(toolbarTitle(R.string.artist_list_title)).check(matches(isDisplayed()))
    }

    @Test
    fun alTocarUnAlbumDelCarrusel_abreSuDetalle_yAtrasVuelveAlArtista() {
        val albums = BackVynils.albumNames()
        val (album, artist) = BackVynils.albumPerformers()
            .first { (album, artist) -> albums.count { it == album } == 1 && artist in uniqueNames }
        openAsVisitor(artist)

        onView(allOf(withId(R.id.artistAlbumName), withText(album))).perform(scrollToWithBarsHidden(), click())

        onView(toolbarTitle(R.string.album_detail_title)).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.albumName), withText(album))).check(matches(isDisplayed()))
        pressBack()
        checkShowingArtist(artist)
    }

    @Test
    fun musicoSinBanda_diceQueNoPerteneceAUnaBanda_yNoMuestraIntegrantes() {
        val musician = BackVynils.musiciansWithoutBand().firstOrNull { it.name in uniqueNames }
        assertTrue("BackVynils no tiene músicos sin banda", musician != null)

        openAsVisitor(musician!!.name)

        checkShowingArtist(musician.name)
        onView(withText(R.string.artist_detail_no_band)).check(matches(isDisplayed()))
        onView(withId(R.id.membersSection)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.albumsCount)).perform(scrollToWithBarsHidden())
            .check(matches(withText(countText(R.plurals.artist_detail_albums_count, musician.albums))))
    }

    @Test
    fun descripcionLarga_verMasMuestraElTextoCompleto() {
        val artist = BackVynils.artistsWithDescriptionLongerThan(DESCRIPTION_PREVIEW_LENGTH).firstOrNull { it.name in uniqueNames }
        assertTrue("BackVynils no tiene artistas con descripción larga", artist != null)
        openAsVisitor(artist!!.name)

        onView(withId(R.id.artistDescriptionToggle)).perform(scrollToWithBarsHidden(), click())

        onView(withId(R.id.artistDescription)).check(matches(withText(artist.description)))
    }

    @Test
    fun laFlechaDeLaBarra_vuelveAlListado() {
        val (band, _) = bandWithMembers()
        openAsVisitor(band.name)

        pressToolbarUp()

        onView(toolbarTitle(R.string.artist_list_title)).check(matches(isDisplayed()))
    }

    private companion object {
        // Mismo corte que la app en ui/common/ExpandableText.kt
        const val DESCRIPTION_PREVIEW_LENGTH = 170
    }
}
