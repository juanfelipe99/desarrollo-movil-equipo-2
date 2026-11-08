package com.equipo2.vinilos.support

import android.app.UiAutomation
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import androidx.test.platform.app.InstrumentationRegistry
import com.equipo2.vinilos.MainActivity
import com.equipo2.vinilos.VinilosApplication
import com.equipo2.vinilos.data.model.ThemeMode
import kotlinx.coroutines.runBlocking
import org.junit.rules.ExternalResource
import java.util.concurrent.TimeUnit

/** Prepara cada E2E y al terminar deja el dispositivo y la app como estaban. */
class VinilosRule : ExternalResource() {

    private val originalContainer = VinilosApplication.container
    private val apiIdling = ApiIdlingResource(originalContainer.apiClient.httpClient.dispatcher)
    private var scenario: ActivityScenario<MainActivity>? = null
    private val originalAnimationScales = mutableMapOf<String, String>()

    override fun before() {
        // Con animaciones, Espresso puede tocar una vista que todavía se mueve
        ANIMATION_SCALES.forEach { scale ->
            originalAnimationScales[scale] = shell("settings get global $scale")
            shell("settings put global $scale 0")
        }

        // El servidor puede tardar en despertar más de lo que Espresso espera por defecto
        IdlingPolicies.setIdlingResourceTimeout(2, TimeUnit.MINUTES)
        IdlingPolicies.setMasterPolicyTimeout(2, TimeUnit.MINUTES)
        IdlingRegistry.getInstance().register(apiIdling)
        BackVynils.wakeUp()
        // Cada prueba parte sin datos guardados de la anterior
        originalContainer.albumRepository.clearCache()
        originalContainer.artistRepository.clearCache()
        originalContainer.collectorRepository.clearCache()
    }

    override fun after() {
        scenario?.close()
        IdlingRegistry.getInstance().unregister(apiIdling)
        VinilosApplication.container = originalContainer
        runBlocking { originalContainer.themeModeRepository.setThemeMode(ThemeMode.SYSTEM) }
        // Una escala que nunca se cambió se lee como null y vale 1.
        // Es solo limpieza, así que si el dispositivo no la acepta no hace fallar una prueba que ya pasó
        originalAnimationScales.forEach { (scale, value) ->
            runCatching { shell("settings put global $scale ${value.toFloatOrNull() ?: 1f}") }
        }
    }

    /** Abre la app, que empieza en la selección de rol. */
    fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    /** Estado de la actividad, por ejemplo para ver si Atrás cerró la app. */
    val state: Lifecycle.State
        get() = checkNotNull(scenario).state

    /**
     * Ejecuta un comando de shell con UiAutomation.
     * Algunos Samsung cortan esa conexión cuando cambia la accesibilidad del sistema. Si pasa, se pide una conexión
     * nueva con otras opciones, que obliga a Android a crearla de nuevo, y se repite el comando.
     */
    private fun shell(command: String): String {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val output = try {
            instrumentation.uiAutomation.executeShellCommand(command)
        } catch (e: IllegalStateException) {
            // Pedir la conexión con opciones solo existe desde Android 7.0
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) throw e
            instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).executeShellCommand(command)
        }
        return ParcelFileDescriptor.AutoCloseInputStream(output).bufferedReader().use { it.readText().trim() }
    }

    private companion object {
        val ANIMATION_SCALES = listOf("window_animation_scale", "transition_animation_scale", "animator_duration_scale")
    }
}
