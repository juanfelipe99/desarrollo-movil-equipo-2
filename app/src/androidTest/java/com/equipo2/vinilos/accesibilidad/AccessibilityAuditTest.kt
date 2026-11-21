package com.equipo2.vinilos.accesibilidad

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.equipo2.vinilos.R
import com.equipo2.vinilos.hu01.openAlbum
import com.equipo2.vinilos.hu04.openArtist
import com.equipo2.vinilos.hu06.openCollector
import com.equipo2.vinilos.support.VinilosRule
import com.equipo2.vinilos.support.enterAs
import com.equipo2.vinilos.support.openTab
import com.equipo2.vinilos.support.scrollToWithBarsHidden
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Auditoría de accesibilidad con las mismas revisiones de Accessibility Scanner (Accessibility Test Framework).
 * Recorre las pantallas principales con datos de la API, en tema claro y oscuro, y guarda cada hallazgo en
 * files/auditoria-accesibilidad.csv de la app. No revisa nada ni falla: es un informe para la revisión de accesibilidad.
 *
 * Solo corre con el argumento auditoria=si, así no cambia la suite E2E ni la CI:
 * adb shell am instrument -w -e auditoria si -e class com.equipo2.vinilos.accesibilidad.AccessibilityAuditTest ...
 * No guarda nada en la API, solo abre las pantallas.
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityAuditTest {

    @get:Rule
    val app = VinilosRule()

    private val auditor = Auditor()

    @Before
    fun onlyWhenAsked() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("auditoria") == "si")
    }

    @Test
    fun recorrido_temaClaro() = tour(night = false)

    @Test
    fun recorrido_temaOscuro() = tour(night = true)

    private fun tour(night: Boolean) {
        app.launch()
        auditor.useTheme(night)
        auditor.audit("Selección de rol")

        enterAs(collector = true)
        auditor.settle()
        auditor.audit("HU01 · Catálogo de álbumes")
        openAlbum(ALBUM)
        auditor.settle()
        auditor.audit("HU02 · Detalle de álbum")
        onView(withId(R.id.addTrackButton)).perform(scrollToWithBarsHidden(), click())
        auditor.settle()
        auditor.audit("HU08 · Agregar track")
        pressBack()
        pressBack()
        onView(withId(R.id.createAlbumButton)).perform(click())
        auditor.settle()
        auditor.audit("HU07 · Crear álbum")
        pressBack()

        openTab(R.id.artistsGraph)
        auditor.settle()
        auditor.audit("HU03 · Listado de artistas")
        openArtist(MUSICIAN)
        auditor.settle()
        auditor.audit("HU04 · Detalle de músico")
        onView(withId(R.id.addPrizeButton)).perform(scrollToWithBarsHidden(), click())
        auditor.settle()
        auditor.audit("HU14 · Agregar premio")
        onView(withId(R.id.prizeCreateButton)).perform(click())
        auditor.settle()
        auditor.audit("HU13 · Crear premio")
        pressBack()
        pressBack()
        onView(withId(R.id.addAlbumButton)).perform(scrollToWithBarsHidden(), click())
        auditor.settle()
        auditor.audit("HU15 · Agregar álbum a un músico")
        pressBack()
        pressBack()
        openArtist(BAND)
        auditor.settle()
        auditor.audit("HU04 · Detalle de banda")
        onView(withId(R.id.addMemberButton)).perform(scrollToWithBarsHidden(), click())
        auditor.settle()
        auditor.audit("HU12 · Agregar integrante")
        pressBack()

        openTab(R.id.collectorsGraph)
        auditor.settle()
        auditor.audit("HU05 · Listado de coleccionistas")
        openCollector(COLLECTOR)
        auditor.settle()
        auditor.audit("HU06 · Detalle de coleccionista")
    }

    private companion object {
        const val ALBUM = "Buscando América"
        const val MUSICIAN = "Rubén Blades Bellido de Luna"
        const val BAND = "Queen"
        const val COLLECTOR = "Jaime Monsalve"
    }
}

