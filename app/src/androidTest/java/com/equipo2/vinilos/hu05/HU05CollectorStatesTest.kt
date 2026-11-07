package com.equipo2.vinilos.hu05

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
import androidx.test.platform.app.InstrumentationRegistry
import com.equipo2.vinilos.R
import com.equipo2.vinilos.support.VinilosRule
import com.equipo2.vinilos.support.toolbarTitle
import com.equipo2.vinilos.support.useCollectorRepository
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** UI de HU05 con un repositorio en memoria: listado con total, carga, vacío, error con reintento y apertura del detalle. */
@RunWith(AndroidJUnit4::class)
class HU05CollectorStatesTest {

    @get:Rule
    val app = VinilosRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val errorTitle = context.getString(R.string.error_load_title, context.getString(R.string.collector_list_object))

    private val manolo = testCollector(100, "Manolo Bellon", "manollo@caracol.com.co", albumCount = 3)
    private val jaime = testCollector(101, "Jaime Monsalve", "jmonsalve@rtvc.com.co", albumCount = 1)

    @Test
    fun conColeccionistas_muestraElTotal_yCadaFilaConNombreAlbumesYCorreo() {
        useCollectorRepository(InMemoryCollectorRepository(Result.success(listOf(manolo, jaime))))
        app.launch()

        openCollectors()

        onView(toolbarTitle(R.string.collector_list_title)).check(matches(isDisplayed()))
        onView(withText("2 coleccionistas")).check(matches(isDisplayed()))
        onView(collectorRowName("Manolo Bellon")).check(matches(isDisplayed()))
        onView(withText("3 álbumes en su colección")).check(matches(isDisplayed()))
        onView(withText("manollo@caracol.com.co")).check(matches(isDisplayed()))
        onView(collectorRowName("Jaime Monsalve")).check(matches(isDisplayed()))
        onView(withText("1 álbum en su colección")).check(matches(isDisplayed()))
        onView(withText("jmonsalve@rtvc.com.co")).check(matches(isDisplayed()))
    }

    @Test
    fun mientrasCarga_muestraElIndicadorDeCarga_sinTotalNiListado() {
        useCollectorRepository(InMemoryCollectorRepository().apply { neverAnswer = true })
        app.launch()

        openCollectors()

        onView(withId(R.id.collectorsLoading)).check(matches(isDisplayed()))
        onView(withText(R.string.collector_list_loading)).check(matches(isDisplayed()))
        onView(withId(R.id.collectorHeader)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.collectorList)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun sinColeccionistas_muestraNoHayColeccionistasRegistrados_conElTotalEnCero_sinReintentar() {
        useCollectorRepository(InMemoryCollectorRepository(Result.success(emptyList())))
        app.launch()

        openCollectors()

        onView(withText("No hay coleccionistas registrados")).check(matches(isDisplayed()))
        onView(withText("0 coleccionistas")).check(matches(isDisplayed()))
        onView(withId(R.id.stateRetry)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.collectorList)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun siFallaLaConexion_muestraElMensaje_yElBotonReintentar() {
        useCollectorRepository(InMemoryCollectorRepository(noConnection()))
        app.launch()

        openCollectors()

        onView(withText(errorTitle)).check(matches(isDisplayed()))
        onView(withText(R.string.error_network_body)).check(matches(isDisplayed()))
        onView(withId(R.id.stateRetry)).check(matches(isDisplayed()))
        onView(withId(R.id.collectorHeader)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.collectorList)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun reintentar_haceUnaNuevaPeticion_yMuestraLaLista() {
        val repository = InMemoryCollectorRepository(listOf(noConnection(), Result.success(listOf(manolo))))
        useCollectorRepository(repository)
        app.launch()
        openCollectors()
        onView(withText(errorTitle)).check(matches(isDisplayed()))
        assertEquals("La pantalla debe consultar una vez al abrirse", 1, repository.requests)

        onView(withId(R.id.stateRetry)).perform(click())

        assertEquals("Reintentar debe hacer una nueva petición", 2, repository.requests)
        onView(collectorRowName("Manolo Bellon")).check(matches(isDisplayed()))
        onView(withText("1 coleccionista")).check(matches(isDisplayed()))
        onView(withText(errorTitle)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun siElReintentoTambienFalla_sigueMostrandoElErrorConReintentar() {
        val repository = InMemoryCollectorRepository(noConnection())
        useCollectorRepository(repository)
        app.launch()
        openCollectors()

        onView(withId(R.id.stateRetry)).perform(click())

        assertEquals(2, repository.requests)
        onView(withText(errorTitle)).check(matches(isDisplayed()))
        onView(withId(R.id.stateRetry)).check(matches(isDisplayed()))
    }

    @Test
    fun tocarUnColeccionista_abreElDetalleProvisional_yAtrasVuelveAlListado() {
        useCollectorRepository(InMemoryCollectorRepository(Result.success(listOf(manolo, jaime))))
        app.launch()
        openCollectors()

        onView(collectorRowName("Jaime Monsalve")).perform(click())

        onView(toolbarTitle(R.string.collector_detail_title)).check(matches(isDisplayed()))
        onView(withText(R.string.coming_soon_title)).check(matches(isDisplayed()))
        onView(withText(R.string.collector_detail_soon_body)).check(matches(isDisplayed()))

        pressBack()

        onView(toolbarTitle(R.string.collector_list_title)).check(matches(isDisplayed()))
        onView(collectorRowName("Jaime Monsalve")).check(matches(isDisplayed()))
    }
}
