package com.equipo2.vinilos

import android.app.Application
import androidx.annotation.VisibleForTesting
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

/** Arranque de la app: crea los repositorios, aplica el tema guardado y configura la carga de portadas. */
class VinilosApplication : Application(), SingletonImageLoader.Factory {

    private val appScope = MainScope()

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        applySavedThemeMode()
    }

    private fun applySavedThemeMode() {
        val themeMode = container.themeModeRepository.themeMode

        // Antes de la primera pantalla, para no mostrar un instante el tema equivocado
        applyThemeMode(runBlocking { themeMode.first() })

        // Después se aplica cada vez que se guarda uno nuevo
        appScope.launch { themeMode.collect { applyThemeMode(it) } }
    }

    /** Coil usa este cargador en toda la app, con el cliente que acepta Let's Encrypt en Android viejos. */
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
