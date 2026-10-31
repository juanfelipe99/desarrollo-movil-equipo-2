package com.equipo2.vinilos

import android.app.Application
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.equipo2.vinilos.data.network.imageHttpClient
import com.equipo2.vinilos.ui.thememode.applyThemeMode
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/** Crea los repositorios, aplica el tema guardado y configura la carga de portadas. */
class VinilosApplication : Application(), SingletonImageLoader.Factory {

    private val appScope = MainScope()

    override fun onCreate() {
        super.onCreate()
        // La app solo está en español, y así lo que dibuja Android, como el selector de fecha, tampoco sale en otro idioma
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("es"))
        container = AppContainer(this)
        applySavedThemeMode()
    }

    private fun applySavedThemeMode() {
        val themeMode = container.themeModeRepository.themeMode

        // Antes de la primera pantalla, para no mostrar un instante el tema equivocado
        applyThemeMode(runBlocking { themeMode.first() })

        appScope.launch { themeMode.collect { applyThemeMode(it) } }
    }

    /** Cargador de imágenes de toda la app, con el cliente de imageHttpClient. */
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        // Se crea una sola vez y al descargar la primera portada, no al abrir la app
        val client by lazy { imageHttpClient(context) }
        return ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { client })) }
            .build()
    }

    companion object {
        /** Repositorios de la app, que las pruebas pueden reemplazar por unos falsos. */
        lateinit var container: AppContainer
            @VisibleForTesting set
    }
}
