package com.equipo2.vinilos.hu01

import android.app.Activity
import android.content.ContextWrapper
import android.view.View
import android.widget.ImageView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import coil3.SingletonImageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.Album
import com.equipo2.vinilos.data.model.NewAlbum
import com.equipo2.vinilos.data.repository.AlbumRepository
import com.equipo2.vinilos.data.repository.DataError
import com.equipo2.vinilos.hu07.fillAlbumForm
import com.equipo2.vinilos.hu07.openCreateAlbum
import com.equipo2.vinilos.hu07.tapSave
import com.equipo2.vinilos.support.BackVynils
import com.equipo2.vinilos.support.VinilosRule
import com.equipo2.vinilos.support.enterAs
import com.equipo2.vinilos.support.toolbarTitle
import kotlinx.coroutines.runBlocking
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.hamcrest.TypeSafeMatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

/** Casos límite del listado de álbumes (HU01): giro de pantalla, doble toque, posición del catálogo, nombres largos y portadas que no cargan. */
@RunWith(AndroidJUnit4::class)
class HU01CatalogEdgeCasesTest {

    @get:Rule
    val app = VinilosRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val manyAlbums = List(30) { i -> testAlbum(200 + i, "Álbum de prueba ${i + 1}", 1960 + i, "Rock") }

    @After
    fun deleteTestAlbums() {
        BackVynils.deleteAlbumsStartingWith(HU01_ALBUM_PREFIX)
    }

    @Test
    fun atrasDesdeElDetalle_conservaLaPosicionDelCatalogo() {
        useAlbumRepository(CountingAlbumRepository(manyAlbums))
        app.launch()
        enterAs(collector = false)

        openAlbum("Álbum de prueba 30")
        pressBack()

        onView(albumCardName("Álbum de prueba 30")).check(matches(isDisplayed()))
    }

    @Test
    fun girarElCatalogo_conservaLosAlbumes_sinPedirlosDeNuevo() {
        val repository = CountingAlbumRepository(manyAlbums.take(2))
        useAlbumRepository(repository)
        app.launch()
        enterAs(collector = false)

        rotateScreen()

        onView(albumCardName("Álbum de prueba 1")).check(matches(isDisplayed()))
        assertEquals("Pedidos del catálogo", 1, repository.albumsCalls.get())
    }

    @Test
    fun girarConError_sigueMostrandoElError_sinPedirDeNuevo() {
        val repository = CountingAlbumRepository(manyAlbums, failCatalog = true)
        useAlbumRepository(repository)
        app.launch()
        enterAs(collector = false)

        rotateScreen()

        onView(withText(errorTitle(R.string.album_list_object))).check(matches(isDisplayed()))
        assertEquals("Pedidos del catálogo", 1, repository.albumsCalls.get())
    }

    @Test
    fun dobleToqueRapidoEnUnAlbum_abreUnSoloDetalle() {
        useAlbumRepository(CountingAlbumRepository(manyAlbums.take(2)))
        app.launch()
        enterAs(collector = false)

        onView(albumCard("Álbum de prueba 1")).perform(clickTwiceFast())
        pressBack()

        onView(toolbarTitle(R.string.album_list_title)).check(matches(isDisplayed()))
    }

    @Test
    fun nombreMuyLargo_noTapaElAnioYGenero() {
        val longName = "Un nombre de álbum larguísimo ".repeat(6).trim()
        useAlbumRepository(CountingAlbumRepository(listOf(testAlbum(300, longName, 1984, "Salsa"))))
        app.launch()
        enterAs(collector = false)

        onView(albumCardName(longName)).check(matches(isDisplayed()))
        onView(allOf(withText("1984 · Salsa"), isDescendantOfA(albumCard(longName)))).check(matches(isDisplayed()))
    }

