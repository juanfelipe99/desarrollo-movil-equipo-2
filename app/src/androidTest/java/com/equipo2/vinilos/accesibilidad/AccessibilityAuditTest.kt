package com.equipo2.vinilos.accesibilidad

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatDelegate
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isRoot
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
import com.equipo2.vinilos.support.waitForNightMode
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckPreset
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult.AccessibilityCheckResultType
import com.google.android.apps.common.testing.accessibility.framework.Parameters
import com.google.android.apps.common.testing.accessibility.framework.uielement.AccessibilityHierarchyAndroid
import com.google.android.apps.common.testing.accessibility.framework.utils.contrast.BitmapImage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale

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

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val report = File(instrumentation.targetContext.filesDir, "auditoria-accesibilidad.csv")
    private var theme = "claro"

    @Before
    fun onlyWhenAsked() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("auditoria") == "si")
    }

    @Test
    fun recorrido_temaClaro() {
        if (!report.exists()) report.writeText("tema;pantalla;tipo;revision;elemento;mensaje\n")
        tour(night = false)
    }

    @Test
    fun recorrido_temaOscuro() {
        if (!report.exists()) report.writeText("tema;pantalla;tipo;revision;elemento;mensaje\n")
        tour(night = true)
    }

    private fun tour(night: Boolean) {
        theme = if (night) "oscuro" else "claro"
        app.launch()
        val mode = if (night) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        instrumentation.runOnMainSync { AppCompatDelegate.setDefaultNightMode(mode) }
        waitForNightMode(mode)
        settle()
        audit("Selección de rol")

        enterAs(collector = true)
        settle()
        audit("HU01 · Catálogo de álbumes")
        openAlbum(ALBUM)
        settle()
        audit("HU02 · Detalle de álbum")
        onView(withId(R.id.addTrackButton)).perform(scrollToWithBarsHidden(), click())
        settle()
        audit("HU08 · Agregar track")
        pressBack()
        pressBack()
        onView(withId(R.id.createAlbumButton)).perform(click())
        settle()
        audit("HU07 · Crear álbum")
        pressBack()

        openTab(R.id.artistsGraph)
        settle()
        audit("HU03 · Listado de artistas")
        openArtist(MUSICIAN)
        settle()
        audit("HU04 · Detalle de músico")
        onView(withId(R.id.addPrizeButton)).perform(scrollToWithBarsHidden(), click())
        settle()
        audit("HU14 · Agregar premio")
        onView(withId(R.id.prizeCreateButton)).perform(click())
        settle()
        audit("HU13 · Crear premio")
        pressBack()
        pressBack()
        onView(withId(R.id.addAlbumButton)).perform(scrollToWithBarsHidden(), click())
        settle()
        audit("HU15 · Agregar álbum a un músico")
        pressBack()
        pressBack()
        openArtist(BAND)
        settle()
        audit("HU04 · Detalle de banda")
        onView(withId(R.id.addMemberButton)).perform(scrollToWithBarsHidden(), click())
        settle()
        audit("HU12 · Agregar integrante")
        pressBack()

        openTab(R.id.collectorsGraph)
        settle()
        audit("HU05 · Listado de coleccionistas")
        openCollector(COLLECTOR)
        settle()
        audit("HU06 · Detalle de coleccionista")
    }

    // Las imágenes y las listas llegan después de que la pantalla aparece, y el contraste se mide sobre lo que se ve
    private fun settle() {
        instrumentation.waitForIdleSync()
        Thread.sleep(SETTLE_MS)
    }

    /** Corre todas las revisiones sobre la pantalla actual, con una captura para las de contraste. */
    private fun audit(screen: String) {
        var lines = emptyList<String>()
        // Captura de la pantalla real y no de la vista, porque las portadas de Coil son bitmaps de hardware que un
        // Canvas de software no puede dibujar. La copia en ARGB_8888 deja leer los píxeles
        val screenshot = instrumentation.uiAutomation.takeScreenshot()
        val capture = screenshot.copy(Bitmap.Config.ARGB_8888, false)
        screenshot.recycle()
        onView(isRoot()).check { view, noView ->
            val root = (view ?: throw noView).rootView
            val hierarchy = AccessibilityHierarchyAndroid.newBuilder(root).build()
            val parameters = Parameters().apply { putScreenCapture(BitmapImage(capture)) }
            val checks = AccessibilityCheckPreset.getAccessibilityHierarchyChecksForPreset(AccessibilityCheckPreset.LATEST)
            lines = checks.flatMap { check -> check.runCheckOnHierarchy(hierarchy, null, parameters) }
                .filter { it.type == AccessibilityCheckResultType.ERROR || it.type == AccessibilityCheckResultType.WARNING }
                .map { result ->
                    val element = result.element
                    val name = element?.resourceName ?: element?.className?.toString() ?: ""
                    val message = result.getMessage(Locale.forLanguageTag("es")).toString().replace(';', ',').replace('\n', ' ')
                    listOf(theme, screen, result.type.name, result.sourceCheckClass.simpleName, name, message).joinToString(";")
                }
        }
        capture.recycle()
        report.appendText(lines.joinToString("") { "$it\n" })
        // Si una pantalla no tiene hallazgos igual queda en el informe
        if (lines.isEmpty()) report.appendText("$theme;$screen;OK;;;Sin hallazgos\n")
    }

    private companion object {
        const val ALBUM = "Buscando América"
        const val MUSICIAN = "Rubén Blades Bellido de Luna"
        const val BAND = "Queen"
        const val COLLECTOR = "Jaime Monsalve"
        const val SETTLE_MS = 2_500L
    }
}

