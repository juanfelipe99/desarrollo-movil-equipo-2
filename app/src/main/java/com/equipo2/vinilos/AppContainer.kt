package com.equipo2.vinilos

import android.content.Context
import com.equipo2.vinilos.data.local.RoleLocalDataSource
import com.equipo2.vinilos.data.local.ThemeModeLocalDataSource
import com.equipo2.vinilos.data.local.appDataStore
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.data.network.ApiClient
import com.equipo2.vinilos.data.network.album.AlbumServiceAdapter
import com.equipo2.vinilos.data.network.artist.ArtistAlbumServiceAdapter
import com.equipo2.vinilos.data.network.artist.ArtistApi
import com.equipo2.vinilos.data.network.artist.ArtistServiceAdapter
import com.equipo2.vinilos.data.network.artist.BandServiceAdapter
import com.equipo2.vinilos.data.network.artist.PrizeCatalog
import com.equipo2.vinilos.data.network.artist.PrizeServiceAdapter
import com.equipo2.vinilos.data.network.collector.CollectorServiceAdapter
import com.equipo2.vinilos.data.repository.AlbumRepository
import com.equipo2.vinilos.data.repository.AlbumRepositoryImpl
import com.equipo2.vinilos.data.repository.ArtistAlbumRepository
import com.equipo2.vinilos.data.repository.ArtistAlbumRepositoryImpl
import com.equipo2.vinilos.data.repository.ArtistRepository
import com.equipo2.vinilos.data.repository.ArtistRepositoryImpl
import com.equipo2.vinilos.data.repository.BandRepository
import com.equipo2.vinilos.data.repository.BandRepositoryImpl
import com.equipo2.vinilos.data.repository.CollectorRepository
import com.equipo2.vinilos.data.repository.CollectorRepositoryImpl
import com.equipo2.vinilos.data.repository.PrizeRepository
import com.equipo2.vinilos.data.repository.PrizeRepositoryImpl
import com.equipo2.vinilos.data.repository.RoleRepository
import com.equipo2.vinilos.data.repository.RoleRepositoryImpl
import com.equipo2.vinilos.data.repository.ThemeModeRepository
import com.equipo2.vinilos.data.repository.ThemeModeRepositoryImpl

open class AppContainer(context: Context) {

    val apiClient by lazy { ApiClient(BuildConfig.API_BASE_URL) }

    open val albumRepository: AlbumRepository by lazy { AlbumRepositoryImpl(AlbumServiceAdapter(apiClient)) }
    // El detalle del artista y Agregar premio comparten el catálogo de premios
    private val prizeCatalog by lazy { PrizeCatalog(apiClient.create(ArtistApi::class.java)) }

    open val artistRepository: ArtistRepository by lazy { ArtistRepositoryImpl(ArtistServiceAdapter(apiClient, prizeCatalog)) }
    // El álbum de cada comentario sale del catálogo que el repositorio de álbumes ya tiene en memoria
    open val collectorRepository: CollectorRepository by lazy {
        CollectorRepositoryImpl(CollectorServiceAdapter(apiClient, catalog = { albumRepository.getAlbums().getOrThrow() }))
    }
    open val bandRepository: BandRepository by lazy { BandRepositoryImpl(BandServiceAdapter(apiClient), onMembersChanged = { artistRepository.clearCache() }) }
    open val prizeRepository: PrizeRepository by lazy {
        PrizeRepositoryImpl(
            PrizeServiceAdapter(apiClient, prizeCatalog, receivedDeliveries = ::receivedDeliveries),
            onPrizesChanged = { artistRepository.clearCache() },
        )
    }
    open val artistAlbumRepository: ArtistAlbumRepository by lazy {
        // Agregar álbum usa el catálogo que el repositorio de álbumes ya tiene en memoria
        val catalog = suspend { albumRepository.getAlbums().getOrThrow() }
        ArtistAlbumRepositoryImpl(ArtistAlbumServiceAdapter(apiClient, catalog, artistAlbums = ::artistAlbumIds), onAlbumsChanged = {
            artistRepository.clearCache()
            albumRepository.clearCache()
        })
    }
    open val roleRepository: RoleRepository by lazy { RoleRepositoryImpl(RoleLocalDataSource(context.appDataStore)) }
    open val themeModeRepository: ThemeModeRepository by lazy { ThemeModeRepositoryImpl(ThemeModeLocalDataSource(context.appDataStore)) }

    // Agregar premio y Agregar álbum se abren desde el detalle del artista, que ya está en memoria. Si ya no existe,
    // el detalle falla con NotFound y la pantalla lo dice. Entregar un premio o agregar un álbum olvida el detalle
    private suspend fun receivedDeliveries(artistId: Int, type: ArtistType): Set<Int> =
        artistRepository.getArtistDetail(artistId, type).getOrThrow().prizes.mapTo(HashSet()) { it.id }

    private suspend fun artistAlbumIds(artistId: Int, type: ArtistType): Set<Int> =
        artistRepository.getArtistDetail(artistId, type).getOrThrow().albums.mapTo(HashSet()) { it.id }
}
