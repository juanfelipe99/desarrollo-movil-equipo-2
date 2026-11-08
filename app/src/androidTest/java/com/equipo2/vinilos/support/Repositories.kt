package com.equipo2.vinilos.support

import androidx.test.platform.app.InstrumentationRegistry
import com.equipo2.vinilos.AppContainer
import com.equipo2.vinilos.VinilosApplication
import com.equipo2.vinilos.data.repository.AlbumRepository
import com.equipo2.vinilos.data.repository.ArtistRepository
import com.equipo2.vinilos.data.repository.BandRepository
import com.equipo2.vinilos.data.repository.CollectorRepository

/** La app usa este repositorio de álbumes en lugar del real. VinilosRule devuelve el real al terminar. */
fun useAlbumRepository(repository: AlbumRepository) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    VinilosApplication.container = object : AppContainer(context) {
        override val albumRepository: AlbumRepository = repository
    }
}

/**
 * La app usa estos repositorios de artistas y de bandas en lugar de los reales (HU12).
 * Van juntos porque Agregar integrante se abre desde el detalle de la banda.
 */
fun useBandRepositories(artists: ArtistRepository, bands: BandRepository) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    VinilosApplication.container = object : AppContainer(context) {
        override val artistRepository: ArtistRepository = artists
        override val bandRepository: BandRepository = bands
    }
}

/** La app usa este repositorio de artistas en lugar del real. VinilosRule devuelve el real al terminar. */
fun useArtistRepository(repository: ArtistRepository) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    VinilosApplication.container = object : AppContainer(context) {
        override val artistRepository: ArtistRepository = repository
    }
}

/** La app usa este repositorio de coleccionistas en lugar del real. VinilosRule devuelve el real al terminar. */
fun useCollectorRepository(repository: CollectorRepository) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    VinilosApplication.container = object : AppContainer(context) {
        override val collectorRepository: CollectorRepository = repository
    }
}

/** Las tres pestañas de la barra inferior usan estos repositorios, para recorrerlas sin la API real. */
fun useTabRepositories(albums: AlbumRepository, artists: ArtistRepository, collectors: CollectorRepository) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    VinilosApplication.container = object : AppContainer(context) {
        override val albumRepository: AlbumRepository = albums
        override val artistRepository: ArtistRepository = artists
        override val collectorRepository: CollectorRepository = collectors
    }
}
