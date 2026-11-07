package com.equipo2.vinilos.support

import androidx.test.platform.app.InstrumentationRegistry
import com.equipo2.vinilos.AppContainer
import com.equipo2.vinilos.VinilosApplication
import com.equipo2.vinilos.data.repository.AlbumRepository
import com.equipo2.vinilos.data.repository.ArtistRepository
import com.equipo2.vinilos.data.repository.BandRepository

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
