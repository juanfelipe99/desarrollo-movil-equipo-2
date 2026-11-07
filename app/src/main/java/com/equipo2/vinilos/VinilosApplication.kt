package com.equipo2.vinilos

import android.app.ActivityManager
import android.app.Application
import android.content.ComponentCallbacks2
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.getSystemService
import androidx.core.os.LocaleListCompat
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.allowRgb565
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

    /** Cargador de imágenes de toda la app, con el cliente de imageHttpClient y cachés en memoria y en disco. */
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        // Se crea una sola vez y al descargar la primera portada, no al abrir la app
        val client by lazy { imageHttpClient(context) }
        // En un teléfono con poca memoria cada portada ocupa la mitad y la caché en memoria es más chica
        val lowRam = getSystemService<ActivityManager>()?.isLowRamDevice == true
        return ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { client })) }
            // En el perfilamiento las portadas ocupaban la mayor parte de la memoria del catálogo. Con el 15 % caben unas tres
            // pantallas de portadas, y las que salen se vuelven a leer del disco sin descargarlas otra vez
            .memoryCache { MemoryCache.Builder().maxSizePercent(context, if (lowRam) 0.1 else 0.15).build() }
            .diskCache { DiskCache.Builder().directory(cacheDir.resolve("portadas")).maxSizeBytes(IMAGE_DISK_CACHE_BYTES).build() }
            .allowRgb565(lowRam)
            .build()
    }

    /** Si Android pide memoria se sueltan los datos guardados de la API. Coil ya libera solo sus imágenes. */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Pasar a segundo plano no cuenta, así al volver a la app no se repiten las peticiones
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW && level != ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            container.albumRepository.clearCache()
            container.artistRepository.clearCache()
            container.collectorRepository.clearCache()
        }
    }

    companion object {
        /** Espacio máximo de las portadas guardadas en disco. */
        private const val IMAGE_DISK_CACHE_BYTES = 50L * 1024 * 1024

        /** Repositorios de la app, que las pruebas pueden reemplazar por unos falsos. */
        lateinit var container: AppContainer
            @VisibleForTesting set
    }
}
