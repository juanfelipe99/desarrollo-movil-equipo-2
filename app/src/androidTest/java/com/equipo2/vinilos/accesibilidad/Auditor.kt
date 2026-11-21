package com.equipo2.vinilos.accesibilidad

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatDelegate
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.platform.app.InstrumentationRegistry
import com.equipo2.vinilos.support.waitForNightMode
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckPreset
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult.AccessibilityCheckResultType
import com.google.android.apps.common.testing.accessibility.framework.Parameters
import com.google.android.apps.common.testing.accessibility.framework.uielement.AccessibilityHierarchyAndroid
import com.google.android.apps.common.testing.accessibility.framework.utils.contrast.BitmapImage
import java.io.File
import java.util.Locale

/**
 * Corre las revisiones de Accessibility Scanner (Accessibility Test Framework) sobre la pantalla actual y guarda cada
 * hallazgo en files/auditoria-accesibilidad.csv de la app.
 * Con el argumento capturas=si guarda además la captura de cada pantalla en files/capturas, para revisar a ojo
 * la pantalla con la letra o el tamaño de pantalla del sistema al máximo.
 */
class Auditor {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val files = instrumentation.targetContext.filesDir
    private val report = File(files, "auditoria-accesibilidad.csv")
    private val captures = File(files, "capturas").takeIf { InstrumentationRegistry.getArguments().getString("capturas") == "si" }

    var theme = "claro"
        private set

    init {
        if (!report.exists()) report.writeText("tema;pantalla;tipo;revision;elemento;mensaje\n")
        captures?.mkdirs()
    }

    fun useTheme(night: Boolean) {
        theme = if (night) "oscuro" else "claro"
        val mode = if (night) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        instrumentation.runOnMainSync { AppCompatDelegate.setDefaultNightMode(mode) }
        waitForNightMode(mode)
        settle()
    }

    /**
     * Revisa la pantalla actual en tema claro y luego en oscuro, y la deja en claro.
     * Al cambiar de tema la actividad se recrea y el detalle vuelve arriba, por eso prepare se repite en cada tema,
     * por ejemplo para bajar otra vez hasta una sección.
     */
    fun auditBothThemes(screen: String, prepare: () -> Unit = {}) {
        useTheme(night = false)
        prepare()
        settle()
        audit(screen)
        useTheme(night = true)
        prepare()
        settle()
        audit(screen)
        useTheme(night = false)
    }

    // Las imágenes y las listas llegan después de que la pantalla aparece, y el contraste se mide sobre lo que se ve
    fun settle() {
        instrumentation.waitForIdleSync()
        Thread.sleep(SETTLE_MS)
    }

    /** Corre todas las revisiones sobre la pantalla actual, con una captura para las de contraste. */
    fun audit(screen: String) {
        var lines = emptyList<String>()
        // Captura de la pantalla real y no de la vista, porque las portadas de Coil son bitmaps de hardware que un
        // Canvas de software no puede dibujar. La copia en ARGB_8888 deja leer los píxeles
        val screenshot = instrumentation.uiAutomation.takeScreenshot()
        val capture = screenshot.copy(Bitmap.Config.ARGB_8888, false)
        screenshot.recycle()
        captures?.let { dir ->
            val name = "$theme-$screen".replace(Regex("[^\\p{L}\\p{N}]+"), "-").trim('-').lowercase(Locale.ROOT)
            File(dir, "%02d-%s.png".format(dir.list()?.size ?: 0, name)).outputStream().use {
                capture.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
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
        const val SETTLE_MS = 2_500L
    }
}
