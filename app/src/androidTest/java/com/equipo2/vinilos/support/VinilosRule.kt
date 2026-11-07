package com.equipo2.vinilos.support

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
    }

    override fun after() {
        scenario?.close()
        IdlingRegistry.getInstance().unregister(apiIdling)
        VinilosApplication.container = originalContainer
        runBlocking { originalContainer.themeModeRepository.setThemeMode(ThemeMode.SYSTEM) }
        // Una escala que nunca se cambió se lee como null y vale 1
        originalAnimationScales.forEach { (scale, value) -> shell("settings put global $scale ${value.toFloatOrNull() ?: 1f}") }
    }

    /** Abre la app, que empieza en la selección de rol. */
    fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    /** Estado de la actividad, por ejemplo para ver si Atrás cerró la app. */
    val state: Lifecycle.State
        get() = checkNotNull(scenario).state

    private fun shell(command: String): String {
        val output = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(output).bufferedReader().use { it.readText().trim() }
    }

    private companion object {
        val ANIMATION_SCALES = listOf("window_animation_scale", "transition_animation_scale", "animator_duration_scale")
    }
}
