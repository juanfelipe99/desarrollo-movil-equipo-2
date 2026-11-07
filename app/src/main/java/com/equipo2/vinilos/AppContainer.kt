package com.equipo2.vinilos

import android.content.Context
import com.equipo2.vinilos.data.local.RoleLocalDataSource
import com.equipo2.vinilos.data.local.ThemeModeLocalDataSource
import com.equipo2.vinilos.data.local.appDataStore
import com.equipo2.vinilos.data.network.ApiClient
import com.equipo2.vinilos.data.network.album.AlbumServiceAdapter
import com.equipo2.vinilos.data.network.artist.ArtistServiceAdapter
import com.equipo2.vinilos.data.network.artist.BandServiceAdapter
import com.equipo2.vinilos.data.repository.AlbumRepository
import com.equipo2.vinilos.data.repository.AlbumRepositoryImpl
import com.equipo2.vinilos.data.repository.ArtistRepository
import com.equipo2.vinilos.data.repository.ArtistRepositoryImpl
import com.equipo2.vinilos.data.repository.BandRepository
import com.equipo2.vinilos.data.repository.BandRepositoryImpl
import com.equipo2.vinilos.data.repository.RoleRepository
import com.equipo2.vinilos.data.repository.RoleRepositoryImpl
import com.equipo2.vinilos.data.repository.ThemeModeRepository
import com.equipo2.vinilos.data.repository.ThemeModeRepositoryImpl

open class AppContainer(context: Context) {

    val apiClient by lazy { ApiClient(BuildConfig.API_BASE_URL) }

    open val albumRepository: AlbumRepository by lazy { AlbumRepositoryImpl(AlbumServiceAdapter(apiClient)) }
    open val artistRepository: ArtistRepository by lazy { ArtistRepositoryImpl(ArtistServiceAdapter(apiClient)) }
    open val bandRepository: BandRepository by lazy { BandRepositoryImpl(BandServiceAdapter(apiClient)) }
    open val roleRepository: RoleRepository by lazy { RoleRepositoryImpl(RoleLocalDataSource(context.appDataStore)) }
    open val themeModeRepository: ThemeModeRepository by lazy { ThemeModeRepositoryImpl(ThemeModeLocalDataSource(context.appDataStore)) }
}
