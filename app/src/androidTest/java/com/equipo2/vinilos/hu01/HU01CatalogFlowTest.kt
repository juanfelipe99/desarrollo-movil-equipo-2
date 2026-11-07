package com.equipo2.vinilos.hu01

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.equipo2.vinilos.R
import com.equipo2.vinilos.hu07.fillAlbumForm
import com.equipo2.vinilos.hu07.openCreateAlbum
import com.equipo2.vinilos.hu07.tapSave
import com.equipo2.vinilos.support.BackVynils
import com.equipo2.vinilos.support.VinilosRule
import com.equipo2.vinilos.support.enterAs
import com.equipo2.vinilos.support.toolbarTitle
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** E2E de HU01 contra BackVynils: consultar el catálogo, ver el álbum nuevo y abrir el detalle. */
@RunWith(AndroidJUnit4::class)
class HU01CatalogFlowTest {

    @get:Rule
    val app = VinilosRule()

    private val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources

    @After
    fun deleteTestAlbums() {
        BackVynils.deleteAlbumsStartingWith(HU01_ALBUM_PREFIX)
    }

    @Test
    fun visitante_veElCatalogoDeLaApi_yAbreElDetalleDeUnAlbum() {
        // Un álbum propio de esta ejecución: el resto del catálogo puede cambiar mientras corre la prueba
        val name = "$HU01_ALBUM_PREFIX ${System.currentTimeMillis()}"
        BackVynils.createAlbum(name)
        app.launch()

        enterAs(collector = false)

        val shown = shownAlbums()
        assertTrue("El catálogo no muestra el álbum creado en la API", shown.any { it.album.name == name })
        val count = resources.getQuantityString(R.plurals.album_list_count, shown.size, shown.size)
        onView(withText(count)).check(matches(isDisplayed()))

        openAlbum(name)

        onView(toolbarTitle(R.string.album_detail_title)).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.albumName), withText(name))).check(matches(isDisplayed()))

        pressBack()

        onView(toolbarTitle(R.string.album_list_title)).check(matches(isDisplayed()))
    }

    @Test
    fun coleccionista_creaUnAlbum_yAparecePrimeroComoNuevo_yAbreSuDetalle() {
        val name = "$HU01_ALBUM_PREFIX ${System.currentTimeMillis()}"
        app.launch()
        openCreateAlbum()
        fillAlbumForm(name)

        tapSave()

        onView(toolbarTitle(R.string.album_list_title)).check(matches(isDisplayed()))
        val first = shownAlbums().first()
        assertEquals(name, first.album.name)
        assertTrue(first.isNew)
        onView(allOf(withId(R.id.albumNewBadge), isDescendantOfA(albumCard(name)))).check(matches(isDisplayed()))

        openAlbum(name)

        onView(allOf(withId(R.id.albumName), withText(name))).check(matches(isDisplayed()))
    }
}