    @Test
    fun portadaQueNoCarga_muestraElMarcador_yLaTarjetaSigueFuncionando() {
        val album = testAlbum(400, "Portada rota", 1984, "Salsa").copy(cover = BROKEN_COVER_URL)
        useAlbumRepository(CountingAlbumRepository(listOf(album)))
        app.launch()
        enterAs(collector = false)

        // Coil carga fuera de la vista de Espresso: se confirma que esta portada falla antes de revisar la tarjeta
        assertTrue("La portada de prueba debería fallar", coverFails(BROKEN_COVER_URL))

        onView(allOf(withId(R.id.albumCover), isDescendantOfA(albumCard("Portada rota")))).check(matches(hasNoImage()))
        onView(coverPlaceholder("Portada rota")).check(matches(isDisplayed()))
        onView(albumCardName("Portada rota")).check(matches(isDisplayed()))
        onView(allOf(withText("1984 · Salsa"), isDescendantOfA(albumCard("Portada rota")))).check(matches(isDisplayed()))

        openAlbum("Portada rota")

        onView(toolbarTitle(R.string.album_detail_title)).check(matches(isDisplayed()))
    }

    /** E2E contra la API: el distintivo «Nuevo» no se pierde al ir al detalle y volver, ni al girar. */
    @Test
    fun albumNuevo_conservaElDistintivo_alVolverDelDetalle_yAlGirar() {
        val name = "$HU01_ALBUM_PREFIX ${System.currentTimeMillis()}"
        app.launch()
        openCreateAlbum()
        fillAlbumForm(name)
        tapSave()

        openAlbum(name)
        pressBack()
        rotateScreen()

        val first = shownAlbums().first()
        assertEquals(name, first.album.name)
        assertTrue("El álbum perdió el distintivo «Nuevo»", first.isNew)
        onView(allOf(withId(R.id.albumNewBadge), isDescendantOfA(albumCard(name)))).check(matches(isDisplayed()))
    }

    private fun errorTitle(objectRes: Int) = context.getString(R.string.error_load_title, context.getString(objectRes))
}

/** Repositorio en memoria que cuenta cuántas veces se pide el catálogo. */
private class CountingAlbumRepository(
    private val albums: List<Album>,
    private val failCatalog: Boolean = false,
) : AlbumRepository {

    val albumsCalls = AtomicInteger()

    override suspend fun getAlbums(): Result<List<Album>> {
        albumsCalls.incrementAndGet()
        return if (failCatalog) Result.failure(DataError.Network()) else Result.success(albums)
    }

    override suspend fun getAlbumById(id: Int): Result<Album> = Result.success(albums.first { it.id == id })

    override suspend fun createAlbum(album: NewAlbum): Result<Album> = Result.failure(UnsupportedOperationException())
}

/** Puerto cerrado en el propio dispositivo: la descarga falla al instante y sin depender de internet. */
private const val BROKEN_COVER_URL = "http://127.0.0.1:1/portada.jpg"

private fun coverFails(url: String): Boolean {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val request = ImageRequest.Builder(context).data(url).build()
    return runBlocking { SingletonImageLoader.get(context).execute(request) } is ErrorResult
}

/** Ícono de disco que queda detrás de la portada de la tarjeta. */
private fun coverPlaceholder(name: String): Matcher<View> = allOf(
    isAssignableFrom(ImageView::class.java),
    not(withId(R.id.albumCover)),
    isDescendantOfA(allOf(withId(R.id.albumCoverFrame), isDescendantOfA(albumCard(name)))),
)

/** Imagen sin contenido, como queda la portada cuando no se pudo descargar. */
private fun hasNoImage(): Matcher<View> = object : TypeSafeMatcher<View>() {
    override fun describeTo(description: Description) {
        description.appendText("imagen sin contenido")
    }

    override fun matchesSafely(view: View) = view is ImageView && view.drawable == null
}

/** Recrea la actividad como al girar la pantalla: el ViewModel sobrevive y las vistas se crean de nuevo. */
private fun rotateScreen() {
    onView(isRoot()).perform(object : ViewAction {
        override fun getConstraints(): Matcher<View> = isRoot()
        override fun getDescription() = "girar la pantalla"
        override fun perform(uiController: UiController, view: View) {
            var context = view.context
            while (context !is Activity) context = (context as ContextWrapper).baseContext
            context.recreate()
            uiController.loopMainThreadUntilIdle()
        }
    })
}

/** Dos toques seguidos sin esperar a que la app navegue, como un usuario impaciente. */
private fun clickTwiceFast() = object : ViewAction {
    override fun getConstraints(): Matcher<View> = isDisplayed()
    override fun getDescription() = "tocar dos veces muy rápido"
    override fun perform(uiController: UiController, view: View) {
        view.performClick()
        view.performClick()
        uiController.loopMainThreadUntilIdle()
    }
}
